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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;

    private final PasswordEncoder passwordEncoder;

    private static final long OTP_EXPIRATION_MINUTES = 5;

    private static final int MAX_ATTEMPTS = 5;


    @Override
    public GeneratedOtp generateVerificationCode(User user, VerificationType verificationType, VerificationPurpose verificationPurpose) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        if (verificationType == null) {
            throw new IllegalArgumentException(
                    "Verification type cannot be null"
            );
        }

        if (verificationPurpose == null) {
            throw new IllegalArgumentException(
                    "Verification purpose cannot be null"
            );
        }

        //Remove previous OTP for the same verification purpose
        // This ensures that only the latest OTP remains valid
        verificationCodeRepository
                .deleteByUserIdAndVerificationTypeAndVerificationPurpose(
                        user.getId(),
                        verificationType,
                        verificationPurpose
                );

        // Generate OTP
        String otp = OtpUtils.generateOTP();

         // Hash OTP before storing it.
        String otpHash = passwordEncoder.encode(otp);

        //Determine destination
        String destination = getDestination(
                user,
                verificationType
        );

        //Create verification code
        VerificationCode verificationCode = VerificationCode.builder()
                        .otpHash(otpHash)
                        .user(user)
                        .destination(destination)
                        .verificationType(verificationType)
                        .verificationPurpose(verificationPurpose)
                        .expiresAt(
                                Instant.now()
                                        .plus(
                                                Duration.ofMinutes(
                                                        OTP_EXPIRATION_MINUTES
                                                )
                                        )
                        )
                        .used(false)
                        .attempts(0)
                        .maxAttempts(MAX_ATTEMPTS)
                        .build();

        VerificationCode savedCode =
                verificationCodeRepository.save(
                        verificationCode
                );

        return new GeneratedOtp(
                otp,
                savedCode
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationCode getVerificationCodeById(Long id) {
        return verificationCodeRepository
                .findById(id)
                .orElseThrow(() ->
                        new VerificationCodeNotFoundException(
                                "Verification code not found with id: "
                                        + id
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationCode getLatestVerificationCode(Long userId, VerificationType verificationType, VerificationPurpose verificationPurpose) {
        return verificationCodeRepository
                .findTopByUserIdAndVerificationTypeAndVerificationPurposeOrderByCreatedAtDesc(
                        userId,
                        verificationType,
                        verificationPurpose
                )
                .orElseThrow(() ->
                        new VerificationCodeNotFoundException(
                                "Verification code not found"
                        )
                );
    }

    @Override
    public boolean verifyCode(VerificationCode verificationCode, String otp) {
        if (verificationCode == null) {
            throw new InvalidVerificationCodeException(
                    "Verification code cannot be null"
            );
        }

        if (otp == null || otp.isBlank()) {
            throw new InvalidVerificationCodeException(
                    "OTP cannot be empty"
            );
        }

        if (verificationCode.isExpired()) {
            throw new InvalidVerificationCodeException(
                    "Verification code has expired"
            );
        }

        if (verificationCode.isUsed()) {
            throw new InvalidVerificationCodeException(
                    "Verification code has already been used"
            );
        }

        if (verificationCode.hasExceededAttempts()) {
            throw new InvalidVerificationCodeException(
                    "Maximum verification attempts exceeded"
            );
        }

        //Compare submitted OTP with stored hash
        boolean matches = passwordEncoder.matches(
                otp,
                verificationCode.getOtpHash()
        );

        if (!matches) {

            verificationCode.incrementAttempts();

            verificationCodeRepository.save(
                    verificationCode
            );

            throw new InvalidVerificationCodeException(
                    "Invalid verification code"
            );
        }

       // OTP is valid.
      //  Mark it as used so it cannot be reused.
        verificationCode.markAsUsed();

        verificationCodeRepository.save(verificationCode);

        return true;
    }

    @Override
    public void deleteVerificationCode(VerificationCode verificationCode) {

        if (verificationCode == null) {
            return;
        }

        verificationCodeRepository.delete(verificationCode);
    }


    private String getDestination(
            User user,
            VerificationType verificationType
    ) {

        return switch (verificationType) {

            case EMAIL -> {

                if (user.getEmail() == null ||
                        user.getEmail().isBlank()) {

                    throw new IllegalArgumentException(
                            "User email is not available"
                    );
                }

                yield user.getEmail();
            }

            case MOBILE -> {

                if (user.getMobile() == null ||
                        user.getMobile().isBlank()) {

                    throw new IllegalArgumentException(
                            "User mobile number is not available"
                    );
                }

                yield user.getMobile();
            }
        };
    }
}
