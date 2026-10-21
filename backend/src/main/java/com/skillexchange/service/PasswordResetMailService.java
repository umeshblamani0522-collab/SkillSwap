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
public class PasswordResetMailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.mode:smtp}")
    private String mailMode;

    public void sendResetEmail(String to, String fullName, String resetLink, long expiryMinutes) {
        if (!"smtp".equalsIgnoreCase(mailMode)) {
            throw new BusinessRuleException("Real email delivery is required. Configure APP_MAIL_MODE=smtp before using password reset.");
        }
        if (mailHost == null || mailHost.isBlank() || from == null || from.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            throw new BusinessRuleException("Real email delivery is not configured. Set MAIL_HOST, MAIL_USERNAME, MAIL_PASSWORD and APP_MAIL_MODE=smtp in .env.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject("SkillSwap password reset");
        message.setText((fullName == null || fullName.isBlank() ? "Hello" : "Hello " + fullName) + ",\n\n"
                + "We received a request to reset your SkillSwap password.\n\n"
                + "Use this link to choose a new password:\n" + resetLink + "\n\n"
                + "This link expires in " + expiryMinutes + " minutes and can be used only once.\n\n"
                + "If you did not request this, you can safely ignore this email.\n\n"
                + "SkillSwap Security");
        mailSender.send(message);
    }
}
