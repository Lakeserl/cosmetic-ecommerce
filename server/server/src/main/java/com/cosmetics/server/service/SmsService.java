package com.cosmetics.server.service;

import com.cosmetics.server.entity.ENUM.OtpPurposes;
import com.cosmetics.server.exception.SmsSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.http.HttpHeaders;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    @Value("${app.sms.provider}")
    private String smsProvider;

    @Value("${app.sms.api-key}")
    private String smsApiKey;

    @Value("${app.sms.api-secret}")
    private String smsApiSecret;

    @Value("${spring.application.name}")
    private String appName;

    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    public void sendOtp(String phoneNumber, String otp, OtpPurposes purposes) {
        String message = generateSmsMessage(otp, purposes);

        String redisKey = "OTP:" + phoneNumber;
        redisTemplate.opsForValue().set(redisKey, otp, Duration.ofMinutes(5));

        switch (smsProvider.toLowerCase()) {
            case "mock" -> sendViaMock(phoneNumber, message);
            default -> sendViaMock(phoneNumber, message); // fallback
        }
    }

    private void sendViaMock(String phoneNumber, String message) {
        // Mock implementation for development/testing
        log.info("MOCK SMS to {}: {}", maskPhone(phoneNumber), message);

        if (log.isDebugEnabled()) {
            log.debug("Mock SMS content: {}", message);
        }
    }

    private String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return phoneNumber;
        }
        return phoneNumber.substring(0, 4) + "****" + phoneNumber.substring(Math.max(4, phoneNumber.length() - 2));
    }

    private String generateSmsMessage(String otp, OtpPurposes purpose) {
        return switch (purpose) {
            case REGISTER -> String.format("Welcome to %s! Your verification code is: %s. Valid for 5 minutes.", appName, otp);
            case LOGIN -> String.format("Your %s login code is: %s. Valid for 5 minutes.", appName, otp);
            case FORGET_PASSWORD, CHANGE_PASSWORD -> String.format("Your %s password reset code is: %s. Valid for 5 minutes.", appName, otp);
            case ADD_PHONE -> String.format("Verify your phone number for %s. Code: %s. Valid for 5 minutes.", appName, otp);
            case CHECKOUT -> String.format("Your %s transaction verification code is: %s. Valid for 5 minutes.", appName, otp);
            default -> String.format("Your %s verification code is: %s. Valid for 5 minutes.", appName, otp);
        };
    }


}
