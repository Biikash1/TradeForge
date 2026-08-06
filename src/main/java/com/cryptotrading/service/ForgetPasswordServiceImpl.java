package com.cryptotrading.service;

import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.ForgetPasswordToken;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.ForgetPasswordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ForgetPasswordServiceImpl implements ForgetPasswordService{

    private final ForgetPasswordRepository forgetPasswordRepository;

    @Override
    public ForgetPasswordToken createToken(User user, String id, String otp, VerificationType verificationType, String sendTo) {
         ForgetPasswordToken token = new ForgetPasswordToken();
         token.setUser(user);
         token.setSendTo(sendTo);
         token.setVerificationType(verificationType);
         token.setOtp(otp);
         token.setId(id);
        return forgetPasswordRepository.save(token);
    }

    @Override
    public ForgetPasswordToken findById(String id) {
        Optional<ForgetPasswordToken> token = forgetPasswordRepository.findById(id);
        return token.orElse(null);
    }

    @Override
    public ForgetPasswordToken findByUser(Long userId) {
        return forgetPasswordRepository.findByUserId(userId);
    }

    @Override
    public void deleteToken(ForgetPasswordToken token) {
        forgetPasswordRepository.delete(token);
    }
}
