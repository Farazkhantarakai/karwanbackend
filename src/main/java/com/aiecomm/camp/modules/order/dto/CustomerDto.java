package com.aiecomm.camp.modules.order.dto;

import com.aiecomm.camp.modules.order.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDto {

    private Long customerId;
    private Long orderId;
    private String shipmentstatus;
    private String details;

    public static CustomerDto fromEntity(Customer entity) {
        if (entity == null) return null;
        return CustomerDto.builder()
                .customerId(entity.getCustomerId())
                .orderId(entity.getOrder() != null ? entity.getOrder().getId() : null)
                .shipmentstatus(entity.getShipmentstatus())
                .details(entity.getDetails())
                .build();
    }
}
