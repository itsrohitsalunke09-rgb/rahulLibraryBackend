package org.ngolibrary.service;

import java.time.Year;
import java.util.List;

import org.ngolibrary.domain.Clipping;
import org.ngolibrary.dto.ClippingRequest;
import org.ngolibrary.repo.ClippingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** कात्रणे master: clipping files by title and the years they cover, newest first. */
@Service
public class ClippingService {

    private static final int FIRST_YEAR = 1800;

    private final ClippingRepository clippings;

    public ClippingService(ClippingRepository clippings) {
        this.clippings = clippings;
    }

    public List<Clipping> list() {
        return clippings.findAllByOrderByYearFromDescTitleAsc();
    }

    public Clipping create(ClippingRequest request) {
        Clipping clipping = new Clipping();
        apply(clipping, request);
        return clippings.save(clipping);
    }

    public Clipping update(Long id, ClippingRequest request) {
        Clipping clipping = clippings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clipping not found"));
        apply(clipping, request);
        return clippings.save(clipping);
    }

    public void delete(Long id) {
        if (!clippings.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Clipping not found");
        }
        clippings.deleteById(id);
    }

    private static void apply(Clipping clipping, ClippingRequest r) {
        String title = r.title() == null ? "" : r.title().trim();
        if (title.isEmpty() || title.length() > 200) {
            throw bad("Enter the title (up to 200 characters).");
        }
        int thisYear = Year.now().getValue();
        Integer from = r.yearFrom();
        Integer to = r.yearTo() == null ? from : r.yearTo();
        if (from == null || from < FIRST_YEAR || from > thisYear || to < FIRST_YEAR || to > thisYear) {
            throw bad("Enter years between " + FIRST_YEAR + " and " + thisYear + ".");
        }
        if (to < from) {
            throw bad("\"To\" year must be the same as or after \"From\" year.");
        }
        clipping.setTitle(title);
        clipping.setYearFrom(from);
        clipping.setYearTo(to);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
