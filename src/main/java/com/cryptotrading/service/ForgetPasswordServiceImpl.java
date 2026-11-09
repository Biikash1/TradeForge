package com.cryptotrading.service;

import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.exception.InvalidOtpException;
import com.cryptotrading.exception.OtpExpiredException;
import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.ForgetPasswordToken;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.ForgetPasswordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgetPasswordServiceImpl implements ForgetPasswordService {

    private final ForgetPasswordRepository forgetPasswordRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    @Transactional
    public ForgetPasswordToken createToken(User user, String otp, VerificationType verificationType, String sendTo) {
        validateCreateRequest(user, otp, verificationType, sendTo);
        forgetPasswordRepository.deleteByUserId(user.getId());

        ForgetPasswordToken token = ForgetPasswordToken.builder()
                .user(user)
                .otpHash(passwordEncoder.encode(otp))
                .verificationType(verificationType)
                .sendTo(sendTo)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES))
                .verified(false)
                .attempts(0)
                .build();

        log.info("Created new password reset token for User {}", user.getId());
        return forgetPasswordRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public ForgetPasswordToken findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Token ID cannot be empty");
        }
        return forgetPasswordRepository.findById(id).orElseThrow(() -> {
            log.warn("ForgetPasswordToken lookup failed for ID {}", id);
            return new ResourceNotFoundException("Password reset token not found with id: " + id);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public ForgetPasswordToken findByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return forgetPasswordRepository.findByUserId(userId).orElseThrow(() -> {
            log.warn("ForgetPasswordToken lookup failed for User ID {}", userId);
            return new ResourceNotFoundException("Password reset token not found for user: " + userId);
        });
    }

    @Override
    @Transactional
    public boolean verifyToken(ForgetPasswordToken token, String otp) {
        if (token == null || otp == null || otp.isBlank()) {
            log.warn("verifyToken failed: Token or OTP is missing");
            throw new InvalidOtpException("Password reset token and OTP are required");
        }
        if (token.isVerified()) {
            log.warn("verifyToken failed for User {}: Token already used", token.getUser().getId());
            throw new InvalidOtpException("OTP has already been used");
        }
        if (token.isExpired()) {
            log.warn("verifyToken failed for User {}: Token expired", token.getUser().getId());
            throw new OtpExpiredException("OTP has expired");
        }
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("verifyToken failed for User {}: Max attempts exceeded", token.getUser().getId());
            throw new InvalidOtpException("Maximum OTP verification attempts exceeded");
        }

        token.setAttempts(token.getAttempts() + 1);

        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            forgetPasswordRepository.save(token);
            log.warn("verifyToken failed for User {}: Invalid OTP", token.getUser().getId());
            throw new InvalidOtpException("Invalid OTP");
        }

        token.setVerified(true);
        forgetPasswordRepository.save(token);
        log.info("Successfully verified password reset token for User {}", token.getUser().getId());
        return true;
    }

    @Override
    public void deleteToken(ForgetPasswordToken token) {
        if (token == null) return;
        log.debug("Deleting password reset token {}", token.getId());
        forgetPasswordRepository.delete(token);
    }

    private void validateCreateRequest(User user, String otp, VerificationType verificationType, String sendTo) {
        if (user == null || otp == null || otp.isBlank() || verificationType == null || sendTo == null || sendTo.isBlank()) {
            throw new IllegalArgumentException("Invalid token creation request parameters");
        }
    }
}