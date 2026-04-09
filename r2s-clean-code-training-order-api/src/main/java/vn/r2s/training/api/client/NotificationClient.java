package vn.r2s.training.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.r2s.training.api.client.config.NotificationClientConfig;
import vn.r2s.training.api.client.payload.SendNotificationRequest;

@FeignClient(
    name = "notification-service",
    url = "http://localhost:8081",
    configuration = NotificationClientConfig.class
)
public interface NotificationClient {

  @PostMapping(value = "/internal/notifications")
  ResponseEntity<String> sendNotification(
      @RequestBody SendNotificationRequest request
  );
}
