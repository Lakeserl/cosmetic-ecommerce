package com.cosmetics.server.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class EmailOrPhoneValidator implements ConstraintValidator<EmailOrPhone, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+84|84|0)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$|"
//                   + "^(\\+84|84|0)(2[0-9])[0-9]{8}$|" + // Vietnamese landline
//                    "^\\+[1-9]\\d{1,14}$" // International E.164 format
    );

    @Override
    public void initialize(EmailOrPhone constraintAnnotation) {
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if(s == null || s.trim().isEmpty()){
            return false;
        }

        String trimmed = s.trim();

        if(trimmed.contains("@")){
            return EMAIL_PATTERN.matcher(trimmed).matches();
        }

        String normalized = normalizePhone(trimmed);
        return PHONE_PATTERN.matcher(normalized).matches();
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";

        String cleaned = phone.replaceAll("[^+\\d]", "");

        // Convert Vietnamese local format to international
        if (cleaned.startsWith("0")) {
            cleaned = "+84" + cleaned.substring(1);
        } else if (cleaned.startsWith("84") && !cleaned.startsWith("+84")) {
            cleaned = "+" + cleaned;
        }

        return cleaned;
    }
}
