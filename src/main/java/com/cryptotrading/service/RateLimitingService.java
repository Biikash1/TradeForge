package com.cryptotrading.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final ProxyManager<byte[]> proxyManager;

    public Bucket resolveChatBucket(String userId) {
        Supplier<BucketConfiguration> configSupplier = () -> {
            // 20 requests per hour for the AI ChatBot
            Refill refill = Refill.intervally(20, Duration.ofHours(1));
            Bandwidth limit = Bandwidth.classic(20, refill);
            return BucketConfiguration.builder().addLimit(limit).build();
        };
        return proxyManager.builder().build(("chat_limit_" + userId).getBytes(), configSupplier);
    }

    public Bucket resolveTradingBucket(String userId) {
        Supplier<BucketConfiguration> configSupplier = () -> {
            // 80 requests per minute for Trading & Financial APIs
            Refill refill = Refill.intervally(80, Duration.ofMinutes(1));
            Bandwidth limit = Bandwidth.classic(80, refill);
            return BucketConfiguration.builder().addLimit(limit).build();
        };
        return proxyManager.builder().build(("trading_limit_" + userId).getBytes(), configSupplier);
    }
}
