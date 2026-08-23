package com.aiecomm.camp.NotificationService;

import com.aiecomm.camp.NotificationService.dto.BrevoResponse;
import com.aiecomm.camp.NotificationService.dto.BrevoTemplate;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;



@AllArgsConstructor
@RequiredArgsConstructor
@Component
public class BrevoNotificationImpl implements NotificationService{


    @Value("${mail.key}")
    String mailApiKey;

    @Value("${brevo-url}")
    String brevoUrl;

    private Logger logger= LoggerFactory.getLogger(BrevoNotificationImpl.class);


    @Override
    public NotificationResponse sendMail(NotifcationRequest notifcationRequest) {

      BrevoTemplate template=  (BrevoTemplate) notifcationRequest;
      logger.info(" json for the brevo "+template.toString());

        logger.info("Sending email via Brevo API, url: " + brevoUrl);
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            httpHeaders.set("accept", "application/json");
            httpHeaders.set("api-key", mailApiKey);

            HttpEntity<NotifcationRequest> httpEntity = new HttpEntity<>(notifcationRequest, httpHeaders);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<BrevoResponse> response = restTemplate.exchange(
                    brevoUrl,
                    HttpMethod.POST,
                    httpEntity,
                    BrevoResponse.class
            );

            if (response != null && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            logger.error("sendMail exception: " + e.getMessage(), e);
        }

        return null;
    }

    @Override
    public void sendSms() {

    }
}
