package vn.r2s.training.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.r2s.training.api.kafka.NotificationProducer;
import vn.r2s.training.api.request.SendNotificationRequestDto;

@RestController
@RequestMapping(value = "/internal/notifications")
@RequiredArgsConstructor
public class NotificationController {
  private final NotificationProducer notificationProducer;

  @PostMapping
  public ResponseEntity<?> sendNotification(@RequestBody SendNotificationRequestDto requestDto) {
    notificationProducer.send(requestDto);
    return ResponseEntity.ok("Send notification to Kafka successfully!");
  }
}
