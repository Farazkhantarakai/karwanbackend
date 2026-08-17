package com.aiecomm.camp.modules.order.dto;

import com.aiecomm.camp.modules.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    private Long id;
    private UUID tenantId;
    private Long storeId;
    private String customerName;
    private String status;
    private Double subtotal;
    private Double discount;
    private Double shipingcost;
    private Double total;
    private String confirmationStatus;
    private String source;
    private Long paymentMethodId;
    private String paymentMethodName;
    private String shipingAddress;
    private Long deliveryModeId;
    private String deliveryMethodName;
    private Instant createdAt;
    private List<OrderItemDto> orderItems;
    private List<CustomerDto> customers;

    public static OrderDto fromEntity(Order entity) {
        if (entity == null) return null;
        return OrderDto.builder()
                .id(entity.getId())
                .tenantId(entity.getTenant() != null ? entity.getTenant().getTenantId() : null)
                .storeId(entity.getStore() != null ? entity.getStore().getStoreId() : null)
                .customerName(entity.getCustomerName())
                .status(entity.getStatus())
                .subtotal(entity.getSubtotal())
                .discount(entity.getDiscount())
                .shipingcost(entity.getShipingcost())
                .total(entity.getTotal())
                .confirmationStatus(entity.getConfirmationStatus())
                .source(entity.getSource())
                .paymentMethodId(entity.getPaymentMethod() != null ? entity.getPaymentMethod().getPaymentMethodId() : null)
                .paymentMethodName(entity.getPaymentMethod() != null ? entity.getPaymentMethod().getMethodName() : null)
                .shipingAddress(entity.getShipingAddress())
                .deliveryModeId(entity.getDeliveryMode() != null ? entity.getDeliveryMode().getDeliveryId() : null)
                .deliveryMethodName(entity.getDeliveryMode() != null ? entity.getDeliveryMode().getMethodName() : null)
                .createdAt(entity.getCreatedAt())
                .orderItems(entity.getOrderItems() != null ? entity.getOrderItems().stream().map(OrderItemDto::fromEntity).collect(Collectors.toList()) : null)
                .customers(entity.getCustomers() != null ? entity.getCustomers().stream().map(CustomerDto::fromEntity).collect(Collectors.toList()) : null)
                .build();
    }
}
