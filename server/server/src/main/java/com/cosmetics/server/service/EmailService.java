package com.cosmetics.server.service;

import com.cosmetics.server.entity.ENUM.OtpPurposes;
import com.cosmetics.server.exception.EmailSendException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;


    @Value("${spring.application.name}")
    private String appName;

    private String maskEmail(String email) {
        if(email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String userName = parts[0];

        if(userName.length() <= 3){
            return userName.charAt(0) + "***@" + parts[1];
        }

        return userName.substring(0, 3) + "***@" + parts[1];
    }

    private String getSubject(OtpPurposes purposes) {
        return switch (purposes) {
            case REGISTER -> "Verify your " + appName + "account";
            case LOGIN -> "Login verification code";
            case FORGET_PASSWORD ,CHANGE_PASSWORD  -> "Password reset verification code";
            case ADD_EMAIL -> "Verification your email address";
            case ADD_PHONE -> "Verification your phone number";
            default -> "verification code";
        };
    }

    @Async
    public void sendOtpMail(String toEmail, String otp, OtpPurposes otpPurposes) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject(getSubject(otpPurposes));

            String htmlContent = generateEmailContent(otp, otpPurposes);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("OTP email sent successfully to: {}", maskEmail(toEmail));
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", maskEmail(toEmail), e);
            throw new EmailSendException("Failed to send OTP email", e);
        }
    }

    @Async
    public void sendWelcomeHelper(String toEmail, String firstName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to " + appName + "!");

            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);

            String htmlContent = templateEngine.process("welcome.html", context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Welcome to {}!", maskEmail(toEmail));

        }catch (Exception e) {
            log.error("Failed to send Welcome to: {}", maskEmail(toEmail), e);
        }
    }

    @Async
    public void sendPasswordResetConfirmation(String toEmail, String firstName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(toEmail);
            helper.setSubject("Password reset successful");

            Context context = new Context();
            context.setVariable("firstName", firstName);
            context.setVariable("appName", appName);
            context.setVariable("timestamp", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            ));

            String htmlContent = templateEngine.process("password-reset-confirmation", context);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Password reset successfully to: {}", maskEmail(toEmail));
        } catch (Exception e) {
            log.error("Failed to send password reset confirmation to: {}", maskEmail(toEmail), e);
        }
    }

    private String generateEmailContent(String otp, OtpPurposes otpPurposes) {
        Context context = new Context();
        context.setVariable("otp", otp);
        context.setVariable("appName", appName);
        context.setVariable("otpPurposes", otpPurposes);
        context.setVariable("expiryMinutes", 5);

        String template = switch (otpPurposes) {
            case REGISTER -> "registration-otp";
            case LOGIN -> "login-otp";
            case FORGET_PASSWORD,CHANGE_PASSWORD -> "password-reset-otp";
            case ADD_EMAIL -> "email-verification-otp";
            case ADD_PHONE -> "email-verification-otp";
            case CHECKOUT -> "checkout-otp";
            default -> "generic-otp";
        };
        return templateEngine.process(template, context);
    }
}
