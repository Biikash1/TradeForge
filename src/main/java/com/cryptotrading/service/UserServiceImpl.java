package com.cryptotrading.service;

import com.cryptotrading.config.JwtProvider;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.dto.TwoFactorAuth;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findUserProfileByJwt(String jwt) {
     String email = JwtProvider.getEmailFromToken(jwt);
     return userRepository.findByEmail(email)
             .orElseThrow(() ->
                     new UsernameNotFoundException("User not found"));
    }

    @Override
    public User findUserByEmail(String email) {
       return userRepository.findByEmail(email)
                .orElseThrow(() ->
                new UsernameNotFoundException("User not found"));
    }

    @Override
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found with id: " + userId
                        )
                );
    }

    @Override
    public User enableTwoFactorAuthentication(
            VerificationType verificationType,
            String sendTo,
            User user) {

        TwoFactorAuth twoFactorAuth = new TwoFactorAuth();
        twoFactorAuth.setEnabled(true);
        twoFactorAuth.setSendTo(verificationType);

        user.setTwoFactorAuth(twoFactorAuth);
        return userRepository.save(user);
    }


    @Override
    public User updatePassword(User user, String newPassword) {
       user.setPassword(
               passwordEncoder.encode(newPassword)
       );
        return userRepository.save(user);
    }
}
