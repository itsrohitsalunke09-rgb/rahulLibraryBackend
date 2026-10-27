package org.ngolibrary.web;

import java.time.LocalDateTime;

import org.ngolibrary.domain.SiteContent;
import org.ngolibrary.repo.SiteContentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The About Us page content: read by anyone (it opens from the login screen), edited by staff.
 * The app owns the document's shape; the server only checks that it is a JSON object of sane size.
 */
@RestController
@RequestMapping("/api/about")
public class AboutController {

    static final String PAGE = "about";
    static final int MAX_LENGTH = 50_000;

    private final SiteContentRepository content;
    private final ObjectMapper json;

    public AboutController(SiteContentRepository content, ObjectMapper json) {
        this.content = content;
        this.json = json;
    }

    /** The saved content, or 204 when none has been saved yet (the app then shows its built-in text). */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> get() {
        return content.findById(PAGE)
                .map(c -> ResponseEntity.ok(c.getJson()))
                .orElse(ResponseEntity.noContent().build());
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public String save(@RequestBody String body) {
        if (body == null || body.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The About Us text is too long.");
        }
        JsonNode node;
        try {
            node = json.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The About Us content could not be read.");
        }
        if (node == null || !node.isObject()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The About Us content could not be read.");
        }
        SiteContent about = content.findById(PAGE).orElseGet(SiteContent::new);
        about.setPage(PAGE);
        about.setJson(node.toString());
        about.setUpdatedAt(LocalDateTime.now());
        return content.save(about).getJson();
    }
}
