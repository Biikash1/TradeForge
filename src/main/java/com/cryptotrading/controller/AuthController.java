package com.cryptotrading.controller;

import com.cryptotrading.Utils.OtpUtils;
import com.cryptotrading.config.JwtProvider;
import com.cryptotrading.dto.AuthRequest;
import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.dto.RegisterRequest;
import com.cryptotrading.dto.VerifyOtpRequest;
import com.cryptotrading.model.TwoFactorOTP;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.service.CustomUserDetailsService;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.service.TwoFactorOtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TwoFactorOtpService twoFactorOtpService;
    private final EmailService emailService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(
           @Valid @RequestBody RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }

        User newUser = new User();
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setMobile(request.getMobile());

        User savedUser = userRepository.save(newUser);

        Authentication auth = authenticate(
                savedUser.getEmail(),
               request.getPassword()
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        AuthResponse response = buildAuthResponse(
                savedUser,
                jwt,
                "User registered successfully"
        );

       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(
           @Valid @RequestBody AuthRequest request)  {


        Authentication authentication = authenticate(
                request.getEmail(),
                request.getPassword()
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        User loggedInUser = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid credentials"
                        )
                );


         //Check whether 2FA is enabled
        if (loggedInUser.getTwoFactorAuth() != null
                && loggedInUser
                .getTwoFactorAuth()
                .isEnabled()) {

            String otp = OtpUtils.generateOTP();

            //Remove previous OTP
            TwoFactorOTP oldTwoFactorOTP =
                    twoFactorOtpService.findByUser(
                            loggedInUser.getId()
                    );

            if (oldTwoFactorOTP != null) {
                twoFactorOtpService
                        .deleteTwoFactorOtp(oldTwoFactorOTP);
            }

             // Create new OTP session.
            TwoFactorOTP newTwoFactorOTP =
                    twoFactorOtpService.createTwoFactorOtp(
                            loggedInUser,
                            otp,
                            null
                    );

             // Send OTP
            emailService.sendVerificationOtpEmail(
                    loggedInUser.getEmail(),
                    otp
            );

            AuthResponse response = new AuthResponse();

            response.setMessage(
                    "Two-factor authentication required"
            );
            response.setTwoFactorAuthEnabled(true);
            response.setSession(newTwoFactorOTP.getId());
            response.setStatus(true);

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(response);
        }

         // Normal login.
        String jwt = JwtProvider.generateToken(authentication);

        AuthResponse response =
                buildAuthResponse(
                        loggedInUser,
                        jwt,
                        "User logged in successfully"
                );

        return ResponseEntity.ok(response);
    }

    private Authentication authenticate(String userName, String password) {

        UserDetails userDetails =
                customUserDetailsService
                        .loadUserByUsername(userName);

        if (userDetails == null) {
            throw new BadCredentialsException(
                    "Invalid credentials"
            );
        }

        if (!passwordEncoder.matches(
                password,
                userDetails.getPassword()
        )) {
            throw new BadCredentialsException(
                    "Invalid credentials"
            );
        }

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private AuthResponse buildAuthResponse(User user, String jwt, String message) {
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setMobile(user.getMobile());

        response.setJwt(jwt);
        response.setStatus(true);
        response.setMessage(message);

        return response;
    }

    @PostMapping("/two-factor/otp/")
    public ResponseEntity<AuthResponse> verifySigninOtp(
            @Valid @RequestBody VerifyOtpRequest request)  {
        TwoFactorOTP twoFactorOTP =
                twoFactorOtpService.findById(request.getSession());

        if (twoFactorOTP == null) {
            throw new IllegalArgumentException(
                    "Invalid or expired OTP session"
            );
        }

        boolean verified =
                twoFactorOtpService.verifyTwoFactorOtp(
                        twoFactorOTP,
                        request.getOtp()
                );

        if (!verified) {
            throw new BadCredentialsException(
                    "Invalid OTP"
            );
        }

        // * Generate JWT only after successful
        //         * 2FA verification.

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(
                        twoFactorOTP.getUser().getEmail()
                );

        // Create authenticated Authentication object
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        // Generate JWT only after successful 2FA verification
        String jwt = JwtProvider.generateToken(
                authentication
        );

        // OTP is one-time use
        twoFactorOtpService
                .deleteTwoFactorOtp(twoFactorOTP);

        AuthResponse response = new AuthResponse();

        response.setMessage(
                "Two-factor authentication verified"
        );
        response.setTwoFactorAuthEnabled(true);
        response.setJwt(jwt);
        response.setStatus(true);

        return ResponseEntity.ok(response);
    }
}
