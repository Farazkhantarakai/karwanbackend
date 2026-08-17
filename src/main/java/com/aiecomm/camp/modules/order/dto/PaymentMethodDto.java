package com.aiecomm.camp.modules.order.dto;

import com.aiecomm.camp.modules.order.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodDto {

    private Long paymentMethodId;
    private String methodName;

    public static PaymentMethodDto fromEntity(PaymentMethod entity) {
        if (entity == null) return null;
        return PaymentMethodDto.builder()
                .paymentMethodId(entity.getPaymentMethodId())
                .methodName(entity.getMethodName())
                .build();
    }
}
