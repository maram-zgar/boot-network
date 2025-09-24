package dev.maram.boot_network.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendEmail(
            String to,
            String userName,
            EmailTemplateName emailTemplate,
            String confirmationUrl,
            String activationCode,
            String subject
    ) throws MessagingException {
        log.info("Starting email sending process to: {}", to);

        String templateName;
        if(emailTemplate == null) {
            templateName = "confirm-email";
            log.info("Using default email template: confirm-email");
        } else {
            templateName = emailTemplate.name();
            log.info("Using email template: {}", templateName);
        }

        // Create the base email message
        log.info("Creating MIME message for email to: {}", to);
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        // Helper to set email properties easily
        log.info("Setting up MIME message helper");
        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED, // allows multiple parts like HTML + attachments
                StandardCharsets.UTF_8.name() // ensures correct encoding
        );
        Map<String, Object> properties = new HashMap<>();
        properties.put("userName", userName);
        properties.put("confirmationUrl", confirmationUrl);
        properties.put("activation_code", activationCode);

        log.info("Email properties set for user: {}", userName);

        Context context = new Context();
        context.setVariables(properties);
        log.info("Thymeleaf context prepared");

        helper.setFrom("test@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        log.info("Email headers set - From: test@gmail.com, To: {}, Subject: {}", to, subject);

        log.info("Processing email template: {}", templateName);
        String template = templateEngine.process(templateName, context);
        log.info("Email template processed successfully");

        helper.setText(template, true);
        log.info("Email content set with HTML formatting");

        log.info("Sending email to: {}", to);
        mailSender.send(mimeMessage);
        log.info("Email sent successfully to: {}", to);

    }

}
