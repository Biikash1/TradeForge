package com.cryptotrading.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

public class JwtTokenValidator extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        // `Bearer ` token
        if(jwt != null && jwt.startsWith("Bearer ")){
            jwt = jwt.substring(7);

            try {
                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JwtConstant.SECRET_KEY));;

                Claims claims =  Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(jwt)
                        .getPayload();

                String email = String.valueOf(claims.get("email", String.class));

                String authorities = String.valueOf(claims.get("authorities", String.class));

                List<GrantedAuthority> authorityList = AuthorityUtils.commaSeparatedStringToAuthorityList(authorities);

                Authentication auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorityList
                );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");
            }
        }
        filterChain.doFilter(request, response);
    }
}
