package com.cryptotrading.service;

import com.cryptotrading.dto.TradeEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
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
            helper.setText(String.format(OTP_MESSAGE, otp));

            javaMailSender.send(message);
            log.info("Verification OTP email successfully sent to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send verification OTP email to {}. Reason: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private void validateEmailAndOtp(String email, String otp) {
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!StringUtils.hasText(otp)) {
            throw new IllegalArgumentException("OTP cannot be empty");
        }
    }

    public void sendTradeSuccessEmail(String email, TradeEvent event) {
       if(!StringUtils.hasText(email)) {
           log.warn("Cannot send trade success email: target email is empty for event {}", event.eventId());
           throw new IllegalArgumentException("Email cannot be empty");
       }

       try{
           MimeMessage message = javaMailSender.createMimeMessage();
           MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

           helper.setFrom(fromEmail);
           helper.setTo(email);
           helper.setSubject("TradeForge - Trade Execution Successful");

           //Build a formatted email message
           String text = String.format(
                   "Hello,\n\n" +
                           "Your trade order has been successfully executed on TradeForge.\n\n" +
                           "Order Details:\n" +
                           "Action: %s\n" +
                           "Assets: %s\n" +
                           "Quantity: %s\n" +
                           "Price: $%s\n\n" +
                           "Thank you for trading with us!",
                   event.orderType().name(),
                   event.coinId().toUpperCase(),
                   event.quantity().toPlainString(),
                   event.price().toPlainString()
           );

           helper.setText(text);
           javaMailSender.send(message);
           log.info("Trade success email sent to {} for Order ID {}", email, event.orderId());
       }catch (MessagingException e) {
           log.error("Failed to send trade success email to {} for Order ID {}. Reason: {}", email, event.orderId(), e.getMessage());
           throw new RuntimeException("Failed to send trade success email", e);
       }
    }
}
