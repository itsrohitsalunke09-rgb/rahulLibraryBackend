package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** A local stand-in for the Apps Script web app: a POST is answered with a redirect, the result comes from the GET. */
class EmailSenderTest {

    private HttpServer relay;
    private final AtomicReference<JsonNode> received = new AtomicReference<>();
    private volatile String answer = "{\"ok\":true,\"left\":99}";

    @BeforeEach
    void startRelay() throws IOException {
        relay = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        relay.createContext("/exec", ex -> {
            received.set(new ObjectMapper().readTree(ex.getRequestBody()));
            ex.getResponseHeaders().add("Location", "/result");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        relay.createContext("/result", ex -> reply(ex, answer));
        relay.start();
    }

    @AfterEach
    void stopRelay() {
        relay.stop(0);
    }

    private static void reply(HttpExchange ex, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private EmailSender sender(String secret) {
        String url = "http://127.0.0.1:" + relay.getAddress().getPort() + "/exec";
        // No Brevo key: a relay failure has nothing to fall back to.
        return new EmailSender("", "", "Rahul Library", url, secret);
    }

    @Test
    void sendsThroughTheGmailRelayFollowingItsRedirect() {
        assertTrue(sender("s3cret").send("asha@example.com", "Asha", "Your code", "Code 123456"));
        JsonNode sent = received.get();
        assertEquals("s3cret", sent.path("secret").asText());
        assertEquals("asha@example.com", sent.path("to").asText());
        assertEquals("Your code", sent.path("subject").asText());
        assertEquals("Code 123456", sent.path("text").asText());
    }

    @Test
    void aRefusedRelayFallsBackToBrevoAndFailsWhenBrevoIsNotSetUp() {
        answer = "{\"ok\":false,\"error\":\"forbidden\"}";
        assertFalse(sender("wrong").send("asha@example.com", "Asha", "Your code", "Code 123456"));
        answer = "<html>Sign in</html>";
        assertFalse(sender("s3cret").send("asha@example.com", "Asha", "Your code", "Code 123456"));
    }

    @Test
    void withoutARelayItGoesStraightToBrevo() {
        assertFalse(new EmailSender("", "", "Rahul Library", "", "").send("a@b.c", "A", "S", "T"));
        assertEquals(null, received.get());
    }
}
