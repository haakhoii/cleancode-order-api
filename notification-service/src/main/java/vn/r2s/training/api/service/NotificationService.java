package vn.r2s.training.api.service;


import vn.r2s.training.api.dto.SendNotificationRequest;
import vn.r2s.training.api.request.SendNotificationRequestDto;

public interface NotificationService {
  void sendOrderNotification(SendNotificationRequest request);
}
