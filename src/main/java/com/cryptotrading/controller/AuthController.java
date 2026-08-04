package com.cryptotrading.controller;

import com.cryptotrading.Utils.OtpUtils;
import com.cryptotrading.config.JwtProvider;
import com.cryptotrading.dto.AuthRequest;
import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.model.TwoFactorOTP;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.service.CustomUserDetailsService;
import com.cryptotrading.service.TwoFactorOtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final CustomUserDetailsService customUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TwoFactorOtpService twoFactorOtpService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@RequestBody User user) throws Exception {

      User isEmailExist = userRepository.findByEmail(user.getEmail());
      if(isEmailExist != null) {
          throw new IllegalArgumentException("Email already exists");
      }

        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setMobileNumber(user.getMobileNumber());

        User savedUser = userRepository.save(newUser);

        Authentication auth = authenticate(
                savedUser.getEmail(),
               user.getPassword()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        AuthResponse response = buildAuthResponse(
                savedUser,
                jwt,
                "User registered successfully"
        );

       return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request, User user)  {


        Authentication auth = authenticate(
                request.getEmail(),
                request.getPassword()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        User loggedInUser  = userRepository.findByEmail(request.getEmail());

        if(user.getTwoFactorAuth().isEnabled()) {
            AuthResponse response = new AuthResponse();
            response.setMessage("Two Factor auth is enabled");
            response.setTwoFactorAuthEnabled(true);
            String otp = OtpUtils.generateOTP();

            TwoFactorOTP oldTwoFactorOTP = twoFactorOtpService.findByUser(loggedInUser.getId());
            if(oldTwoFactorOTP != null) {
                twoFactorOtpService.deleteTwoFactorOtp(oldTwoFactorOTP);
            }

            TwoFactorOTP newTwoFactorOTP =  twoFactorOtpService.createTwoFactorOtp(
                    loggedInUser,
                    otp,
                    jwt
            );

            response.setSession(newTwoFactorOTP.getId());
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
        }

        AuthResponse response = buildAuthResponse(
                loggedInUser ,
                jwt,
                "User logged in successfully"
        );

        return ResponseEntity.ok(response);
    }

    private Authentication authenticate(String userName, String password) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);

        if (userDetails == null) {
            throw new BadCredentialsException("Invalid Credentials");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid Credentials");
        }
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
    }

    private AuthResponse buildAuthResponse(User user, String jwt, String message) {
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setMobileNumber(user.getMobileNumber());

        response.setJwt(jwt);
        response.setStatus(true);
        response.setMessage(message);

        return response;
    }
}
