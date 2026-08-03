package com.cryptotrading.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class JwtProvider {

    private static SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtConstant.SECRET_KEY));

    public static String generateToken(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String roles = populateAuthorities(authorities);

        String jwt = Jwts.builder()
                . issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .claim("email", authentication.getName())
                .claim("authorities", roles)
                .signWith(key)
                .compact();
        return jwt;
    }

    public static String getEmailFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("email", String.class);
    }

    private static String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auth = new HashSet<>();
        for(GrantedAuthority grant : authorities) {
            auth.add(grant.getAuthority());
        }
        return String.join(",", auth);
    }
}
