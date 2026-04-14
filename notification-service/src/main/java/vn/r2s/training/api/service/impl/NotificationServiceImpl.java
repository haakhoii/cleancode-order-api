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
    log.info("(sendNotification) Start send notification: {}", requestDto);

    if (NotificationChannel.EMAIL.equals(requestDto.getChannel())) {
      sendEmail(requestDto);
    }

    log.info("(sendNotification) Done send notification: {}", requestDto);
  }

  private void sendEmail(SendNotificationRequestDto request) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(request.getTo());
    message.setSubject("Notification from system");
    message.setText(request.getContent());

    mailSender.send(message);
  }
}