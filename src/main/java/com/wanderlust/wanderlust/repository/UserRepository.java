package com.wanderlust.wanderlust.repository;

import com.wanderlust.wanderlust.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailHash(String emailHash);
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
    Optional<User> findByResetToken(String resetToken);
    boolean existsByUsername(String username);
    boolean existsByEmailHash(String emailHash);
}
