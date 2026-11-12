package com.cryptotrading.controller;

import com.cryptotrading.Utils.OtpUtils;
import com.cryptotrading.config.JwtProvider;
import com.cryptotrading.dto.ApiResponse;
import com.cryptotrading.dto.AuthRequest;
import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.dto.RegisterRequest;
import com.cryptotrading.dto.VerifyOtpRequest;
import com.cryptotrading.model.RefreshToken;
import com.cryptotrading.model.TwoFactorOTP;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.service.CustomUserDetailsService;
import com.cryptotrading.service.EmailService;
import com.cryptotrading.service.RefreshTokenService;
import com.cryptotrading.service.TwoFactorOtpService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
    private final RefreshTokenService refreshTokenService;


    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse httpServletResponse) {

        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new BadCredentialsException("Email already exists");
        }

        User newUser = new User();
        newUser.setFullName(request.getFullName().trim());
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setMobile(request.getMobile());

        User savedUser = userRepository.save(newUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                savedUser.getEmail(),
                null,
                java.util.Collections.emptyList()
        );

        String jwt = jwtProvider.generateToken(authentication);

        // Generate and set refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);
        setRefreshTokenCookie(httpServletResponse, refreshToken.getToken());

        AuthResponse response = buildAuthResponse(
                savedUser,
                jwt,
                "User registered successfully"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse httpServletResponse)  { // Added HttpServletResponse

        String email = normalizeEmail(request.getEmail());

        Authentication authentication = authenticate(
                email,
                request.getPassword()
        );

        User loggedInUser = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid credentials")
                );

        if (isTwoFactorEnabled(loggedInUser)) {
            return initiateTwoFactorAuthentication(loggedInUser);
        }

        String jwt = jwtProvider.generateToken(authentication);

        // Generate and set refresh token for standard login
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(loggedInUser);
        setRefreshTokenCookie(httpServletResponse, refreshToken.getToken());

        AuthResponse response = buildAuthResponse(
                loggedInUser,
                jwt,
                "User logged in successfully"
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/two-factor/otp")
    public ResponseEntity<AuthResponse> verifySigninOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletResponse httpServletResponse)  {

        TwoFactorOTP twoFactorOTP = twoFactorOtpService.findById(request.getSession());

        boolean verified = twoFactorOtpService.verifyTwoFactorOtp(twoFactorOTP, request.getOtp());

        if (!verified) {
            throw new BadCredentialsException("Invalid or expired OTP");
        }

        User user = twoFactorOTP.getUser();

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String jwt = jwtProvider.generateToken(authentication);

        // Generate and set refresh token after successful 2FA
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        setRefreshTokenCookie(httpServletResponse, refreshToken.getToken());

        twoFactorOtpService.deleteTwoFactorOtp(twoFactorOTP);

        AuthResponse response = buildAuthResponse(user, jwt, "Two-factor authentication verified");
        response.setTwoFactorAuthEnabled(true);

        return ResponseEntity.ok(response);
    }

    // ---  Refresh Token Endpoint ---
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String requestRefreshToken) {

        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is missing");
        }

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken);
        refreshToken = refreshTokenService.verifyExpiration(refreshToken);
        User user = refreshToken.getUser();

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String newJwt = jwtProvider.generateToken(authentication);
        AuthResponse response = buildAuthResponse(user, newJwt, "Token refreshed successfully");

        return ResponseEntity.ok(response);
    }

    // ---  Logout Endpoint ---
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @CookieValue(name = "refreshToken", required = false) String requestRefreshToken,
            HttpServletResponse httpResponse) {

        if (requestRefreshToken != null && !requestRefreshToken.isBlank()) {
            try {
                RefreshToken token = refreshTokenService.findByToken(requestRefreshToken);
                refreshTokenService.deleteByUserId(token.getUser().getId());
            } catch (Exception ignored) {}
        }

        // Clear the cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new ApiResponse("Logged out successfully"));
    }

    private ResponseEntity<AuthResponse> initiateTwoFactorAuthentication(User user) {
        String otp = OtpUtils.generateOTP();

        TwoFactorOTP oldTwoFactorOTP = twoFactorOtpService.findByUser(user.getId());
        if (oldTwoFactorOTP != null) {
            twoFactorOtpService.deleteTwoFactorOtp(oldTwoFactorOTP);
        }

        TwoFactorOTP newTwoFactorOTP = twoFactorOtpService.createTwoFactorOtp(user, otp);
        emailService.sendVerificationOtpEmail(user.getEmail(), otp);

        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setMobile(user.getMobile());
        response.setMessage("Two-factor authentication required");
        response.setTwoFactorAuthEnabled(true);
        response.setSession(newTwoFactorOTP.getId());
        response.setStatus(true);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    private boolean isTwoFactorEnabled(User user) {
        return user.getTwoFactorAuth() != null && user.getTwoFactorAuth().isEnabled();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Email is required");
        }
        return email.trim().toLowerCase();
    }

    private Authentication authenticate(String userName, String password) {
        UserDetails userDetails;
        try {
            userDetails = customUserDetailsService.loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
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

    // Helper method to set cookies
    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false) // Set to true in production
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7 days in seconds
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}