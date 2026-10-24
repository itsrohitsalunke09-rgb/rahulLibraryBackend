package org.ngolibrary.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.ngolibrary.service.NewsService;
import org.ngolibrary.service.PhotoStorage;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class PhotoStorageMigrationTest {

    private JdbcTemplate jdbc;
    private PhotoStorage storage;
    private PhotoStorageMigration migration;
    private final byte[] bytes = {1, 2, 3};

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        storage = mock(PhotoStorage.class);
        migration = new PhotoStorageMigration(jdbc, storage);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("public.news_photos"))).thenReturn(true);
        when(jdbc.queryForList("SELECT story_id FROM news_photos", Long.class)).thenReturn(List.of(4L));
        when(jdbc.queryForMap(anyString(), eq(4L))).thenReturn(Map.of("content_type", "image/png", "data", bytes));
    }

    @Test
    void storesEachPhotoBeforeDeletingItsRowThenDropsTheTable() {
        migration.move("news_photos", "story_id", NewsService::photoPath);
        InOrder order = inOrder(storage, jdbc);
        order.verify(storage).put("news/4", "image/png", bytes);
        order.verify(jdbc).update("DELETE FROM news_photos WHERE story_id = ?", 4L);
        order.verify(jdbc).execute("DROP TABLE news_photos");
    }

    @Test
    void keepsTheRowAndTheTableWhenStorageFails() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(storage).put(any(), any(), any());
        migration.move("news_photos", "story_id", NewsService::photoPath);
        verify(jdbc, never()).update(anyString(), any(Object[].class));
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void doesNothingWhenTheTableIsAlreadyGone() {
        when(jdbc.queryForObject(anyString(), eq(Boolean.class), eq("public.news_photos"))).thenReturn(false);
        migration.move("news_photos", "story_id", NewsService::photoPath);
        verify(storage, never()).put(any(), any(), any());
        verify(jdbc, never()).execute(anyString());
    }
}
