package com.cryptotrading.config;

import com.cryptotrading.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Let standard security filters handle unauthenticated users
        if(auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return true;
        }

        String email = auth.getName();
        Bucket bucket = null;

        // Route to the appropriate Bucket strategy
        if(path.startsWith("/api/chat")) {
            bucket = rateLimitingService.resolveChatBucket(email);
        } else if (path.startsWith("/api/orders") || path.startsWith("/api/wallet/transfer") || path.startsWith("/api/withdrawal")) {
            bucket = rateLimitingService.resolveTradingBucket(email);
        }

        // Apply consumption
        if(bucket != null) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if(!probe.isConsumed()) {
                long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again in " + waitForRefill + " seconds.\"}");

                return false; // Drop request
            }
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        }
        return true;
    }
}
