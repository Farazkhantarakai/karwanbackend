package com.aiecomm.camp.modules.order.dto;

import com.aiecomm.camp.modules.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productname;
    private Double unitprice;
    private Integer quantity;
    private Double total;

    public static OrderItemDto fromEntity(OrderItem entity) {
        if (entity == null) return null;
        return OrderItemDto.builder()
                .id(entity.getId())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .productId(entity.getProduct() != null ? entity.getProduct().getProductId() : null)
                .productname(entity.getProductname())
                .unitprice(entity.getUnitprice())
                .quantity(entity.getQuantity())
                .total(entity.getTotal())
                .build();
    }
}
