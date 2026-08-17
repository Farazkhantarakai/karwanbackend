package com.aiecomm.camp.modules.subscription.dto;

import com.aiecomm.camp.modules.subscription.entity.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlanDto {

    private Long subscriptionId;
    private String subscriptionName;
    private Double price;
    private Instant startingDate;
    private Instant endingDate;

    public static SubscriptionPlanDto fromEntity(SubscriptionPlan entity) {
        if (entity == null) return null;
        return SubscriptionPlanDto.builder()
                .subscriptionId(entity.getSubscriptionId())
                .subscriptionName(entity.getSubscriptionName())
                .price(entity.getPrice())
                .startingDate(entity.getStartingDate())
                .endingDate(entity.getEndingDate())
                .build();
    }
}
