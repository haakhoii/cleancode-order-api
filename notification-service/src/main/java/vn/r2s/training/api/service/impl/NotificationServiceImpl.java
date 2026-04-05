package vn.r2s.training.api.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.r2s.training.api.request.SendNotificationRequestDto;
import vn.r2s.training.api.service.NotificationService;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

  @Override
  public void sendNotification(SendNotificationRequestDto requestDto) {
    log.info("(sendNotification)Do send notification for request: [{}]",
        requestDto);
    try {
      Thread.sleep(10_000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    log.info("(sendNotification)Done send notification for request: [{}]",
        requestDto);
  }
}
