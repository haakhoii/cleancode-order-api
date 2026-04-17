package vn.r2s.training.api.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.kafka.event.OrderCreatedEvent;
import vn.r2s.training.api.model.NotificationChannel;
import vn.r2s.training.api.request.SendNotificationRequestDto;
import vn.r2s.training.api.service.NotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

  private final NotificationService notificationService;

  @KafkaListener(topics = "order-created", groupId = "notification-group")
  public void consume(OrderCreatedEvent event) {

    SendNotificationRequestDto request = new SendNotificationRequestDto(
        event.getContent(),
        event.getTo(),
        NotificationChannel.EMAIL
    );

    notificationService.sendNotification(request);
  }

}