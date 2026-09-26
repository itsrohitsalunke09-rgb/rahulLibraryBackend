package org.ngolibrary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppServiceImpl implements WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppServiceImpl.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.whatsapp.provider:log}")
    private String provider;

    @Value("${app.whatsapp.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.whatsapp.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.whatsapp.twilio.from-number:}")
    private String twilioFromNumber;

    @Value("${app.whatsapp.gupshup.api-key:}")
    private String gupshupApiKey;

    @Value("${app.whatsapp.gupshup.source-number:}")
    private String gupshupSourceNumber;

    @Value("${app.whatsapp.custom.api-url:}")
    private String customApiUrl;

    @Value("${app.whatsapp.custom.api-key:}")
    private String customApiKey;

    @Override
    public void sendOverdueNotification(String toPhone, String studentName, String bookTitle, String dueDate) {
        String message = String.format(
            "Dear %s,\n\nYour book \"%s\" was due on %s and is now OVERDUE.\nPlease return it to the library as soon as possible to avoid fines.\n\nThank you,\nPathshala Library",
            studentName, bookTitle, dueDate
        );
        sendMessage(toPhone, message);
    }

    @Override
    public void sendDueSoonNotification(String toPhone, String studentName, String bookTitle, String dueDate, int daysLeft) {
        String message = String.format(
            "Dear %s,\n\nReminder: Your book \"%s\" is due on %s (%d day%s left).\nPlease return or renew it on time.\n\nThank you,\nPathshala Library",
            studentName, bookTitle, dueDate, daysLeft, daysLeft == 1 ? "" : "s"
        );
        sendMessage(toPhone, message);
    }

    private void sendMessage(String toPhone, String message) {
        if (toPhone == null || toPhone.trim().isEmpty()) {
            log.warn("Cannot send WhatsApp: empty phone number");
            return;
        }

        String formattedPhone = formatPhoneNumber(toPhone);

        switch (provider.toLowerCase()) {
            case "twilio" -> sendViaTwilio(formattedPhone, message);
            case "gupshup" -> sendViaGupshup(formattedPhone, message);
            case "custom" -> sendViaCustomApi(formattedPhone, message);
            default -> log.info("WhatsApp (LOG MODE) to {}: {}", formattedPhone, message);
        }
    }

    private String formatPhoneNumber(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits;
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            return digits;
        }
        return digits;
    }

    private void sendViaTwilio(String toPhone, String message) {
        if (twilioAccountSid.isEmpty() || twilioAuthToken.isEmpty() || twilioFromNumber.isEmpty()) {
            log.warn("Twilio credentials not configured, falling back to LOG mode");
            log.info("WhatsApp (LOG MODE) to {}: {}", toPhone, message);
            return;
        }

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Messages.json";
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(twilioAccountSid, twilioAuthToken);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", "whatsapp:" + twilioFromNumber);
            body.add("To", "whatsapp:" + toPhone);
            body.add("Body", message);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            log.info("WhatsApp sent via Twilio to {}", toPhone);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp via Twilio to {}: {}", toPhone, e.getMessage());
        }
    }

    private void sendViaGupshup(String toPhone, String message) {
        if (gupshupApiKey.isEmpty() || gupshupSourceNumber.isEmpty()) {
            log.warn("Gupshup credentials not configured, falling back to LOG mode");
            log.info("WhatsApp (LOG MODE) to {}: {}", toPhone, message);
            return;
        }

        try {
            String url = "https://api.gupshup.io/sm/api/v1/msg";
            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", gupshupApiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("channel", "whatsapp");
            body.add("source", gupshupSourceNumber);
            body.add("destination", toPhone);
            body.add("message", "{\"type\": \"text\", \"text\": \"" + message.replace("\"", "\\\"").replace("\n", "\\n") + "\"}");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, request, String.class);
            log.info("WhatsApp sent via Gupshup to {}", toPhone);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp via Gupshup to {}: {}", toPhone, e.getMessage());
        }
    }

    private void sendViaCustomApi(String toPhone, String message) {
        if (customApiUrl.isEmpty()) {
            log.warn("Custom WhatsApp API URL not configured, falling back to LOG mode");
            log.info("WhatsApp (LOG MODE) to {}: {}", toPhone, message);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!customApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + customApiKey);
            }

            String json = String.format("{\"to\": \"%s\", \"message\": \"%s\"}", toPhone, message.replace("\"", "\\\"").replace("\n", "\\n"));

            HttpEntity<String> request = new HttpEntity<>(json, headers);
            restTemplate.postForObject(customApiUrl, request, String.class);
            log.info("WhatsApp sent via Custom API to {}", toPhone);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp via Custom API to {}: {}", toPhone, e.getMessage());
        }
    }
}