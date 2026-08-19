package com.cryptotrading.Utils;

import java.security.SecureRandom;

public final class OtpUtils {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private OtpUtils() {
    }

    public static String generateOTP() {

        int otp = 100000 + SECURE_RANDOM.nextInt(900000);

        return String.valueOf(otp);
    }
}