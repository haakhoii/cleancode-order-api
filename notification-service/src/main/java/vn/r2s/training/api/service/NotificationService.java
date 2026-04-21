package vn.r2s.training.api.service;


import vn.r2s.training.api.dto.SendNotificationRequest;
import vn.r2s.training.api.entity.NotificationHistory;

public interface NotificationService {
  NotificationHistory sendOrderNotification(SendNotificationRequest request);
}
