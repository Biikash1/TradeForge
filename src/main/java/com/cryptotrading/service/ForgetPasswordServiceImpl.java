package com.cryptotrading.service;

import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.exception.InvalidOtpException;
import com.cryptotrading.exception.OtpExpiredException;
import com.cryptotrading.exception.ResourceNotFoundException;
import com.cryptotrading.model.ForgetPasswordToken;
import com.cryptotrading.model.TwoFactorOTP;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.ForgetPasswordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForgetPasswordServiceImpl implements ForgetPasswordService{

    private final ForgetPasswordRepository forgetPasswordRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRATION_MINUTES = 5;

    private static final int MAX_ATTEMPTS = 5;

    @Override
    @Transactional
    public ForgetPasswordToken createToken(User user, String otp, VerificationType verificationType, String sendTo) {

        validateCreateRequest(user, otp, verificationType, sendTo);

        forgetPasswordRepository.deleteByUserId(
                user.getId()
        );

        ForgetPasswordToken token =
                ForgetPasswordToken.builder()
                        .user(user)
                        .otpHash(
                                passwordEncoder.encode(otp)
                        )
                        .verificationType(
                                verificationType
                        )
                        .sendTo(sendTo)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(
                            LocalDateTime.now().plusMinutes(
                                        OTP_EXPIRATION_MINUTES
                                )
                        )
                        .verified(false)
                        .attempts(0)
                        .build();

        return forgetPasswordRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public ForgetPasswordToken findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Token ID cannot be empty"
            );
        }

        return forgetPasswordRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Password reset token not found with id: "
                                        + id
                        )
                );

    }

    @Override
    @Transactional(readOnly = true)
    public ForgetPasswordToken findByUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null"
            );
        }

        return forgetPasswordRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Password reset token not found for user: "
                                        + userId
                        )
                );
    }

    @Override
    @Transactional
    public boolean verifyToken(
            ForgetPasswordToken token,
            String otp) {

        if (token == null) {
            throw new InvalidOtpException(
                    "Password reset token is required"
            );
        }

        if (otp == null || otp.isBlank()) {
            throw new InvalidOtpException(
                    "OTP cannot be empty"
            );
        }

         //Prevent OTP reuse.
        if (token.isVerified()) {
            throw new InvalidOtpException(
                    "OTP has already been used"
            );
        }


         //Check expiration.
        if (token.isExpired()) {
            throw new OtpExpiredException(
                    "OTP has expired"
            );
        }

         //Prevent brute-force attacks
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            throw new InvalidOtpException(
                    "Maximum OTP verification attempts exceeded"
            );
        }

         // Increment attempt count for every
         // verification request.

        token.setAttempts(
                token.getAttempts() + 1
        );

        //Compare raw OTP with BCrypt hash.
        boolean valid =
                passwordEncoder.matches(
                        otp,
                        token.getOtpHash()
                );

        if (!valid) {
            forgetPasswordRepository.save(
                    token
            );

            throw new InvalidOtpException(
                    "Invalid OTP"
            );
        }

         //Mark token as verified.
        token.setVerified(true);

        forgetPasswordRepository.save(
                token
        );

        return true;
    }

    @Override
    public void deleteToken(ForgetPasswordToken token) {
        if (token == null) {
            return;
        }

        forgetPasswordRepository.delete(
                token
        );
    }

    private void validateCreateRequest(
            User user,
            String otp,
            VerificationType verificationType,
            String sendTo) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        if (otp == null || otp.isBlank()) {
            throw new IllegalArgumentException(
                    "OTP cannot be empty"
            );
        }

        if (verificationType == null) {
            throw new IllegalArgumentException(
                    "Verification type is required"
            );
        }

        if (sendTo == null || sendTo.isBlank()) {
            throw new IllegalArgumentException(
                    "Send to cannot be empty"
            );
        }
    }
}
