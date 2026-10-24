package org.ngolibrary.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Sends email through Brevo's HTTP API. Render's free plan blocks SMTP ports, so plain
 * SMTP (e.g. Gmail) can't be used from the server.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate http = new RestTemplate();
    private final String apiKey;
    private final String from;
    private final String fromName;

    public EmailSender(@Value("${app.email.brevo-api-key:}") String apiKey, @Value("${app.email.from:}") String from,
                       @Value("${app.email.from-name:Rahul Library}") String fromName) {
        this.apiKey = apiKey;
        this.from = from;
        this.fromName = fromName;
    }

    /** Sends a plain-text email; returns whether Brevo accepted it. */
    public boolean send(String to, String toName, String subject, String text) {
        if (apiKey.isBlank() || from.isBlank()) {
            log.error("Email is not configured: set BREVO_API_KEY and MAIL_FROM. Could not send \"{}\".", subject);
            return false;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", fromName, "email", from),
                "to", List.of(Map.of("email", to, "name", toName)),
                "subject", subject,
                "textContent", text);
        try {
            http.postForObject(BREVO_URL, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (RestClientException e) {
            log.error("Brevo did not send \"{}\": {}", subject, e.getMessage());
            return false;
        }
    }
}
