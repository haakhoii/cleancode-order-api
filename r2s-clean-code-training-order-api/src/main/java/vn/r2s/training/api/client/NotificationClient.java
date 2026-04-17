package vn.r2s.training.api.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "notification-service",
    url = "${notification.service.url}"
)
public interface NotificationClient {

  @PostMapping("/api/notifications/send")
  ResponseEntity<Void> sendNotification(@RequestBody SendOrderNotificationRequest request);
}