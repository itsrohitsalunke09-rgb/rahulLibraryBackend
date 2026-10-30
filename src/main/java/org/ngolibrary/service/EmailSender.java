package org.ngolibrary.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sends email over HTTPS (Render's free plan blocks SMTP ports, so plain SMTP can't be used).
 * First choice: the library's Gmail, through a Google Apps Script relay (scripts/gmail-relay.gs);
 * Gmail really sends it, so it reaches the inbox. Fallback: Brevo's API, used when the relay
 * isn't set up or fails (Brevo's mail from a @gmail.com sender often lands in spam).
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate http = new RestTemplate();
    // Apps Script answers a POST with a redirect to the result; NORMAL follows it (as a GET).
    private final HttpClient relayClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;
    private final String from;
    private final String fromName;
    private final String relayUrl;
    private final String relaySecret;

    public EmailSender(@Value("${app.email.brevo-api-key:}") String apiKey, @Value("${app.email.from:}") String from,
                       @Value("${app.email.from-name:Rahul Library}") String fromName,
                       @Value("${app.email.gmail-relay-url:}") String relayUrl,
                       @Value("${app.email.gmail-relay-secret:}") String relaySecret) {
        this.apiKey = apiKey;
        this.from = from;
        this.fromName = fromName;
        this.relayUrl = relayUrl;
        this.relaySecret = relaySecret;
    }

    /** Sends a plain-text email: Gmail relay first, then Brevo; returns whether either accepted it. */
    public boolean send(String to, String toName, String subject, String text) {
        if (!relayUrl.isBlank() && !relaySecret.isBlank() && sendWithGmail(to, toName, subject, text)) {
            return true;
        }
        return sendWithBrevo(to, toName, subject, text);
    }

    /** Posts to the Apps Script relay; true only when it answers {"ok": true}. */
    private boolean sendWithGmail(String to, String toName, String subject, String text) {
        try {
            String body = JSON.writeValueAsString(Map.of(
                    "secret", relaySecret, "to", to, "toName", toName == null ? "" : toName, "fromName", fromName,
                    "subject", subject, "text", text));
            HttpRequest request = HttpRequest.newBuilder(URI.create(relayUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = relayClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode reply = JSON.readTree(response.body());
            if (reply.path("ok").asBoolean(false)) {
                return true;
            }
            log.error("Gmail relay did not send \"{}\" (HTTP {}): {}; trying Brevo.",
                    subject, response.statusCode(), reply.path("error").asText("no reason given"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Gmail relay interrupted for \"{}\"; trying Brevo.", subject);
        } catch (Exception e) {
            // Includes a non-JSON reply, e.g. Google's sign-in page when the web app isn't open to "Anyone".
            log.error("Gmail relay failed for \"{}\": {}; trying Brevo.", subject, e.getMessage());
        }
        return false;
    }

    private boolean sendWithBrevo(String to, String toName, String subject, String text) {
        if (apiKey.isBlank() || from.isBlank()) {
            log.error("Email is not configured: set GMAIL_RELAY_URL and GMAIL_RELAY_SECRET (or BREVO_API_KEY and MAIL_FROM). "
                    + "Could not send \"{}\".", subject);
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
