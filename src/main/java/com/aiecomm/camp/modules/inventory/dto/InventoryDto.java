package com.aiecomm.camp.modules.inventory.dto;

import com.aiecomm.camp.modules.inventory.entity.Inventory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDto {

    private Long inventoryId;
    private Long productId;
    private Long locationId;
    private Integer quantity;
    private Instant createdOn;
    private Instant updatedOn;

    public static InventoryDto fromEntity(Inventory entity) {
        if (entity == null) return null;
        return InventoryDto.builder()
                .inventoryId(entity.getInventoryId())
                .productId(entity.getProduct() != null ? entity.getProduct().getProductId() : null)
                .locationId(entity.getLocation() != null ? entity.getLocation().getLocationId() : null)
                .quantity(entity.getQuantity())
                .createdOn(entity.getCreatedOn())
                .updatedOn(entity.getUpdatedOn())
                .build();
    }
}
