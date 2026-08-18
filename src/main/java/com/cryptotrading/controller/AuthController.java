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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(
           @Valid @RequestBody RegisterRequest request) {

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }

        User newUser = new User();
        newUser.setFullName(request.getFullName().trim());
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setMobile(request.getMobile());

        User savedUser = userRepository.save(newUser);

         // Authenticate the newly registered user.
         // We generate a fresh JWT for this successful authentication
        Authentication authentication =
                authenticate(
                        savedUser.getEmail(),
                        request.getPassword()
                );

        String jwt =
                jwtProvider.generateToken(
                        authentication
                );

       return ResponseEntity
               .status(HttpStatus.CREATED)
               .body(
                       buildAuthResponse(
                               savedUser,
                               jwt,
                               "User registered successfully"
                       )
               );
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(
           @Valid @RequestBody AuthRequest request)  {

        String email = normalizeEmail(request.getEmail());

        Authentication authentication = authenticate(
                email,
                request.getPassword()
        );

        User loggedInUser = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid credentials"
                        )
                );


         //Check whether 2FA is enabled
        if (isTwoFactorEnabled(loggedInUser)) {

            return initiateTwoFactorAuthentication(
                    loggedInUser
            );
        }

         // Normal login.
        String jwt =  jwtProvider.generateToken(
                authentication
        );

        AuthResponse response =
                buildAuthResponse(
                        loggedInUser,
                        jwt,
                        "User logged in successfully"
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/two-factor/otp")
    public ResponseEntity<AuthResponse> verifySigninOtp(
            @Valid @RequestBody VerifyOtpRequest request)  {

        TwoFactorOTP twoFactorOTP =
                twoFactorOtpService.findById(request.getSession());

        boolean verified =
                twoFactorOtpService.verifyTwoFactorOtp(
                        twoFactorOTP,
                        request.getOtp()
                );

        if (!verified) {
            throw new BadCredentialsException(
                    "Invalid or expired OTP"
            );
        }

        User user = twoFactorOTP.getUser();

        // * Generate JWT only after successful
        //         * 2FA verification.

        UserDetails userDetails =
                customUserDetailsService.loadUserByUsername(
                        user.getEmail()
                );

        // Create authenticated Authentication object
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        // Generate JWT only after successful 2FA verification
        String jwt =  jwtProvider.generateToken(
                authentication
        );

        // OTP is one-time use
        twoFactorOtpService
                .deleteTwoFactorOtp(twoFactorOTP);

        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setMobile(user.getMobile());

        response.setMessage(
                "Two-factor authentication verified"
        );
        response.setTwoFactorAuthEnabled(true);
        response.setJwt(jwt);
        response.setStatus(true);

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<AuthResponse>
    initiateTwoFactorAuthentication(User user) {

        String otp = OtpUtils.generateOTP();

        //Remove old OTP if one exists.
        TwoFactorOTP oldTwoFactorOTP =
                twoFactorOtpService.findByUser(
                        user.getId()
                );

        if (oldTwoFactorOTP != null) {
            twoFactorOtpService
                    .deleteTwoFactorOtp(oldTwoFactorOTP);
        }

        // Create new OTP session.
        TwoFactorOTP newTwoFactorOTP =
                twoFactorOtpService.createTwoFactorOtp(
                        user,
                        otp
                );

        // Send OTP through email
        emailService.sendVerificationOtpEmail(
                user.getEmail(),
                otp
        );

        /*
         * Do NOT generate JWT here.
         *
         * JWT will only be generated after
         * successful OTP verification.
         */
        AuthResponse response = new AuthResponse();

        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setMobile(user.getMobile());

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

    private boolean isTwoFactorEnabled(User user) {

        return user.getTwoFactorAuth() != null
                && user.getTwoFactorAuth().isEnabled();
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {

            throw new BadCredentialsException(
                    "Email is required"
            );
        }

        return email
                .trim()
                .toLowerCase();
    }

    private Authentication authenticate(String userName, String password) {

        UserDetails userDetails;

        try {
            userDetails =
                    customUserDetailsService
                            .loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
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



}
