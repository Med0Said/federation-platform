package com.federation.common.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text e-mails through SMTP when {@code spring.mail.host} is configured.
 * Without SMTP (local development) the message is written to the log instead, so
 * flows such as password reset remain testable end to end.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.from:no-reply@federation.local}")
    private String from;

    /** @return {@code true} when the message was handed to an SMTP server */
    public boolean send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("[mail disabled] To: {} | Subject: {}\n{}", to, subject, body);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return true;
        } catch (MailException e) {
            log.error("Failed to send e-mail to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
