package vn.r2s.training.api.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.kafka.message.NotificationMessage;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

  private final JavaMailSender mailSender;

  @KafkaListener(topics = "order.notification", groupId = "notification-group")
  public void consume(NotificationMessage message) {
    try {
      SimpleMailMessage mail = new SimpleMailMessage();
      mail.setTo(message.getCustomerEmail());
      mail.setSubject(message.getSubject());
      mail.setText(message.getContent());
      mailSender.send(mail);
      log.info("[KafkaConsumer] Email sent to={} orderId={}",
          message.getCustomerEmail(), message.getOrderId());
    } catch (Exception e) {
      log.error("[KafkaConsumer] Failed to send email orderId={}: {}",
          message.getOrderId(), e.getMessage());
    }
  }
}
