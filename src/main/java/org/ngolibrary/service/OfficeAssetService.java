package org.ngolibrary.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.ngolibrary.domain.OfficeAsset;
import org.ngolibrary.repo.OfficeAssetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Office assets master: what the office owns, by name. */
@Service
public class OfficeAssetService {

    /** An asset as entered: name required; quantity 1 when empty; condition Good when empty. */
    public record AssetRequest(String name, Integer quantity, String location, LocalDate purchaseDate,
                               BigDecimal price, OfficeAsset.Condition condition, String notes) {
    }

    private final OfficeAssetRepository assets;

    public OfficeAssetService(OfficeAssetRepository assets) {
        this.assets = assets;
    }

    public List<OfficeAsset> list() {
        return assets.findAllByOrderByNameAsc();
    }

    public OfficeAsset create(AssetRequest request) {
        OfficeAsset asset = new OfficeAsset();
        apply(asset, request);
        return assets.save(asset);
    }

    public OfficeAsset update(Long id, AssetRequest request) {
        OfficeAsset asset = assets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));
        apply(asset, request);
        return assets.save(asset);
    }

    public void delete(Long id) {
        if (!assets.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
        }
        assets.deleteById(id);
    }

    static void apply(OfficeAsset asset, AssetRequest r) {
        String name = trimmed(r.name());
        if (name == null || name.length() > 150) {
            throw bad("Enter the asset's name (up to 150 characters).");
        }
        int quantity = r.quantity() == null ? 1 : r.quantity();
        if (quantity < 1 || quantity > 100000) {
            throw bad("Quantity must be 1 or more.");
        }
        String location = trimmed(r.location());
        String notes = trimmed(r.notes());
        if ((location != null && location.length() > 150) || (notes != null && notes.length() > 500)) {
            throw bad("Keep the place under 150 and the notes under 500 characters.");
        }
        if (r.purchaseDate() != null && r.purchaseDate().isAfter(LocalDate.now())) {
            throw bad("The purchase date can't be in the future.");
        }
        if (r.price() != null && (r.price().signum() < 0 || r.price().compareTo(BigDecimal.valueOf(100_000_000)) > 0)) {
            throw bad("Enter a price of 0 or more.");
        }
        asset.setName(name);
        asset.setQuantity(quantity);
        asset.setLocation(location);
        asset.setPurchaseDate(r.purchaseDate());
        asset.setPrice(r.price());
        asset.setCondition(r.condition() == null ? OfficeAsset.Condition.GOOD : r.condition());
        asset.setNotes(notes);
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
