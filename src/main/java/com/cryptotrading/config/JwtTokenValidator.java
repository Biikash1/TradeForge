package com.cryptotrading.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtTokenValidator extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = request.getHeader(JwtConstant.JWT_HEADER);

        /*
         * Continue normally if Authorization header
         * is missing or is not a Bearer token.
         */
        if (jwt == null || !jwt.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

          try{

              //Remove "Bearer " prefix.
            jwt = jwt.substring(7).trim();

             // Parse and validate JWT using the same
              // secret key configured in JwtProvider.
            Claims claims =
                    jwtProvider.parseToken(jwt);

            // Subject contains user's email.
            String email =
                    claims.getSubject();

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException(
                        "JWT subject is missing"
                );
            }

            // Read authorities from JWT.
            String authorities =
                    claims.get(
                            "authorities",
                            String.class
                    );

            /*
             * Convert comma-separated authorities
             * into Spring Security authorities.
             */
            var authorityList =
                    AuthorityUtils
                            .commaSeparatedStringToAuthorityList(
                                    authorities != null
                                            ? authorities
                                            : ""
                            );


             // Create authenticated user.
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorityList
                    );

            // Store authentication in SecurityContext
            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (Exception e) {

             // Invalid/expired/malformed JWT.
            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired JWT token"
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}
