//package com.cryptotrading;
//
//import io.github.bucket4j.distributed.proxy.ProxyManager;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//
//@SpringBootTest(properties = {
//        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL",
//        "spring.datasource.driverClassName=org.h2.Driver",
//        "spring.datasource.username=sa",
//        "spring.datasource.password=",
//        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
//        "spring.jpa.hibernate.ddl-auto=create-drop",
//        "jwt.secret=dGhpc2lzYXZlcnlzZWN1cmV0ZXN0a2V5dGhhdGlzYXRsZWFzdDMyYnl0ZXM=",
//        "jwt.expiration=86400000",
//        "razorpay.key.id=test_id",
//        "razorpay.key.secret=test_secret",
//        "razorpay.callback.url=http://localhost",
//        "stripe.key.secret=test_secret",
//        "stripe.success.url=http://localhost",
//        "stripe.cancel.url=http://localhost",
//        "gemini.api.base-url=http://localhost",
//        "gemini.api.key=test_key",
//        "gemini.api.model=test_model",
//        "gemini.api.timeout.connect=5000",
//        "gemini.api.timeout.read=5000",
//        "mail.from=test@tradeforge.com"
//})
//class TradeForgeApplicationTests {
//
//    // 1. Mocks Redis connections
//    @MockitoBean
//    private LettuceConnectionFactory lettuceConnectionFactory;
//
//    @MockitoBean
//    private ProxyManager<byte[]> proxyManager;
//
//    // 2. Mocks Email Sender
//    @MockitoBean
//    private JavaMailSender javaMailSender;
//
//    @Test
//    void contextLoads() {
//    }
//
//}