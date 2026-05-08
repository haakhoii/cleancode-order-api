package vn.r2s.training.api.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.kafka.message.NotificationMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationProducer {

  private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;
  @Value("${app.kafka.topic.order-notification}")
  private String topic;

  public void send(NotificationMessage message) {
    kafkaTemplate.send(topic, message.getOrderId(), message)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("[NotificationProducer] Failed to publish orderId=[{}]: {}",
                message.getOrderId(), ex.getMessage());
          } else {
            log.info("[NotificationProducer] Published topic=[{}] orderId=[{}] notificationId=[{}]",
                topic, message.getOrderId(), message.getNotificationId());
          }
        });
  }
}