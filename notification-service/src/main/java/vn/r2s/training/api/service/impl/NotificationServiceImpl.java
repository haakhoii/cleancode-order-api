package vn.r2s.training.api.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.model.NotificationChannel;
import vn.r2s.training.api.request.SendNotificationRequestDto;
import vn.r2s.training.api.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

  private final JavaMailSender mailSender;

  @Override
  public void sendNotification(SendNotificationRequestDto requestDto) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(requestDto.getTo());
    message.setSubject("Order Notification");
    message.setText(requestDto.getContent());

    mailSender.send(message);
    log.info("Send email success");
  }
}