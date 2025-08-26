package com.cosmetics.server.pattern;

class RedisKetPattern {
    public static final String OTP_KEY_PATTERN = "OTP:%s:%s"; // OTP:{username}:{purpose}
    public static final String OTP_ATTEMPTS_PATTERN = "OTP_ATTEMPTS:%s:%s"; // OTP_ATTEMPTS:{username}:{purpose}
    public static final String OTP_BLOCK_PATTERN = "OTP_BLOCK:%s"; // OTP_BLOCK:{username}

    // Rate limiting keys
    public static final String RATE_LIMIT_LOGIN_PATTERN = "RATE_LIMIT:LOGIN:%s"; // RATE_LIMIT:LOGIN:{username}
    public static final String RATE_LIMIT_OTP_SEND_PATTERN = "RATE_LIMIT:OTP_SEND:%s"; // RATE_LIMIT:OTP_SEND:{username}
    public static final String RATE_LIMIT_REGISTER_PATTERN = "RATE_LIMIT:REGISTER:%s"; // RATE_LIMIT:REGISTER:{ip}
    public static final String RATE_LIMIT_PASSWORD_RESET_PATTERN = "RATE_LIMIT:PASSWORD_RESET:%s";

    // Token blacklist
    public static final String BLACKLIST_TOKEN_PATTERN = "BLACKLIST_TOKEN:%s"; // BLACKLIST_TOKEN:{jti}
    public static final String BLACKLIST_REFRESH_TOKEN_PATTERN = "BLACKLIST_REFRESH:%s"; // BLACKLIST_REFRESH:{token_hash}

    // User session
    public static final String USER_SESSION_PATTERN = "USER_SESSION:%s"; // USER_SESSION:{user_id}
    public static final String USER_DEVICE_PATTERN = "USER_DEVICE:%s:%s"; // USER_DEVICE:{user_id}:{device_id}

    // OTP resend cooldown
    public static final String OTP_RESEND_COOLDOWN_PATTERN = "OTP_RESEND:%s:%s"; // OTP_RESEND:{username}:{purpose}
    public static final String OTP_RESEND_COUNT_PATTERN = "OTP_RESEND_COUNT:%s:%s"; // OTP_RESEND_COUNT:{username}:{purpose}

    // Challenge ID for 2FA
    public static final String CHALLENGE_PATTERN = "CHALLENGE:%s"; // CHALLENGE:{challenge_id}

    // Utility methods
    public static String buildOtpKey(String username, String purpose) {
        return String.format(OTP_KEY_PATTERN, username, purpose);
    }

    public static String buildOtpAttemptsKey(String username, String purpose) {
        return String.format(OTP_ATTEMPTS_PATTERN, username, purpose);
    }

    public static String buildOtpBlockKey(String username) {
        return String.format(OTP_BLOCK_PATTERN, username);
    }

    public static String buildRateLimitLoginKey(String username) {
        return String.format(RATE_LIMIT_LOGIN_PATTERN, username);
    }

    public static String buildRateLimitOtpSendKey(String username) {
        return String.format(RATE_LIMIT_OTP_SEND_PATTERN, username);
    }

    public static String buildRateLimitRegisterKey(String ip) {
        return String.format(RATE_LIMIT_REGISTER_PATTERN, ip);
    }

    public static String buildBlacklistTokenKey(String jti) {
        return String.format(BLACKLIST_TOKEN_PATTERN, jti);
    }

    public static String buildUserSessionKey(String userId) {
        return String.format(USER_SESSION_PATTERN, userId);
    }

    public static String buildOtpResendCooldownKey(String username, String purpose) {
        return String.format(OTP_RESEND_COOLDOWN_PATTERN, username, purpose);
    }

    public static String buildOtpResendCountKey(String username, String purpose) {
        return String.format(OTP_RESEND_COUNT_PATTERN, username, purpose);
    }

    public static String buildChallengeKey(String challengeId) {
        return String.format(CHALLENGE_PATTERN, challengeId);
    }
}
