package com.wanderlust.wanderlust.service;

import com.wanderlust.wanderlust.entity.Reserve;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Async
    public void sendBookingConfirmationEmail(Reserve reserve, String transactionId) {
        try {
            String toEmail = reserve.getReservedBy().getEmail();
            String username = reserve.getReservedBy().getUsername();
            String listingTitle = reserve.getListing().getTitle();
            String location = reserve.getListing().getLocation() + ", " + reserve.getListing().getCountry();
            String checkin = reserve.getCheckin().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            String checkout = reserve.getCheckout().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            long nights = ChronoUnit.DAYS.between(reserve.getCheckin(), reserve.getCheckout());
            int total = reserve.getTotal();

            String subject = "Booking Confirmed - " + listingTitle + " | Wanderlust";

            String htmlContent = buildConfirmationEmailHtml(
                    username, listingTitle, location, checkin, checkout,
                    nights, total, reserve.getAdult(), reserve.getChildren(),
                    reserve.getMobile(), transactionId, reserve.getId()
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Booking confirmation email sent to {} for reservation {}", toEmail, reserve.getId());

        } catch (Exception e) {
            log.error("Failed to send booking confirmation email for reservation {}: {}",
                    reserve.getId(), e.getMessage());
        }
    }

    @Async
    public void sendCancellationEmail(Reserve reserve, String reason, String refundId) {
        try {
            String toEmail = reserve.getReservedBy().getEmail();
            String username = reserve.getReservedBy().getUsername();
            String listingTitle = reserve.getListing().getTitle();
            String location = reserve.getListing().getLocation() + ", " + reserve.getListing().getCountry();
            String checkin = reserve.getCheckin().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            String checkout = reserve.getCheckout().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            int total = reserve.getTotal();

            String subject = "Booking Cancelled - " + listingTitle + " | Wanderlust";

            String htmlContent = buildCancellationEmailHtml(
                    username, listingTitle, location, checkin, checkout,
                    total, reason, refundId, reserve.getId()
            );

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Cancellation email sent to {} for reservation {}", toEmail, reserve.getId());

        } catch (Exception e) {
            log.error("Failed to send cancellation email for reservation {}: {}",
                    reserve.getId(), e.getMessage());
        }
    }

    private String buildCancellationEmailHtml(String username, String listingTitle, String location,
                                                String checkin, String checkout, int total,
                                                String reason, String refundId, String bookingId) {
        String refundSection = refundId != null ? """
                            <div style="background-color: #d4edda; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                                <h3 style="color: #155724; margin: 0 0 15px 0; border-bottom: 2px solid #28a745; padding-bottom: 10px;">Refund Details</h3>
                                <table style="width: 100%%; border-collapse: collapse;">
                                    <tr>
                                        <td style="padding: 6px 0; color: #155724;">Refund Amount</td>
                                        <td style="padding: 6px 0; text-align: right; font-weight: bold; color: #28a745;">&#8377;%,d</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #155724;">Refund ID</td>
                                        <td style="padding: 6px 0; text-align: right; font-family: monospace; font-size: 12px;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #155724;">Status</td>
                                        <td style="padding: 6px 0; text-align: right;">
                                            <span style="background-color: #28a745; color: white; padding: 3px 10px; border-radius: 12px; font-size: 12px;">Refund Processed</span>
                                        </td>
                                    </tr>
                                </table>
                                <p style="color: #155724; font-size: 12px; margin: 10px 0 0 0;">The refund will be credited to your original payment method within 5-7 business days.</p>
                            </div>
                """.formatted(total, refundId) : "";

        String reasonText = reason != null && !reason.isBlank() ? reason : "No reason provided";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                        <!-- Header -->
                        <div style="background-color: #6c757d; padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px;">Wanderlust</h1>
                            <p style="color: #ffffffcc; margin: 8px 0 0 0; font-size: 14px;">Booking Cancellation</p>
                        </div>

                        <!-- Cancel Icon -->
                        <div style="text-align: center; padding: 30px 30px 10px 30px;">
                            <div style="width: 64px; height: 64px; background-color: #dc3545; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center;">
                                <span style="color: white; font-size: 32px;">&#10007;</span>
                            </div>
                            <h2 style="color: #333; margin: 15px 0 5px 0;">Booking Cancelled</h2>
                            <p style="color: #666; margin: 0;">Hi %s, your reservation has been cancelled.</p>
                        </div>

                        <!-- Booking Details -->
                        <div style="padding: 20px 30px;">
                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                                <h3 style="color: #333; margin: 0 0 15px 0; border-bottom: 2px solid #dc3545; padding-bottom: 10px;">Cancelled Booking</h3>
                                <p style="margin: 8px 0;"><strong>Property:</strong> %s</p>
                                <p style="margin: 8px 0;"><strong>Location:</strong> %s</p>
                                <p style="margin: 8px 0;"><strong>Check-in:</strong> %s</p>
                                <p style="margin: 8px 0;"><strong>Check-out:</strong> %s</p>
                                <p style="margin: 8px 0;"><strong>Amount:</strong> &#8377;%,d</p>
                                <p style="margin: 8px 0;"><strong>Reason:</strong> %s</p>
                            </div>

                            %s

                            <!-- Booking Reference -->
                            <div style="text-align: center; background-color: #f8d7da; border-radius: 8px; padding: 15px; margin-bottom: 20px;">
                                <p style="margin: 0; color: #721c24; font-size: 13px;">Cancelled Booking Reference</p>
                                <p style="margin: 5px 0 0 0; font-family: monospace; font-size: 16px; font-weight: bold; color: #333;">%s</p>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #333; padding: 20px 30px; text-align: center;">
                            <p style="color: #999; margin: 0; font-size: 12px;">Thank you for using Wanderlust!</p>
                            <p style="color: #999; margin: 5px 0 0 0; font-size: 11px;">This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                username,
                listingTitle,
                location,
                checkin,
                checkout,
                total,
                reasonText,
                refundSection,
                bookingId
        );
    }

    private String buildConfirmationEmailHtml(String username, String listingTitle, String location,
                                               String checkin, String checkout, long nights,
                                               int total, int adults, int children,
                                               String mobile, String transactionId, String bookingId) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                        <!-- Header -->
                        <div style="background-color: #fe424d; padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px;">Wanderlust</h1>
                            <p style="color: #ffffffcc; margin: 8px 0 0 0; font-size: 14px;">Your booking is confirmed!</p>
                        </div>

                        <!-- Success Icon -->
                        <div style="text-align: center; padding: 30px 30px 10px 30px;">
                            <div style="width: 64px; height: 64px; background-color: #28a745; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center;">
                                <span style="color: white; font-size: 32px;">&#10003;</span>
                            </div>
                            <h2 style="color: #333; margin: 15px 0 5px 0;">Booking Confirmed!</h2>
                            <p style="color: #666; margin: 0;">Hi %s, your reservation has been successfully confirmed.</p>
                        </div>

                        <!-- Booking Details -->
                        <div style="padding: 20px 30px;">
                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                                <h3 style="color: #333; margin: 0 0 15px 0; border-bottom: 2px solid #fe424d; padding-bottom: 10px;">Property Details</h3>
                                <p style="margin: 8px 0;"><strong>Property:</strong> %s</p>
                                <p style="margin: 8px 0;"><strong>Location:</strong> %s</p>
                            </div>

                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                                <h3 style="color: #333; margin: 0 0 15px 0; border-bottom: 2px solid #fe424d; padding-bottom: 10px;">Trip Details</h3>
                                <table style="width: 100%%; border-collapse: collapse;">
                                    <tr>
                                        <td style="padding: 6px 0; color: #666;">Check-in</td>
                                        <td style="padding: 6px 0; text-align: right; font-weight: bold;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #666;">Check-out</td>
                                        <td style="padding: 6px 0; text-align: right; font-weight: bold;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #666;">Duration</td>
                                        <td style="padding: 6px 0; text-align: right; font-weight: bold;">%d night%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #666;">Guests</td>
                                        <td style="padding: 6px 0; text-align: right; font-weight: bold;">%d adult%s%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #666;">Contact</td>
                                        <td style="padding: 6px 0; text-align: right; font-weight: bold;">%s</td>
                                    </tr>
                                </table>
                            </div>

                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
                                <h3 style="color: #333; margin: 0 0 15px 0; border-bottom: 2px solid #fe424d; padding-bottom: 10px;">Payment Summary</h3>
                                <table style="width: 100%%; border-collapse: collapse;">
                                    <tr>
                                        <td style="padding: 8px 0; color: #666;">Total Amount</td>
                                        <td style="padding: 8px 0; text-align: right; font-weight: bold; font-size: 20px; color: #fe424d;">&#8377;%,d</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #666;">Transaction ID</td>
                                        <td style="padding: 6px 0; text-align: right; font-family: monospace; font-size: 12px;">%s</td>
                                    </tr>
                                    <tr>
                                        <td style="padding: 6px 0; color: #666;">Status</td>
                                        <td style="padding: 6px 0; text-align: right;">
                                            <span style="background-color: #28a745; color: white; padding: 3px 10px; border-radius: 12px; font-size: 12px;">Paid</span>
                                        </td>
                                    </tr>
                                </table>
                            </div>

                            <!-- Booking Reference -->
                            <div style="text-align: center; background-color: #fff3cd; border-radius: 8px; padding: 15px; margin-bottom: 20px;">
                                <p style="margin: 0; color: #856404; font-size: 13px;">Booking Reference</p>
                                <p style="margin: 5px 0 0 0; font-family: monospace; font-size: 16px; font-weight: bold; color: #333;">%s</p>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #333; padding: 20px 30px; text-align: center;">
                            <p style="color: #999; margin: 0; font-size: 12px;">Thank you for choosing Wanderlust!</p>
                            <p style="color: #999; margin: 5px 0 0 0; font-size: 11px;">This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(
                username,
                listingTitle,
                location,
                checkin,
                checkout,
                nights, nights > 1 ? "s" : "",
                adults, adults > 1 ? "s" : "",
                children > 0 ? ", " + children + " child" + (children > 1 ? "ren" : "") : "",
                mobile,
                total,
                transactionId,
                bookingId
        );
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String resetLink) {
        try {
            String subject = "Reset Your Password | Wanderlust";

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f5f5f5;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff;">
                        <!-- Header -->
                        <div style="background-color: #fe424d; padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 28px;">Wanderlust</h1>
                            <p style="color: #ffffffcc; margin: 8px 0 0 0; font-size: 14px;">Password Reset</p>
                        </div>

                        <!-- Content -->
                        <div style="padding: 40px 30px;">
                            <h2 style="color: #333; margin: 0 0 20px 0;">Hi %s,</h2>
                            <p style="color: #555; font-size: 16px; line-height: 1.6;">
                                We received a request to reset your password. Click the button below to create a new password:
                            </p>

                            <div style="text-align: center; margin: 35px 0;">
                                <a href="%s"
                                   style="background-color: #fe424d; color: #ffffff; padding: 14px 40px; text-decoration: none;
                                          border-radius: 8px; font-size: 16px; font-weight: bold; display: inline-block;">
                                    Reset Password
                                </a>
                            </div>

                            <p style="color: #888; font-size: 14px; line-height: 1.6;">
                                This link will expire in <strong>30 minutes</strong>. If you didn't request a password reset,
                                you can safely ignore this email.
                            </p>

                            <div style="background-color: #f8f9fa; border-radius: 8px; padding: 15px; margin-top: 25px;">
                                <p style="margin: 0; color: #666; font-size: 13px;">
                                    If the button doesn't work, copy and paste this link into your browser:
                                </p>
                                <p style="margin: 8px 0 0 0; word-break: break-all; font-size: 12px; color: #fe424d;">
                                    %s
                                </p>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #333; padding: 20px 30px; text-align: center;">
                            <p style="color: #999; margin: 0; font-size: 12px;">This is an automated email from Wanderlust. Please do not reply.</p>
                            <p style="color: #999; margin: 5px 0 0 0; font-size: 11px;">For security, this request was received from your account.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, resetLink, resetLink);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }
    }
}
