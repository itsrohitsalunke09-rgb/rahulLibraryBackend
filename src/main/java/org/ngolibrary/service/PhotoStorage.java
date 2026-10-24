package org.ngolibrary.service;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Photos (news pictures, Aadhaar cards) kept as files in a private Supabase Storage bucket,
 * so they don't fill the database. Only this server can read the bucket; the API serves them.
 */
@Component
public class PhotoStorage {

    /** A stored file and its type. */
    public record Photo(String contentType, byte[] data) {
    }

    private static final Logger log = LoggerFactory.getLogger(PhotoStorage.class);

    private final RestTemplate http = new RestTemplate();
    private final String url;
    private final String key;
    private final String bucket;

    public PhotoStorage(@Value("${app.storage.url:}") String url, @Value("${app.storage.key:}") String key,
                        @Value("${app.storage.bucket:photos}") String bucket) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.key = key;
        this.bucket = bucket;
    }

    public boolean isConfigured() {
        return !url.isBlank() && !key.isBlank();
    }

    /** Stores the file, replacing any file already at that path. */
    public void put(String path, String contentType, byte[] data) {
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set("x-upsert", "true");
        try {
            http.exchange(objectUrl(path), HttpMethod.POST, new HttpEntity<>(data, headers), String.class);
        } catch (RestClientException e) {
            log.error("Could not store photo {}: {}", path, e.getMessage());
            throw unavailable();
        }
    }

    public Optional<Photo> get(String path) {
        try {
            ResponseEntity<byte[]> response = http.exchange(url + "/storage/v1/object/authenticated/" + bucket + "/" + path,
                    HttpMethod.GET, new HttpEntity<>(headers()), byte[].class);
            MediaType type = response.getHeaders().getContentType();
            return Optional.of(new Photo(type == null ? "image/jpeg" : type.toString(), response.getBody()));
        } catch (HttpClientErrorException e) {
            // shortcut: Supabase has answered a missing file with 400 as well as 404; treat both as "no photo".
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return Optional.empty();
            }
            log.error("Could not read photo {}: {}", path, e.getMessage());
            throw unavailable();
        } catch (RestClientException e) {
            log.error("Could not read photo {}: {}", path, e.getMessage());
            throw unavailable();
        }
    }

    /** Removes the file; a file that is already gone is fine. */
    public void delete(String path) {
        try {
            http.exchange(objectUrl(path), HttpMethod.DELETE, new HttpEntity<>(headers()), String.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND && e.getStatusCode() != HttpStatus.BAD_REQUEST) {
                log.error("Could not delete photo {}: {}", path, e.getMessage());
                throw unavailable();
            }
        } catch (RestClientException e) {
            log.error("Could not delete photo {}: {}", path, e.getMessage());
            throw unavailable();
        }
    }

    /** Creates the private bucket on first start; runs before the copy of old database photos. */
    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void createBucket() {
        if (!isConfigured()) {
            log.error("Photo storage is not configured: set SUPABASE_URL and SUPABASE_SECRET_KEY. Photo uploads will fail.");
            return;
        }
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            http.exchange(url + "/storage/v1/bucket", HttpMethod.POST,
                    new HttpEntity<>(Map.of("id", bucket, "name", bucket, "public", false), headers), String.class);
            log.info("Created private storage bucket {}", bucket);
        } catch (HttpClientErrorException e) {
            // Already there (Supabase answers 400 or 409): nothing to do.
        } catch (RestClientException e) {
            log.error("Could not reach photo storage: {}", e.getMessage());
        }
    }

    private String objectUrl(String path) {
        return url + "/storage/v1/object/" + bucket + "/" + path;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", key);
        // New secret keys (sb_secret_…) go only in apikey; legacy service_role keys are JWTs and also need Authorization.
        if (!key.startsWith("sb_")) {
            headers.setBearerAuth(key);
        }
        return headers;
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Photo storage is not available. Please try again.");
    }
}
