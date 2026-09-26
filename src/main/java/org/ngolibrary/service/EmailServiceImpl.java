package org.ngolibrary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean enabled;

    @Value("${app.email.from:noreply@ngolibrary.org}")
    private String fromEmail;

    @Value("${app.email.from-name:Pathshala Library}")
    private String fromName;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOverdueNotification(String toEmail, String studentName, String bookTitle, String dueDate) {
        String subject = "OVERDUE: \"" + bookTitle + "\" was due on " + dueDate;
        String body = String.format(
            "Dear %s,\n\n" +
            "Your book \"%s\" was due on %s and is now OVERDUE.\n" +
            "Please return it to the library as soon as possible to avoid fines.\n\n" +
            "Thank you,\n" +
            "Pathshala Library",
            studentName, bookTitle, dueDate
        );
        sendEmail(toEmail, subject, body);
    }

    @Override
    public void sendDueSoonNotification(String toEmail, String studentName, String bookTitle, String dueDate, int daysLeft) {
        String subject = "REMINDER: \"" + bookTitle + "\" due in " + daysLeft + " day" + (daysLeft == 1 ? "" : "s");
        String body = String.format(
            "Dear %s,\n\n" +
            "Reminder: Your book \"%s\" is due on %s (%d day%s left).\n" +
            "Please return or renew it on time.\n\n" +
            "Thank you,\n" +
            "Pathshala Library",
            studentName, bookTitle, dueDate, daysLeft, daysLeft == 1 ? "" : "s"
        );
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String toEmail, String subject, String body) {
        if (!enabled) {
            log.info("EMAIL (DISABLED) to {}: Subject: {}", toEmail, subject);
            log.debug("Email body:\n{}", body);
            return;
        }

        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Cannot send email: empty email address");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromEmail + ">");
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }
}