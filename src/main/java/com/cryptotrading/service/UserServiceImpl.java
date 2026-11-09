package com.cryptotrading.service;

import com.cryptotrading.config.JwtProvider;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.dto.TwoFactorAuth;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional(readOnly = true)
    public User findUserProfileByJwt(String jwt) {
        String email = jwtProvider.getEmailFromToken(jwt);
        return userRepository.findByEmail(email).orElseThrow(() -> {
            log.warn("findUserProfileByJwt failed: User not found for email {}", email);
            return new UsernameNotFoundException("User not found");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByEmail(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        return userRepository.findByEmail(email.trim().toLowerCase()).orElseThrow(() -> {
            log.warn("findUserByEmail failed: User not found for email {}", email);
            return new UsernameNotFoundException("User not found");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        return userRepository.findById(userId).orElseThrow(() -> {
            log.warn("findUserById failed: User not found for ID {}", userId);
            return new IllegalArgumentException("User not found with id: " + userId);
        });
    }

    @Override
    public User enableTwoFactorAuthentication(VerificationType verificationType, String sendTo, User user) {
        if (verificationType == null || sendTo == null || sendTo.isBlank() || user == null) {
            throw new IllegalArgumentException("Invalid parameters for enabling 2FA");
        }

        TwoFactorAuth twoFactorAuth = new TwoFactorAuth();
        twoFactorAuth.setEnabled(true);
        twoFactorAuth.setSendTo(verificationType);
        user.setTwoFactorAuth(twoFactorAuth);

        log.info("Enabled 2FA ({}) for User {}", verificationType, user.getId());
        return userRepository.save(user);
    }

    @Override
    public User updatePassword(User user, String newPassword) {
        if (user == null || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("User and new password are required");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("Password updated successfully for User {}", user.getId());
        return userRepository.save(user);
    }
}