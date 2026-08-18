package com.cryptotrading.controller;

import com.cryptotrading.dto.*;
import com.cryptotrading.domain.VerificationPurpose;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.User;
import com.cryptotrading.model.VerificationCode;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.service.UserService;
import com.cryptotrading.service.VerificationCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);

        return ResponseEntity.ok(buildUserProfileResponse(user));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfileById(
            @PathVariable Long userId) {
        User user = userService.findUserById(userId);

        return ResponseEntity.ok(buildUserProfileResponse(user));
    }


    @PostMapping("/verification/{verificationType}/send-otp")
    public ResponseEntity<ApiResponse> sendVerificationOtp(
            @RequestHeader("Authorization") String jwt,
            @PathVariable VerificationType verificationType) {

        User user = userService.findUserProfileByJwt(jwt);

        VerificationPurpose purpose =
                verificationType == VerificationType.EMAIL
                        ? VerificationPurpose.EMAIL_VERIFICATION
                        : VerificationPurpose.MOBILE_VERIFICATION;

        GeneratedOtp generateOtp =
                verificationCodeService
                        .generateVerificationCode(
                                user,
                                verificationType,
                                purpose
                        );

        sendOtp(
                user,
                verificationType,
                generateOtp.getOtp()
        );

        ApiResponse response = new ApiResponse();

        response.setMessage(
                "Verification OTP sent successfully"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable-two-factor/send-otp")
    public ResponseEntity<ApiResponse> sendTwoFactorOtp(
            @RequestHeader("Authorization") String jwt,
            @RequestParam VerificationType verificationType) {

        User user =
                userService.findUserProfileByJwt(jwt);

        GeneratedOtp generatedOtp =
                verificationCodeService.generateVerificationCode(
                        user,
                        verificationType,
                        VerificationPurpose.TWO_FACTOR_AUTH
                );

        sendOtp(
                user,
                verificationType,
                generatedOtp.getOtp()
        );

        ApiResponse response =
                new ApiResponse();

        response.setMessage(
                "Two-factor authentication OTP sent successfully"
        );

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/enable-two-factor/verify-otp/{otp}")
    public ResponseEntity<UserProfileResponse> enableTwoFactorAuthentication(
            @PathVariable String otp,
            @RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt);

        VerificationType verificationType =
                user.getTwoFactorAuth() != null
                        && user.getTwoFactorAuth().getSendTo() != null
                        ? user.getTwoFactorAuth().getSendTo()
                        : VerificationType.EMAIL;

        VerificationCode verificationCode =
                verificationCodeService.getLatestVerificationCode(
                        user.getId(),
                        verificationType,
                        VerificationPurpose.TWO_FACTOR_AUTH
                );

        if (verificationCode == null) {
            throw new IllegalArgumentException(
                    "No valid two-factor authentication OTP found"
            );
        }

        verificationCodeService.verifyCode(
                verificationCode,
                otp
        );


        String sendTo =
                verificationType == VerificationType.EMAIL
                        ? user.getEmail()
                        : user.getMobile();

        User updatedUser =
                userService.enableTwoFactorAuthentication(
                        verificationType,
                        sendTo,
                        user
                );

        return ResponseEntity.ok(buildUserProfileResponse(updatedUser));
    }

    @PostMapping("/reset-password/send-otp")
    public ResponseEntity<AuthResponse> sendForgetPasswordOtp(
           @Valid @RequestBody ForgetPasswordTokenRequest request) {

        User user = userService.findUserByEmail(
                request.getSendTo()
        );

        VerificationType verificationType =
                request.getVerificationType();


        if (verificationType == null) {
            verificationType = VerificationType.EMAIL;
        }


        GeneratedOtp generatedOtp =
                verificationCodeService.generateVerificationCode(
                        user,
                        verificationType,
                        VerificationPurpose.PASSWORD_RESET
                );


        sendOtp(
                user,
                verificationType,
                generatedOtp.getOtp()
        );

            AuthResponse response = new AuthResponse();

            response.setSession(
                    generatedOtp
                            .getVerificationCode()
                            .getId()
                            .toString()
            );

            response.setMessage(
                    "Password reset OTP sent successfully"
            );

            response.setStatus(true);

            return ResponseEntity.ok(response);

    }

    //Verify password-reset OTP and update password.
    //     * Example:
    //     * PATCH /api/users/reset-password/verify-otp?id=10

    @PatchMapping("/reset-password/verify-otp")
    public ResponseEntity<ApiResponse> verifyResetPassword(
            @RequestParam Long id,
            @Valid @RequestBody ResetPasswordRequest request) {


        VerificationCode verificationCode =
                verificationCodeService.getVerificationCodeById(id);

        if (verificationCode == null) {
            throw new IllegalArgumentException(
                    "Invalid or expired verification code"
            );
        }

        if (verificationCode.getVerificationPurpose()
                != VerificationPurpose.PASSWORD_RESET) {

            throw new IllegalArgumentException(
                    "Invalid verification purpose"
            );
        }

        verificationCodeService.verifyCode(
                verificationCode,
                request.getOtp()
        );

        userService.updatePassword(
                verificationCode.getUser(),
                request.getPassword()
        );


        ApiResponse response = new ApiResponse();

        response.setMessage(
                "Password updated successfully"
        );

        return ResponseEntity.ok(response);
    }

  // Common OTP Sender
  private void sendOtp(
          User user,
          VerificationType verificationType,
          String otp
  ) {

      switch (verificationType) {

          case EMAIL -> {

              emailService.sendVerificationOtpEmail(
                      user.getEmail(),
                      otp
              );
          }

          case MOBILE -> {

              /*
               * TODO:
               *
               * smsService.sendOtp(
               *      user.getMobile(),
               *      otp
               * );
               */

              throw new UnsupportedOperationException(
                      "SMS OTP service is not implemented yet"
              );
          }
      }
  }

    private UserProfileResponse buildUserProfileResponse(
            User user) {

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .role(user.getRole())
                .twoFactorEnabled(
                        user.getTwoFactorAuth() != null
                                && user.getTwoFactorAuth().isEnabled()
                )
                .build();
    }

}
