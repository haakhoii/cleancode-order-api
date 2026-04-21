package vn.r2s.training.api.service.impl;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.dto.SendNotificationRequest;
import vn.r2s.training.api.entity.NotificationHistory;
import vn.r2s.training.api.entity.NotificationStatus;
import vn.r2s.training.api.kafka.message.NotificationMessage;
import vn.r2s.training.api.kafka.producer.NotificationProducer;
import vn.r2s.training.api.repository.NotificationHistoryRepository;
import vn.r2s.training.api.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationProducer notificationProducer;
  private final NotificationHistoryRepository historyRepository;

  @Override
  public NotificationHistory sendOrderNotification(SendNotificationRequest request) {
    String notificationId = UUID.randomUUID().toString();

    NotificationHistory history = NotificationHistory.builder()
        .id(notificationId)
        .orderId(request.getOrderId())
        .to(request.getTo())
        .subject(request.getSubject())
        .content(request.getContent())
        .verificationCode(request.getVerificationCode())
        .status(NotificationStatus.PENDING)
        .createdAt(Instant.now())
        .build();
    historyRepository.save(history);

    NotificationMessage message = NotificationMessage.builder()
        .notificationId(notificationId)
        .orderId(request.getOrderId())
        .to(request.getTo())
        .subject(request.getSubject())
        .content(request.getContent())
        .verificationCode(request.getVerificationCode())
        .channel(request.getChannel())
        .build();
    notificationProducer.send(message);

    log.info("[NotificationService] Queued orderId=[{}] notificationId=[{}] email=[{}]",
        request.getOrderId(), notificationId, request.getTo());

    return history;
  }
}