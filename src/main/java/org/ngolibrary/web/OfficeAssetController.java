package org.ngolibrary.web;

import java.util.List;

import org.ngolibrary.domain.OfficeAsset;
import org.ngolibrary.service.OfficeAssetService;
import org.ngolibrary.service.OfficeAssetService.AssetRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Office assets master; like the other masters, admins only. */
@RestController
@RequestMapping("/api/assets")
@PreAuthorize("hasRole('ADMIN')")
public class OfficeAssetController {

    private final OfficeAssetService service;

    public OfficeAssetController(OfficeAssetService service) {
        this.service = service;
    }

    @GetMapping
    public List<OfficeAsset> list() {
        return service.list();
    }

    @PostMapping
    public OfficeAsset create(@RequestBody AssetRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OfficeAsset update(@PathVariable Long id, @RequestBody AssetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
