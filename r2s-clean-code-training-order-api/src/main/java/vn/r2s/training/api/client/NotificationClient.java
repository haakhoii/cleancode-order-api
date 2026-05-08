package vn.r2s.training.api.client;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
  @CircuitBreaker(name = "notificationService")
  @Retry(name = "notificationService")
  ResponseEntity<Void> sendNotification(@RequestBody SendOrderNotificationRequest request);
}