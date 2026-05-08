package vn.r2s.training.api.kafka.consumer;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.entity.NotificationHistory;
import vn.r2s.training.api.entity.NotificationStatus;
import vn.r2s.training.api.kafka.message.NotificationMessage;
import vn.r2s.training.api.repository.NotificationHistoryRepository;
import vn.r2s.training.api.service.NotificationSender;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

  private final List<NotificationSender> senders;
  private final NotificationHistoryRepository historyRepository;

  @KafkaListener(
      topics = "${app.kafka.topic.order-notification}",
      groupId = "${spring.kafka.consumer.group-id}"
  )
  public void consume(NotificationMessage message) {
    String notificationId = message.getNotificationId();

    NotificationHistory history = historyRepository.findById(notificationId)
        .orElseGet(() -> {
          log.warn("[NotificationConsumer] No PENDING record for notificationId=[{}], creating fallback",
              notificationId);
          return NotificationHistory.builder()
              .id(notificationId)
              .orderId(message.getOrderId())
              .to(message.getTo())
              .subject(message.getSubject())
              .content(message.getContent())
              .verificationCode(message.getVerificationCode())
              .build();
        });

    try {
      NotificationSender sender = senders.stream()
          .filter(s -> s.getType() == message.getChannel())
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException(
              "No sender found for channel: " + message.getChannel()));

      sender.send(message);
      history.setStatus(NotificationStatus.SUCCESS);
      log.info("[NotificationConsumer] Sent notificationId=[{}] orderId=[{}] channel=[{}]",
          notificationId, message.getOrderId(), message.getChannel());
    } catch (Exception e) {
      history.setStatus(NotificationStatus.FAILED);
      log.error("[NotificationConsumer] Failed notificationId=[{}] orderId=[{}]: {}",
          notificationId, message.getOrderId(), e.getMessage(), e);
    }

    historyRepository.save(history);
  }
}