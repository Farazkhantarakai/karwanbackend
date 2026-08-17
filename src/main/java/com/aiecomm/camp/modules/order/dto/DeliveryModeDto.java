package com.aiecomm.camp.modules.order.dto;

import com.aiecomm.camp.modules.order.entity.DeliveryMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryModeDto {

    private Long deliveryId;
    private String methodName;

    public static DeliveryModeDto fromEntity(DeliveryMode entity) {
        if (entity == null) return null;
        return DeliveryModeDto.builder()
                .deliveryId(entity.getDeliveryId())
                .methodName(entity.getMethodName())
                .build();
    }
}
