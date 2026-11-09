package com.cryptotrading.service;

import com.cryptotrading.Utils.OtpUtils;
import com.cryptotrading.dto.GeneratedOtp;
import com.cryptotrading.domain.VerificationPurpose;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.exception.InvalidVerificationCodeException;
import com.cryptotrading.exception.VerificationCodeNotFoundException;
import com.cryptotrading.model.User;
import com.cryptotrading.model.VerificationCode;
import com.cryptotrading.repository.VerificationCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    public GeneratedOtp generateVerificationCode(User user, VerificationType verificationType, VerificationPurpose verificationPurpose) {
        if (user == null || verificationType == null || verificationPurpose == null) {
            throw new IllegalArgumentException("Invalid verification code generation parameters");
        }

        verificationCodeRepository.deleteByUserIdAndVerificationTypeAndVerificationPurpose(user.getId(), verificationType, verificationPurpose);

        String otp = OtpUtils.generateOTP();
        String otpHash = passwordEncoder.encode(otp);
        String destination = getDestination(user, verificationType);

        VerificationCode verificationCode = VerificationCode.builder()
                .otpHash(otpHash)
                .user(user)
                .destination(destination)
                .verificationType(verificationType)
                .verificationPurpose(verificationPurpose)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(OTP_EXPIRATION_MINUTES)))
                .used(false)
                .attempts(0)
                .maxAttempts(MAX_ATTEMPTS)
                .build();

        VerificationCode savedCode = verificationCodeRepository.save(verificationCode);
        log.info("Generated Verification Code (Purpose: {}) for User {}", verificationPurpose, user.getId());
        return new GeneratedOtp(otp, savedCode);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationCode getVerificationCodeById(Long id) {
        return verificationCodeRepository.findById(id).orElseThrow(() -> {
            log.warn("Verification Code lookup failed for ID {}", id);
            return new VerificationCodeNotFoundException("Verification code not found with id: " + id);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationCode getLatestVerificationCode(Long userId, VerificationType verificationType, VerificationPurpose verificationPurpose) {
        return verificationCodeRepository
                .findTopByUserIdAndVerificationTypeAndVerificationPurposeOrderByCreatedAtDesc(userId, verificationType, verificationPurpose)
                .orElseThrow(() -> {
                    log.warn("Latest Verification Code not found for User {} (Purpose: {})", userId, verificationPurpose);
                    return new VerificationCodeNotFoundException("Verification code not found");
                });
    }

    @Override
    public boolean verifyCode(VerificationCode verificationCode, String otp) {
        if (verificationCode == null || otp == null || otp.isBlank()) {
            throw new InvalidVerificationCodeException("Verification code and OTP are required");
        }
        if (verificationCode.isExpired()) {
            log.warn("verifyCode failed for User {}: Code expired", verificationCode.getUser().getId());
            throw new InvalidVerificationCodeException("Verification code has expired");
        }
        if (verificationCode.isUsed()) {
            log.warn("verifyCode failed for User {}: Code already used", verificationCode.getUser().getId());
            throw new InvalidVerificationCodeException("Verification code has already been used");
        }
        if (verificationCode.hasExceededAttempts()) {
            log.warn("verifyCode failed for User {}: Max attempts exceeded", verificationCode.getUser().getId());
            throw new InvalidVerificationCodeException("Maximum verification attempts exceeded");
        }

        if (!passwordEncoder.matches(otp, verificationCode.getOtpHash())) {
            verificationCode.incrementAttempts();
            verificationCodeRepository.save(verificationCode);
            log.warn("verifyCode failed for User {}: Invalid OTP", verificationCode.getUser().getId());
            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        verificationCode.markAsUsed();
        verificationCodeRepository.save(verificationCode);
        log.info("Successfully verified code for User {}", verificationCode.getUser().getId());
        return true;
    }

    @Override
    public void deleteVerificationCode(VerificationCode verificationCode) {
        if (verificationCode != null) {
            log.debug("Deleting verification code {}", verificationCode.getId());
            verificationCodeRepository.delete(verificationCode);
        }
    }

    private String getDestination(User user, VerificationType verificationType) {
        return switch (verificationType) {
            case EMAIL -> {
                if (user.getEmail() == null || user.getEmail().isBlank()) throw new IllegalArgumentException("User email is not available");
                yield user.getEmail();
            }
            case MOBILE -> {
                if (user.getMobile() == null || user.getMobile().isBlank()) throw new IllegalArgumentException("User mobile number is not available");
                yield user.getMobile();
            }
        };
    }
}