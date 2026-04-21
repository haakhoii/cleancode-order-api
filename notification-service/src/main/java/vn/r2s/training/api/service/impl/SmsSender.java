package vn.r2s.training.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.kafka.message.NotificationMessage;
import vn.r2s.training.api.model.NotificationChannel;
import vn.r2s.training.api.service.NotificationSender;

@Component
@Slf4j
public class SmsSender implements NotificationSender {

  @Override
  public NotificationChannel getType() {
    return NotificationChannel.SMS;
  }

  @Override
  public void send(NotificationMessage message) {
    log.info("Send SMS: ");
    log.info("To: {} ",  message.getTo());
    log.info("Content: {} ",  message.getContent());
    log.info("Subject: {} ",  message.getSubject());
    log.info("OrderId: {} ",  message.getOrderId());
  }
}