package vn.r2s.training.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.r2s.training.api.dto.SendNotificationRequest;
import vn.r2s.training.api.entity.NotificationHistory;
import vn.r2s.training.api.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationService notificationService;

  @PostMapping("/send")
  public ResponseEntity<NotificationHistory> send(
      @Valid @RequestBody SendNotificationRequest request
  ) {
    NotificationHistory result = notificationService.sendOrderNotification(request);
    return ResponseEntity.ok(result);
  }
}