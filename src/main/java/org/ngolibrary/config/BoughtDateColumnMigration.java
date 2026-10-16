package org.ngolibrary.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time column clean-up on the books table: the book's date used to live in a
 * "date" column next to an unused, empty "bought_date" column. This drops the empty
 * bought_date and renames date to bought_date, keeping every value. If bought_date
 * holds any data it is left alone and nothing is changed. Does nothing once done.
 */
@Component
public class BoughtDateColumnMigration {

    private static final Logger log = LoggerFactory.getLogger(BoughtDateColumnMigration.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public BoughtDateColumnMigration(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() throws SQLException {
        Map<String, String> columns = bookColumns();
        String dateColumn = columns.get("date");
        if (dateColumn == null) {
            return;
        }
        String boughtColumn = columns.get("bought_date");
        if (boughtColumn != null) {
            Long filled = jdbc.queryForObject(
                    "SELECT count(*) FROM books WHERE " + quote(boughtColumn) + " IS NOT NULL", Long.class);
            if (filled != null && filled > 0) {
                log.warn("books has both date and bought_date, and bought_date has {} value(s); "
                        + "leaving both columns unchanged", filled);
                return;
            }
            jdbc.execute("ALTER TABLE books DROP COLUMN " + quote(boughtColumn));
        }
        jdbc.execute("ALTER TABLE books RENAME COLUMN " + quote(dateColumn) + " TO bought_date");
        log.info("books: renamed column date to bought_date{}", boughtColumn != null ? " (dropped the empty bought_date)" : "");
    }

    /** Lower-case column name -> name as the database reports it. */
    private Map<String, String> bookColumns() throws SQLException {
        Map<String, String> columns = new HashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            for (String table : new String[] {"books", "BOOKS"}) {
                try (ResultSet rs = meta.getColumns(null, connection.getSchema(), table, null)) {
                    while (rs.next()) {
                        String name = rs.getString("COLUMN_NAME");
                        columns.put(name.toLowerCase(Locale.ROOT), name);
                    }
                }
            }
        }
        return columns;
    }

    private static String quote(String column) {
        return "\"" + column + "\"";
    }
}
