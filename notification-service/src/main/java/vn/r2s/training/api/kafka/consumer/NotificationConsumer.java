package vn.r2s.training.api.kafka.consumer;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.entity.NotificationHistory;
import vn.r2s.training.api.entity.NotificationStatus;
import vn.r2s.training.api.kafka.message.NotificationMessage;
import vn.r2s.training.api.repository.NotificationHistoryRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

  private final JavaMailSender mailSender;
  private final NotificationHistoryRepository historyRepository;

  @KafkaListener(topics = "order.notification", groupId = "notification-group")
  public void consume(NotificationMessage message) {

    NotificationHistory history = NotificationHistory.builder()
        .orderId(message.getOrderId())
        .customerEmail(message.getCustomerEmail())
        .subject(message.getSubject())
        .content(message.getContent())
        .verificationCode(message.getVerificationCode())
        .createdAt(Instant.now())
        .build();

    try {
      SimpleMailMessage mail = new SimpleMailMessage();
      mail.setTo(message.getCustomerEmail());
      mail.setSubject(message.getSubject());
      mail.setText(message.getContent());

      mailSender.send(mail);

      history.setStatus(NotificationStatus.SUCCESS);

      log.info("[KafkaConsumer] Email sent to={} orderId={}",
          message.getCustomerEmail(), message.getOrderId());

    } catch (Exception e) {

      history.setStatus(NotificationStatus.FAILED);
      history.setErrorMessage(e.getMessage());

      log.error("[KafkaConsumer] Failed to send email orderId={}: {}",
          message.getOrderId(), e.getMessage());
    }

    historyRepository.save(history);
  }
}