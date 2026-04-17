package vn.r2s.training.api.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.r2s.training.api.dto.SendNotificationRequest;
import vn.r2s.training.api.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @PostMapping("/send")
  public ResponseEntity<Void> send(@Valid @RequestBody SendNotificationRequest request) {
    notificationService.sendOrderNotification(request);
    return ResponseEntity.ok().build();
  }
}