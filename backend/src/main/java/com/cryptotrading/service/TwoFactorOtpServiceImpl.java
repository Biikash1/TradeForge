package com.cryptotrading.service;

import com.cryptotrading.exception.InvalidOtpException;
import com.cryptotrading.exception.OtpExpiredException;
import com.cryptotrading.model.TwoFactorOTP;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.TwoFactorOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TwoFactorOtpServiceImpl implements TwoFactorOtpService{

    private final TwoFactorOtpRepository twoFactorOtpRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRATION_MINUTES = 15;

    private static final int MAX_ATTEMPTS = 5;

    @Override
    @Transactional
    public TwoFactorOTP createTwoFactorOtp(User user, String otp) {

        validateCreateRequest(
                user,
                otp
        );

        // Remove any previous OTP.
        twoFactorOtpRepository.deleteByUserId(
                user.getId()
        );

        TwoFactorOTP twoFactorOTP =
                TwoFactorOTP.builder()
                        .otpHash(passwordEncoder.encode(otp))
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(
                                                OTP_EXPIRATION_MINUTES
                                        )
                        )
                        .verified(false)
                        .attempts(0)
                        .build();

        return twoFactorOtpRepository.save(twoFactorOTP);
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorOTP findByUser(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException( "User ID cannot be null" );
        }

        /*
         * No OTP is a valid situation.
         *
         * For example, when a user is logging in
         * with 2FA for the first time, there may be
         * no previous OTP.
         */
        return twoFactorOtpRepository
                .findByUserId(userId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorOTP findById(String id) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException( "OTP ID cannot be empty" );
        }

        return twoFactorOtpRepository
                .findById(id)
                .orElseThrow(() ->
                        new InvalidOtpException(
                                "Invalid or expired OTP session"
                        )
                );
    }

    @Override
    @Transactional
    public boolean verifyTwoFactorOtp(TwoFactorOTP twoFactorOTP, String otp) {

        validateVerificationRequest(twoFactorOTP, otp);

          // Prevent OTP reuse.
        if (twoFactorOTP.isVerified()) {
            throw new InvalidOtpException(
                    "OTP has already been used"
            );
        }


         // Check expiration before comparing OTP.
        if (twoFactorOTP.isExpired()) {
            throw new OtpExpiredException(
                    "OTP has expired"
            );
        }

         // Prevent brute-force attacks.
        if (twoFactorOTP.getAttempts() >= MAX_ATTEMPTS) {

            throw new InvalidOtpException(
                    "Maximum OTP verification attempts exceeded"
            );
        }

         //Increment attempts on every verification
        twoFactorOTP.setAttempts(
                twoFactorOTP.getAttempts() + 1
        );

          //Compare raw OTP with stored hash.
        boolean valid =
                passwordEncoder.matches(
                        otp,
                        twoFactorOTP.getOtpHash()
                );

        if (!valid) {

            twoFactorOtpRepository.save(
                    twoFactorOTP
            );

            throw new InvalidOtpException(
                    "Invalid OTP"
            );
        }

          //OTP successfully verified.
        twoFactorOTP.setVerified(true);

        twoFactorOtpRepository.save(
                twoFactorOTP
        );

        //OTP is one-time use.
        twoFactorOtpRepository.delete( twoFactorOTP );

        return true;

    }

    @Override
    @Transactional
    public void deleteTwoFactorOtp(TwoFactorOTP twoFactorOTP) {
        if (twoFactorOTP == null) {
            return;
        }

        twoFactorOtpRepository.delete(
                twoFactorOTP
        );
    }

    private void validateCreateRequest(
            User user,
            String otp) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        if (otp == null ||
                otp.isBlank()) {

            throw new IllegalArgumentException(
                    "OTP cannot be empty"
            );
        }

        if (passwordEncoder == null) {
            throw new IllegalStateException( "Password encoder is not configured" );
        }
    }

    private void validateVerificationRequest( TwoFactorOTP twoFactorOTP, String otp) {
        if (twoFactorOTP == null) {
            throw new InvalidOtpException( "OTP verification request is invalid" );
        }

        if (otp == null || otp.isBlank()) {
            throw new InvalidOtpException( "OTP cannot be empty" );
        }

        if (twoFactorOTP.getOtpHash() == null ||
                twoFactorOTP.getOtpHash().isBlank()) {
            throw new InvalidOtpException( "OTP verification data is invalid" ); }
    }
}
