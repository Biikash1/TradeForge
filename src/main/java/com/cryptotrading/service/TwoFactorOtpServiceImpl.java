package com.cryptotrading.service;

import com.cryptotrading.exception.InvalidOtpException;
import com.cryptotrading.exception.OtpExpiredException;
import com.cryptotrading.model.TwoFactorOTP;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.TwoFactorOtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorOtpServiceImpl implements TwoFactorOtpService {

    private final TwoFactorOtpRepository twoFactorOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int OTP_EXPIRATION_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;

    @Override
    @Transactional
    public TwoFactorOTP createTwoFactorOtp(User user, String otp) {
        validateCreateRequest(user, otp);
        twoFactorOtpRepository.deleteByUserId(user.getId());

        TwoFactorOTP twoFactorOTP = TwoFactorOTP.builder()
                .otpHash(passwordEncoder.encode(otp))
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES))
                .verified(false)
                .attempts(0)
                .build();

        log.info("Created 2FA OTP session for User {}", user.getId());
        return twoFactorOtpRepository.save(twoFactorOTP);
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorOTP findByUser(Long userId) {
        if (userId == null) throw new IllegalArgumentException("User ID cannot be null");
        return twoFactorOtpRepository.findByUserId(userId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorOTP findById(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("OTP ID cannot be empty");
        return twoFactorOtpRepository.findById(id).orElseThrow(() -> {
            log.warn("2FA OTP session lookup failed for ID {}", id);
            return new InvalidOtpException("Invalid or expired OTP session");
        });
    }

    @Override
    @Transactional
    public boolean verifyTwoFactorOtp(TwoFactorOTP twoFactorOTP, String otp) {
        validateVerificationRequest(twoFactorOTP, otp);

        if (twoFactorOTP.isVerified()) {
            log.warn("verifyTwoFactorOtp failed for User {}: Session already verified", twoFactorOTP.getUser().getId());
            throw new InvalidOtpException("OTP has already been used");
        }
        if (twoFactorOTP.isExpired()) {
            log.warn("verifyTwoFactorOtp failed for User {}: Session expired", twoFactorOTP.getUser().getId());
            throw new OtpExpiredException("OTP has expired");
        }
        if (twoFactorOTP.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("verifyTwoFactorOtp failed for User {}: Max attempts exceeded", twoFactorOTP.getUser().getId());
            throw new InvalidOtpException("Maximum OTP verification attempts exceeded");
        }

        twoFactorOTP.setAttempts(twoFactorOTP.getAttempts() + 1);

        if (!passwordEncoder.matches(otp, twoFactorOTP.getOtpHash())) {
            twoFactorOtpRepository.save(twoFactorOTP);
            log.warn("verifyTwoFactorOtp failed for User {}: Invalid OTP", twoFactorOTP.getUser().getId());
            throw new InvalidOtpException("Invalid OTP");
        }

        twoFactorOTP.setVerified(true);
        twoFactorOtpRepository.save(twoFactorOTP);
        twoFactorOtpRepository.delete(twoFactorOTP);

        log.info("Successfully verified 2FA OTP for User {}", twoFactorOTP.getUser().getId());
        return true;
    }

    @Override
    @Transactional
    public void deleteTwoFactorOtp(TwoFactorOTP twoFactorOTP) {
        if (twoFactorOTP != null) {
            log.debug("Deleting 2FA OTP session {}", twoFactorOTP.getId());
            twoFactorOtpRepository.delete(twoFactorOTP);
        }
    }

    private void validateCreateRequest(User user, String otp) {
        if (user == null || otp == null || otp.isBlank() || passwordEncoder == null) {
            throw new IllegalArgumentException("Invalid 2FA creation request parameters");
        }
    }

    private void validateVerificationRequest(TwoFactorOTP twoFactorOTP, String otp) {
        if (twoFactorOTP == null || otp == null || otp.isBlank() || twoFactorOTP.getOtpHash() == null || twoFactorOTP.getOtpHash().isBlank()) {
            throw new InvalidOtpException("OTP verification request is invalid");
        }
    }
}