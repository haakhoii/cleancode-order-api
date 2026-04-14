package vn.r2s.training.api.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.request.SendNotificationRequestDto;
import vn.r2s.training.api.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

  private final NotificationService notificationService;

  @KafkaListener(topics = "send-notification", groupId = "notification-group")
  public void consume(SendNotificationRequestDto request) {
    log.info("Received message from Kafka: {}", request);
    notificationService.sendNotification(request);
  }
}