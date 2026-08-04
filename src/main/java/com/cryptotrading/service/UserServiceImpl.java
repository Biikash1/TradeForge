package com.cryptotrading.service;

import com.cryptotrading.model.User;
import com.cryptotrading.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    @Override
    public User findUserProfileByJwt(String jwt) {
        return null;
    }

    @Override
    public User findUserByEmail(String email) {
        return null;
    }

    @Override
    public User findUserById(Long userId) {
        return null;
    }

    @Override
    public User enableTwoFactorAuthentication(User user) {
        return null;
    }

    @Override
    public User updatePassword(User user, String newPassword) {
        return null;
    }
}
