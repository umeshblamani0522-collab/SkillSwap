package com.skillexchange.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.skillexchange.exception.BusinessRuleException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationMailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username:}") private String from;
    @Value("${spring.mail.host:}") private String mailHost;
    @Value("${spring.mail.password:}") private String mailPassword;
    @Value("${app.mail.mode:smtp}") private String mailMode;

    public void sendVerificationEmail(String to, String fullName, String link, long expiryMinutes) {
        if (!"smtp".equalsIgnoreCase(mailMode)) {
            throw new BusinessRuleException("Real email verification is required. Configure APP_MAIL_MODE=smtp before registering users.");
        }
        if (mailHost == null || mailHost.isBlank() || from == null || from.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            throw new BusinessRuleException("Real email verification is not configured. Set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD and APP_MAIL_MODE=smtp in .env.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) message.setFrom(from);
        message.setTo(to);
        message.setSubject("Verify your SkillSwap email address");
        message.setText((fullName == null || fullName.isBlank() ? "Hello" : "Hello " + fullName) + ",\n\n"
                + "Welcome to SkillSwap. Please verify that this email address belongs to you:\n"
                + link + "\n\nThis link expires in " + expiryMinutes + " minutes.\n\n"
                + "For security, you cannot sign in until your email address is verified.\n\nSkillSwap Security");
        mailSender.send(message);
    }
}
