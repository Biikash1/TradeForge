package com.cryptotrading.controller;

import com.cryptotrading.Utils.OtpUtils;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.dto.ApiResponse;
import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.dto.ForgetPasswordTokenRequest;
import com.cryptotrading.dto.ResetPasswordRequest;
import com.cryptotrading.model.ForgetPasswordToken;
import com.cryptotrading.model.User;
import com.cryptotrading.model.VerificationCode;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.service.ForgetPasswordService;
import com.cryptotrading.service.UserService;
import com.cryptotrading.service.VerificationCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final ForgetPasswordService forgetPasswordService;
    private final VerificationCodeService verificationCodeService;

    @GetMapping("/profile")
    public ResponseEntity<User> getUserProfile(
            @RequestHeader("Authorization") String jwt) {

        User user = userService.findUserProfileByJwt(jwt);
        return ResponseEntity.ok(user);
    }


    @PostMapping("/verification/{verificationType}/send-otp")
    public ResponseEntity<ApiResponse> sendVerificationOtp(
            @RequestHeader("Authorization") String jwt,
            @PathVariable VerificationType verificationType) throws Exception
    {
        User user = userService.findUserProfileByJwt(jwt);
        VerificationCode verificationCode =
                verificationCodeService
                        .sendVerificationCodeByUser(
                                user,
                                verificationType
                        );

        if (verificationType == VerificationType.EMAIL) {

            emailService.sendVerificationOtpEmail(
                    user.getEmail(),
                    verificationCode.getOtp()
            );
        }

        ApiResponse response = new ApiResponse();

        response.setMessage(
                "Verification OTP sent successfully"
        );

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/enable-two-factor/verify-otp/{otp}")
    public ResponseEntity<User> enableTwoFactorAuthentication(
            @PathVariable String otp,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);

        VerificationCode verificationCode =
                verificationCodeService
                        .getVerificationCodeByUser(user.getId());

        if (verificationCode == null) {
            throw new IllegalArgumentException(
                    "Verification code not found or expired"
            );
        }

        if (!verificationCode.getOtp().equals(otp)) {
            throw new IllegalArgumentException(
                    "Invalid OTP"
            );
        }

        String sendTo =
                verificationCode.getVerificationType()
                        == VerificationType.EMAIL
                        ? verificationCode.getEmail()
                        : verificationCode.getMobile();

        User updatedUser =
                userService.enableTwoFactorAuthentication(
                        verificationCode.getVerificationType(),
                        sendTo,
                        user
                );

        verificationCodeService
                .deleteVerificationCodeById(verificationCode);

        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/reset-password/send-otp")
    public ResponseEntity<AuthResponse> sendForgetPasswordOtp(
           @Valid @RequestBody ForgetPasswordTokenRequest request) throws Exception {

        User user = userService.findUserByEmail(
                request.getSendTo()
        );

        String otp = OtpUtils.generateOTP();

        UUID uuid = UUID.randomUUID();

        String id = uuid.toString();

        ForgetPasswordToken token =
                forgetPasswordService.
                        findByUser(user.getId());

        /*
         * Replace existing token instead of
         * creating multiple active tokens.
         */
        if (token != null) {

            forgetPasswordService.deleteToken(token);
        }

        token =
                forgetPasswordService.createToken(
                        user,
                        id,
                        otp,
                        request.getVerificationType(),
                        request.getSendTo()
                );

        if (request.getVerificationType()
                == VerificationType.EMAIL) {

            emailService.sendVerificationOtpEmail(
                    user.getEmail(),
                    token.getOtp()
            );
        }

        AuthResponse response = new AuthResponse();

        response.setSession(token.getId());
        response.setMessage(
                "Password reset OTP sent successfully"
        );
        response.setStatus(true);

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/reset-password/verify-otp")
    public ResponseEntity<ApiResponse> VerifyResetPassword(
            @RequestParam String id,
            @Valid @RequestBody ResetPasswordRequest request) throws Exception {

        ForgetPasswordToken token = forgetPasswordService.findById(id);

        if (token == null) {
            throw new IllegalArgumentException(
                    "Invalid or expired password reset session"
            );
        }

        if (!token.getOtp().equals(request.getOtp())) {
            throw new IllegalArgumentException(
                    "Invalid OTP"
            );
        }

        userService.updatePassword(
                token.getUser(),
                request.getPassword()
        );

        forgetPasswordService.deleteToken(token);

        ApiResponse response = new ApiResponse();

        response.setMessage(
                "Password updated successfully"
        );

        return ResponseEntity.ok(response);
    }

}
