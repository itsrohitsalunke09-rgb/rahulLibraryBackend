package org.ngolibrary.web;

import java.util.List;

import org.ngolibrary.domain.Clipping;
import org.ngolibrary.dto.ClippingRequest;
import org.ngolibrary.service.ClippingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** कात्रणे master. Anyone signed in can read (the Catalog tab); admins manage. */
@RestController
@RequestMapping("/api/clippings")
public class ClippingController {

    private final ClippingService clippingService;

    public ClippingController(ClippingService clippingService) {
        this.clippingService = clippingService;
    }

    @GetMapping
    public List<Clipping> list() {
        return clippingService.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Clipping create(@RequestBody ClippingRequest request) {
        return clippingService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Clipping update(@PathVariable Long id, @RequestBody ClippingRequest request) {
        return clippingService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        clippingService.delete(id);
    }
}
