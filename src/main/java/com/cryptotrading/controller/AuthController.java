package com.cryptotrading.controller;

import com.cryptotrading.config.JwtProvider;
import com.cryptotrading.dto.AuthRequest;
import com.cryptotrading.dto.AuthResponse;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.UserRepository;
import com.cryptotrading.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> register(@RequestBody User user) throws Exception {

      User isEmailExist = userRepository.findByEmail(user.getEmail());
      if(isEmailExist != null) {
          throw new Exception("Email is already used with another account");
      }

        User newUser = new User();
        newUser.setFullName(user.getFullName());
        newUser.setEmail(user.getEmail());
        newUser.setPassword(user.getPassword());
        newUser.setMobileNumber(user.getMobileNumber());

        User savedUser = userRepository.save(newUser);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
               user.getPassword()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        AuthResponse response = new AuthResponse();
        response.setJwt(jwt);
        response.setStatus(true);
        response.setMessage("User registered successfully");

       return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) throws Exception {

        String userName = request.getEmail();
        String password = request.getPassword();

        Authentication auth = authenticate(
                userName, password
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = JwtProvider.generateToken(auth);

        AuthResponse response = new AuthResponse();
        response.setJwt(jwt);
        response.setStatus(true);
        response.setMessage("User logged in successfully");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Authentication authenticate(String userName, String password) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(userName);

        if (userDetails == null) {
            throw new BadCredentialsException("Invalid Username");
        }

        if (!password.equals(userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }
}
