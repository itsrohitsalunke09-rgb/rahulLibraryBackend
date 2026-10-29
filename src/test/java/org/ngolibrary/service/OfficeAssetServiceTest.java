package org.ngolibrary.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.ngolibrary.domain.OfficeAsset;
import org.ngolibrary.service.OfficeAssetService.AssetRequest;
import org.springframework.web.server.ResponseStatusException;

class OfficeAssetServiceTest {

    private static AssetRequest named(String name) {
        return new AssetRequest(name, null, " ", null, null, null, null);
    }

    @Test
    void fillsSensibleDefaults() {
        OfficeAsset asset = new OfficeAsset();
        OfficeAssetService.apply(asset, named("  Steel almirah "));
        assertEquals("Steel almirah", asset.getName());
        assertEquals(1, asset.getQuantity());
        assertNull(asset.getLocation());
        assertEquals(OfficeAsset.Condition.GOOD, asset.getCondition());
    }

    @Test
    void refusesWhatCantBeRight() {
        OfficeAsset asset = new OfficeAsset();
        assertThrows(ResponseStatusException.class, () -> OfficeAssetService.apply(asset, named(" ")));
        assertThrows(ResponseStatusException.class, () -> OfficeAssetService.apply(asset,
                new AssetRequest("Fan", 0, null, null, null, null, null)));
        assertThrows(ResponseStatusException.class, () -> OfficeAssetService.apply(asset,
                new AssetRequest("Fan", 2, null, LocalDate.now().plusDays(1), null, null, null)));
        assertThrows(ResponseStatusException.class, () -> OfficeAssetService.apply(asset,
                new AssetRequest("Fan", 2, null, null, BigDecimal.valueOf(-5), null, null)));
    }
}
