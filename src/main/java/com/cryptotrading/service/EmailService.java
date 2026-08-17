package com.cryptotrading.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${mail.from}")
    private String fromEmail;

    private static final String OTP_SUBJECT = "TradeForge - Email Verification OTP";
    private static final String OTP_MESSAGE = "Your TradeForge verification code is: %s\n\n"
            + "This OTP is valid for a limited time.\n"
            + "Please do not share this code with anyone.";

    public void sendVerificationOtpEmail(String email, String otp) {

        validateEmailAndOtp(email, otp);

        try {

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(OTP_SUBJECT);
            helper.setText(
                    String.format(OTP_MESSAGE, otp)
            );
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email",e);
        }
    }

    private void validateEmailAndOtp( String email, String otp) {

        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException( "Email cannot be empty" );
        }

        if (!StringUtils.hasText(otp)) {
            throw new IllegalArgumentException( "OTP cannot be empty" );
        }
    }
}
