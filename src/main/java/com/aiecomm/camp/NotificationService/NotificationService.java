package com.aiecomm.camp.NotificationService;

public interface NotificationService<T> {


   public NotificationResponse sendMail(NotifcationRequest notifcationRequest);

   public void sendSms();

}
