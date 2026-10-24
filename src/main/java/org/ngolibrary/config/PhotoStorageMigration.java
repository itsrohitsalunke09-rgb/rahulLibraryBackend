package org.ngolibrary.config;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.ngolibrary.service.NewsService;
import org.ngolibrary.service.PhotoStorage;
import org.ngolibrary.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time move of photos that used to be stored in the database (news_photos, aadhaar_photos)
 * into photo storage. Each row is deleted only after its file is stored, and a table is dropped
 * once it is empty, so an interrupted run just continues on the next start. Does nothing once done.
 */
@Component
public class PhotoStorageMigration {

    private static final Logger log = LoggerFactory.getLogger(PhotoStorageMigration.class);

    private final JdbcTemplate jdbc;
    private final PhotoStorage storage;

    public PhotoStorageMigration(JdbcTemplate jdbc, PhotoStorage storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void migrate() {
        if (!storage.isConfigured()) {
            return;
        }
        move("news_photos", "story_id", NewsService::photoPath);
        move("aadhaar_photos", "user_id", UserService::aadhaarPath);
    }

    void move(String table, String idColumn, Function<Long, String> path) {
        if (jdbc.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + table) != Boolean.TRUE) {
            return;
        }
        List<Long> ids = jdbc.queryForList("SELECT " + idColumn + " FROM " + table, Long.class);
        for (Long id : ids) {
            try {
                Map<String, Object> row = jdbc.queryForMap(
                        "SELECT content_type, data FROM " + table + " WHERE " + idColumn + " = ?", id);
                storage.put(path.apply(id), (String) row.get("content_type"), (byte[]) row.get("data"));
                jdbc.update("DELETE FROM " + table + " WHERE " + idColumn + " = ?", id);
            } catch (RuntimeException e) {
                log.error("Moving {} {} to photo storage failed; will retry on the next start: {}", table, id, e.getMessage());
                return;
            }
        }
        jdbc.execute("DROP TABLE " + table);
        log.info("Moved {} photo(s) from {} to photo storage and dropped the table", ids.size(), table);
    }
}
