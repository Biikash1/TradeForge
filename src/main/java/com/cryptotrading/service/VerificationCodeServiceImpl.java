package com.cryptotrading.service;

import com.cryptotrading.Utils.OtpUtils;
import com.cryptotrading.domain.VerificationType;
import com.cryptotrading.model.User;
import com.cryptotrading.model.VerificationCode;
import com.cryptotrading.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;


    @Override
    public VerificationCode sendVerificationCodeByUser(User user, VerificationType verificationType) {
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setOtp(OtpUtils.generateOTP());
        verificationCode.setVerificationType(verificationType);
        verificationCode.setUser(user);
        return verificationCodeRepository.save(verificationCode);
    }

    @Override
    public VerificationCode getVerificationCodeById(Long id) throws Exception {
                return  verificationCodeRepository.findById(id)
                        .orElseThrow(() ->
                                new Exception("Verification code not found"));
    }

    @Override
    public VerificationCode getVerificationCodeByUser(Long userId) {
        return verificationCodeRepository.findByUserId(userId);
    }

    @Override
    public void deleteVerificationCodeById(VerificationCode verificationCode) {
       verificationCodeRepository.delete(verificationCode);
    }
}
