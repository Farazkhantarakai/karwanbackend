package com.aiecomm.camp.NotificationService.dto;

import com.aiecomm.camp.NotificationService.NotifcationRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class BrevoTemplate implements NotifcationRequest {

    private  Sender sender;
    private List<Reciever> to;
    private String subject;
    private String textContent;


}
