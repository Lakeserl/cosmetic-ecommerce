package com.cosmetics.server.service;

import com.cosmetics.server.entity.ENUM.OtpPurposes;
import com.cosmetics.server.exception.OtpBlockedException;
import com.cosmetics.server.exception.OtpCooldownException;
import com.cosmetics.server.exception.OtpRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    @Value("${app.otp.expiration}")
    private Duration expiration;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    // durations in milliseconds
    @Value("${app.otp.block-duration}")
    private long blockDuration;

    @Value("${app.otp.resend-limit}")
    private int resendLimit;

    @Value("${app.otp.resend-window}")
    private long resendWindow;

    @Value("${app.otp.cooldown}")
    private long cooldown;

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final SmsService smsService;
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendOtp(String username, OtpPurposes purposes) {
        validateRateLimit(username, purposes);

        String otp = generateOtp();
        String key = buildOtpKey(username, purposes);

        OtpData otpData = OtpData.builder()
                .otp(otp)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plus(expiration))
                .purpose(purposes)
                .used(false)
                .build();

        // store OTP with TTL
        redisTemplate.opsForValue().set(key, otpData, expiration);

        if (isEmail(username)) {
            emailService.sendOtpMail(username, otp, purposes);
        } else {
            smsService.sendOtp(username, otp, purposes);
        }

        updateResendTracking(username, purposes);

        log.info("OTP sent to {} for purpose {}", maskUsername(username), purposes);
    }

    private void updateResendTracking(String username, OtpPurposes purposes) {
        String resendKey = buildResendKey(username, purposes);
        ResendTracker tracker = (ResendTracker) redisTemplate.opsForValue().get(resendKey);

        LocalDateTime now = LocalDateTime.now();
        if (tracker == null) {
            tracker = ResendTracker.builder()
                    .count(1)
                    .firstSentAt(now)
                    .lastSentAt(now)
                    .build();
        } else {
            tracker.setCount(tracker.getCount() + 1);
            tracker.setLastSentAt(now);
        }

        // store tracker with resendWindow TTL (resendWindow in millis)
        redisTemplate.opsForValue().set(resendKey, tracker, Duration.ofMillis(resendWindow));
    }

    public boolean verifyOtp(String username, String otp, OtpPurposes purposes) {
        String key = buildOtpKey(username, purposes);
        OtpData otpData = (OtpData) redisTemplate.opsForValue().get(key);

        if (otpData == null) {
            log.warn("OTP expired or not found for {} and purpose {}", maskUsername(username), purposes);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (otpData.getExpiresAt() != null && now.isAfter(otpData.getExpiresAt())) {
            redisTemplate.delete(key);
            log.warn("OTP expired for {} and purpose {}", maskUsername(username), purposes);
            return false;
        }

        if (otpData.isUsed()) {
            log.warn("OTP already used for {} and purpose {}", maskUsername(username), purposes);
            return false;
        }

        otpData.setAttempts(otpData.getAttempts() + 1);

        if (!otp.equals(otpData.getOtp())) {
            if (otpData.getAttempts() >= maxAttempts) {
                blockUser(username, purposes);
                redisTemplate.delete(key);
                log.warn("Max OTP attempts exceeded for {} and purpose {}", maskUsername(username), purposes);
            } else {
                // keep the remaining TTL
                Duration remaining = Duration.between(now, otpData.getExpiresAt());
                if (!remaining.isNegative() && !remaining.isZero()) {
                    redisTemplate.opsForValue().set(key, otpData, remaining);
                } else {
                    redisTemplate.delete(key);
                }
            }
            return false;
        }

        // correct otp
        otpData.setUsed(true);
        // keep record for short time for audit (5 seconds)
        redisTemplate.opsForValue().set(key, otpData, Duration.ofSeconds(5));
        clearResendTracking(username, purposes);

        log.info("OTP verified successfully for {} and purpose {}", maskUsername(username), purposes);
        return true;
    }

    private void validateRateLimit(String username, OtpPurposes purposes) {
        if (isBlocked(username, purposes)) {
            throw new OtpBlockedException("User is blocked for OTP requests");
        }

        String resendKey = buildResendKey(username, purposes);
        ResendTracker tracker = (ResendTracker) redisTemplate.opsForValue().get(resendKey);

        if (tracker != null) {
            if (tracker.getCount() >= resendLimit) {
                throw new OtpRateLimitException("Resend limit exceeded");
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime allowedAt = tracker.getLastSentAt().plus(Duration.ofMillis(cooldown));
            if (now.isBefore(allowedAt)) {
                throw new OtpCooldownException("Please wait before requesting another OTP");
            }
        }
    }

    private void clearResendTracking(String username, OtpPurposes purposes) {
        String resendKey = buildResendKey(username, purposes);
        redisTemplate.delete(resendKey);
    }

    private boolean isBlocked(String username, OtpPurposes purposes) {
        String blockKey = buildBlockKey(username, purposes);
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockKey));
    }

    private void blockUser(String username, OtpPurposes purposes) {
        String blockKey = buildBlockKey(username, purposes);
        // set blocked value with TTL blockDuration (millis)
        redisTemplate.opsForValue().set(blockKey, "blocked", Duration.ofMillis(blockDuration));
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private String buildOtpKey(String username, OtpPurposes purposes) {
        return String.format("otp:%s:%s", normalizationUserName(username), purposes);
    }

    private String buildResendKey(String username, OtpPurposes purposes) {
        return String.format("otp_resend:%s:%s", normalizationUserName(username), purposes);
    }

    private String buildBlockKey(String username, OtpPurposes purposes) {
        return String.format("otp_block:%s:%s", normalizationUserName(username), purposes);
    }

    private String normalizationUserName(String username) {
        if (username == null) return "";
        username = username.trim();
        if (isEmail(username)) {
            return username.toLowerCase();
        } else {
            // basic phone cleanup: keep digits and leading plus
            String cleaned = username.replaceAll("[^+\\d]", "");
            return cleaned;
        }
    }

    private boolean isEmail(String username) {
        return username != null && username.contains("@");
    }

    private String maskUsername(String username) {
        if (username == null) return "***";
        username = username.trim();
        if (isEmail(username)) {
            String[] parts = username.split("@", 2);
            String local = parts[0];
            String domain = parts.length > 1 ? parts[1] : "";
            int visible = Math.min(3, local.length());
            return local.substring(0, visible) + "***@" + domain;
        } else {
            // for phone: show last 4 digits if possible
            String cleaned = normalizationUserName(username);
            if (cleaned.length() <= 4) {
                return "****";
            } else if (cleaned.startsWith("+")) {
                String last = cleaned.substring(Math.max(0, cleaned.length() - 4));
                return "+" + "***" + last;
            } else {
                String last = cleaned.substring(cleaned.length() - 4);
                return "***" + last;
            }
        }
    }
}
