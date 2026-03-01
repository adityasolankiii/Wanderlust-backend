package com.wanderlust.wanderlust.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.wanderlust.wanderlust.dto.*;
import com.wanderlust.wanderlust.entity.Payment;
import com.wanderlust.wanderlust.entity.Refund;
import com.wanderlust.wanderlust.entity.Reserve;
import com.wanderlust.wanderlust.entity.Listing;
import com.wanderlust.wanderlust.entity.User;
import com.wanderlust.wanderlust.exception.ResourceNotFoundException;
import com.wanderlust.wanderlust.repository.PaymentRepository;
import com.wanderlust.wanderlust.repository.RefundRepository;
import com.wanderlust.wanderlust.repository.ReserveRepository;
import com.wanderlust.wanderlust.repository.ListingRepository;
import com.wanderlust.wanderlust.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final double SERVICE_TAX_PERCENT = 10.0;

    private final PaymentRepository paymentRepository;
    private final ReserveRepository reserveRepository;
    private final RefundRepository refundRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    private RazorpayClient razorpayClient;

    @PostConstruct
    public void init() {
        try {
            this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay client", e);
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    /**
     * Creates a Razorpay order WITHOUT creating a reservation.
     * The reservation will only be created after successful payment verification.
     * Amount includes 10% service tax.
     */
    @Transactional
    public Map<String, Object> createOrder(PaymentRequest request, String username) {
        Listing listing = listingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Calculate amount with service tax
        LocalDate checkin = LocalDate.parse(request.getCheckin());
        LocalDate checkout = LocalDate.parse(request.getCheckout());

        // Validate dates
        if (checkin.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Check-in date cannot be in the past");
        }

        if (!checkout.isAfter(checkin)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        // Check date-based availability (overlap with existing confirmed bookings)
        List<Reserve> overlapping = reserveRepository.findOverlappingReservations(
                request.getListingId(), checkin, checkout);
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                    "This property is already booked for the selected dates. Please choose different dates.");
        }

        // Prevent same user from booking the same property if they already have an active booking
        List<Reserve> existingUserBookings = reserveRepository.findActiveBookingsByUser(
                request.getListingId(), user.getId(), LocalDate.now());
        if (!existingUserBookings.isEmpty()) {
            throw new IllegalArgumentException(
                    "You already have an active booking for this property. Please cancel or wait for your existing booking to end before making a new one.");
        }

        long nights = ChronoUnit.DAYS.between(checkin, checkout);
        int subtotal = (int) (listing.getPrice() * nights);
        int serviceFee = (int) Math.round(subtotal * (SERVICE_TAX_PERCENT / 100));
        int totalAmount = subtotal + serviceFee;

        // Create a real Razorpay order with the total (including service tax)
        String orderId = createRazorpayOrder(totalAmount);

        // Create Payment record WITHOUT a reservation link
        Payment payment = Payment.builder()
                .status("pending")
                .amount(totalAmount)
                .currency("INR")
                .razorpayOrderId(orderId)
                .listingId(request.getListingId())
                .checkin(request.getCheckin())
                .checkout(request.getCheckout())
                .adult(request.getAdult())
                .children(request.getChildren() != null ? request.getChildren() : 0)
                .build();

        payment = paymentRepository.save(payment);

        return Map.of(
                "payment", PaymentDto.from(payment),
                "orderId", orderId,
                "keyId", razorpayKeyId,
                "amount", totalAmount,
                "subtotal", subtotal,
                "serviceFee", serviceFee
        );
    }

    private String createRazorpayOrder(int amountInRupees) {
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInRupees * 100); // Razorpay expects paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            return order.get("id");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Failed to create payment order. Please try again.", e);
        }
    }

    /**
     * Verifies Razorpay payment signature and creates the reservation ONLY on success.
     * This ensures no DB entry for reservation exists until payment is confirmed.
     */
    @Transactional
    public ReserveDto verifyPayment(PaymentVerificationRequest request, String username) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Verify signature
        boolean isSignatureValid = verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isSignatureValid) {
            log.error("Razorpay signature verification failed for orderId={}, paymentId={}",
                    request.getRazorpayOrderId(), request.getRazorpayPaymentId());
            payment.setStatus("failed");
            paymentRepository.save(payment);
            throw new IllegalArgumentException("Payment verification failed. Invalid signature.");
        }

        // Payment is valid — update payment status
        payment.setStatus("success");
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setTransactionId(request.getRazorpayPaymentId());
        payment.setPaymentMethod("razorpay");

        // Fetch user and listing
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Listing listing = listingRepository.findById(payment.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));

        // Double-check date availability (race condition guard)
        List<Reserve> overlapping = reserveRepository.findOverlappingReservations(
                listing.getId(),
                LocalDate.parse(payment.getCheckin()),
                LocalDate.parse(payment.getCheckout()));
        if (!overlapping.isEmpty()) {
            payment.setStatus("refund_pending");
            paymentRepository.save(payment);
            throw new IllegalArgumentException(
                    "These dates were booked by another user while you were paying. A refund will be processed.");
        }

        // NOW create the reservation (only after successful payment)
        Reserve reserve = Reserve.builder()
                .checkin(LocalDate.parse(payment.getCheckin()))
                .checkout(LocalDate.parse(payment.getCheckout()))
                .adult(payment.getAdult())
                .children(payment.getChildren())
                .mobile(request.getMobile())
                .total(payment.getAmount())
                .status("confirmed")
                .reservedBy(user)
                .listing(listing)
                .build();

        reserve = reserveRepository.save(reserve);

        // Link payment to reservation (set both sides of the relationship)
        payment.setReserve(reserve);
        reserve.setPayment(payment);
        paymentRepository.save(payment);

        // Send booking confirmation email
        emailService.sendBookingConfirmationEmail(reserve, request.getRazorpayPaymentId());

        return ReserveDto.from(reserve);
    }

    @Transactional(readOnly = true)
    public PaymentDto getById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return PaymentDto.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentDto getByReserve(String reserveId) {
        Payment payment = paymentRepository.findByReserveId(reserveId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for this reservation"));
        return PaymentDto.from(payment);
    }

    @Transactional
    public Map<String, Object> processRefund(RefundRequest request) {
        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!"success".equals(payment.getStatus())) {
            throw new IllegalArgumentException("Can only refund successful payments");
        }

        int refundAmount = request.getAmount() != null ? request.getAmount() : payment.getAmount();

        Refund refund = Refund.builder()
                .amount(refundAmount)
                .reason(request.getReason())
                .status("processed")
                .razorpayRefundId("rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14))
                .payment(payment)
                .build();

        refund = refundRepository.save(refund);

        payment.setStatus("refunded");
        paymentRepository.save(payment);

        return Map.of(
                "refund", Map.of(
                        "id", refund.getId(),
                        "status", refund.getStatus(),
                        "amount", refund.getAmount()
                ),
                "message", "Refund processed successfully"
        );
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());
            String generatedSignature = HexFormat.of().formatHex(hash);
            return generatedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying Razorpay signature", e);
            return false;
        }
    }
}
