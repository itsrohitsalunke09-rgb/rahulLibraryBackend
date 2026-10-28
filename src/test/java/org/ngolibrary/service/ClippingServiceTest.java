package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Year;

import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.Clipping;
import org.ngolibrary.dto.ClippingRequest;
import org.ngolibrary.repo.ClippingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ClippingServiceTest {

    private final ClippingRepository repo = mock(ClippingRepository.class);
    private final ClippingService service = new ClippingService(repo);

    {
        when(repo.save(any(Clipping.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void aClippingIsATitleAndTheYearsItSpans() {
        Clipping c = service.create(new ClippingRequest(" स्वातंत्र्य लढा ", 1990, 2005));
        assertEquals("स्वातंत्र्य लढा", c.getTitle());
        assertEquals(1990, c.getYearFrom());
        assertEquals(2005, c.getYearTo());
        // No "to" year: just the one year.
        assertEquals(2001, service.create(new ClippingRequest("Kargil", 2001, null)).getYearTo());
    }

    @Test
    void refusesABlankTitleOrYearsOutOfOrder() {
        assertBad(new ClippingRequest(" ", 2000, 2001));
        assertBad(new ClippingRequest("Elections", 2005, 1999));
        assertBad(new ClippingRequest("Elections", null, 2001));
        assertBad(new ClippingRequest("Elections", 2000, Year.now().getValue() + 1));
    }

    private void assertBad(ClippingRequest request) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.create(request));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }
}
