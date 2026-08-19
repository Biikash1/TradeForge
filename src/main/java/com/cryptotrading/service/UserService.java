package com.cryptotrading.service;

import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.User;

public interface UserService {

     User findUserProfileByJwt(String jwt);
     User findUserByEmail(String email);
     User findUserById(Long userId) ;

     User enableTwoFactorAuthentication(
            VerificationType verificationType,
            String sendTo,
            User user
    );

    User updatePassword(User user, String newPassword);
}
