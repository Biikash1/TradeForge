package com.cryptotrading.service;

import com.cryptotrading.model.RefreshToken;
import com.cryptotrading.model.User;
import com.cryptotrading.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDurationMs = 7L * 24L * 60L * 60L * 1000L;

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);
        Instant expiryDate = Instant.now().plusMillis(refreshTokenDurationMs);

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
                .map(existing -> {
                    existing.setTokenHash(tokenHash);
                    existing.setExpiryDate(expiryDate);
                    return existing;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .tokenHash(tokenHash)
                        .expiryDate(expiryDate)
                        .build());

        refreshTokenRepository.save(refreshToken);
        return rawToken; // Returned once to be placed in the HttpOnly cookie
    }

    public RefreshToken findByRawToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new BadCredentialsException("Refresh token expired. Please sign in again.");
        }
        return token;
    }

    @Transactional
    public String rotateRefreshToken(RefreshToken existingToken) {
        String newRawToken = UUID.randomUUID().toString();
        existingToken.setTokenHash(hashToken(newRawToken));
        existingToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshTokenRepository.save(existingToken);
        return newRawToken;
    }

    @Transactional
    public void deleteByRawToken(String rawToken) {
        refreshTokenRepository.deleteByTokenHash(hashToken(rawToken));
    }
}