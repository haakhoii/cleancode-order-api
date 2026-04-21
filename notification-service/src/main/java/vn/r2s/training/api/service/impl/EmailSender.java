package vn.r2s.training.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import vn.r2s.training.api.kafka.message.NotificationMessage;
import vn.r2s.training.api.model.NotificationChannel;
import vn.r2s.training.api.service.NotificationSender;

@Component
@RequiredArgsConstructor
public class EmailSender implements NotificationSender {

  private final JavaMailSender mailSender;

  @Override
  public NotificationChannel getType() {
    return NotificationChannel.EMAIL;
  }

  @Override
  public void send(NotificationMessage message) {
    SimpleMailMessage mail = new SimpleMailMessage();
    mail.setTo(message.getTo());
    mail.setSubject(message.getSubject());
    mail.setText(message.getContent());

    mailSender.send(mail);
  }
}
