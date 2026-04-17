package vn.r2s.training.api.service;


import vn.r2s.training.api.dto.SendNotificationRequest;

public interface NotificationService {
  void sendOrderNotification(SendNotificationRequest request);
}
