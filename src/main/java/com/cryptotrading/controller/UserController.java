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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final ForgetPasswordService forgetPasswordService;
    private final VerificationCodeService verificationCodeService;

    @GetMapping("/api/users/profile")
    public ResponseEntity<User> getUserProfile(@RequestHeader("Authorization") String jwt) {
        User user = userService.findUserProfileByJwt(jwt);
        return new ResponseEntity<User>(user, HttpStatus.OK);
    }


    @PostMapping("/api/users/verification/{verificationType}/send-otp")
    public ResponseEntity<String> sendVerificationOtp(
            @RequestHeader("Authorization") String jwt,
            @PathVariable VerificationType verificationType) throws Exception
    {
        User user = userService.findUserProfileByJwt(jwt);
        VerificationCode verificationCode = verificationCodeService
                .getVerificationCodeByUser(user.getId());

        if(verificationCode != null) {
            verificationCode = verificationCodeService
                    .sendVerificationCodeByUser(user, verificationType);
        }

        if(verificationType.equals(VerificationType.EMAIL)) {
            emailService.sendVerificationOtpEmail(user.getEmail(), verificationCode.getOtp());
        }
        return new ResponseEntity<>("verification otp sent successfully", HttpStatus.OK);
    }


    @PatchMapping("/api/users/enable-two-factor/verify-otp/{otp}")
    public ResponseEntity<User> enableTwoFactorAuthentication(
            @PathVariable String otp,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);

        VerificationCode verificationCode = verificationCodeService.getVerificationCodeByUser(user.getId());

        String sendTo = verificationCode.getVerificationType().equals(VerificationType.EMAIL) ?
                verificationCode.getEmail() : verificationCode.getMobile();

        boolean isVerified = verificationCode.getOtp().equals(otp);

        if(isVerified) {
            User updateUser = userService.enableTwoFactorAuthentication(
                    verificationCode.getVerificationType(),
                    sendTo, user
            );
            verificationCodeService.deleteVerificationCodeById(verificationCode);
            return new ResponseEntity<>(updateUser, HttpStatus.OK);
        }
        throw new Exception("Wrong Otp");
    }

    @PostMapping("/auth/users/reset-password/send-otp")
    public ResponseEntity<AuthResponse> sendForgetPasswordOtp(
            @RequestBody ForgetPasswordTokenRequest request) throws Exception {

        User user = userService.findUserByEmail(request.getSendTo());
        String otp = OtpUtils.generateOTP();
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();

        ForgetPasswordToken token = forgetPasswordService.findByUser(user.getId());

        if(token == null) {
            token = forgetPasswordService.createToken(
                    user,
                    id,
                    otp,
                    request.getVerificationType(),
                    request.getSendTo());
        }

        if(request.getVerificationType().equals(VerificationType.EMAIL)) {
            emailService.sendVerificationOtpEmail(
                    user.getEmail(),
                    token.getOtp());
        }

        AuthResponse response = new AuthResponse();
        response.setSession(token.getId());
        response.setMessage("Password reset otp sent successfully");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PatchMapping("/auth/users/reset-password/verify-otp")
    public ResponseEntity<ApiResponse> VerifyResetPassword(
            @RequestParam String id,
            @RequestBody ResetPasswordRequest request,
            @RequestHeader("Authorization") String jwt) throws Exception {

        ForgetPasswordToken forgetPasswordToken = forgetPasswordService.findById(id);

        boolean isVerified = forgetPasswordToken.getOtp().equals(request.getOtp());

        if(isVerified) {
            userService.updatePassword(forgetPasswordToken.getUser(), request.getPassword());
            ApiResponse response = new ApiResponse();
            response.setMessage("Password update Successfully");
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        }
        throw new Exception("Wrong Otp");
    }

}
