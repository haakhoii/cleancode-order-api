package vn.r2s.training.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.dto.SendNotificationRequest;
import vn.r2s.training.api.kafka.message.NotificationMessage;
import vn.r2s.training.api.kafka.producer.NotificationProducer;
import vn.r2s.training.api.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final NotificationProducer notificationProducer;

  @Override
  public void sendOrderNotification(SendNotificationRequest request) {
    NotificationMessage message = NotificationMessage.builder()
        .orderId(request.getOrderId())
        .customerEmail(request.getCustomerEmail())
        .subject(request.getSubject())
        .content(request.getContent())
        .verificationCode(request.getVerificationCode())
        .build();

    notificationProducer.send(message);
    log.info("[NotificationService] Queued order notification for orderId=[{}] email=[{}]",
        request.getOrderId(), request.getCustomerEmail());
  }
}