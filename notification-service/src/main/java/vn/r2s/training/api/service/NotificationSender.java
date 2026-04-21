package vn.r2s.training.api.service;

import vn.r2s.training.api.kafka.message.NotificationMessage;
import vn.r2s.training.api.model.NotificationChannel;

public interface NotificationSender {
  NotificationChannel getType();
  void send(NotificationMessage message);
}
