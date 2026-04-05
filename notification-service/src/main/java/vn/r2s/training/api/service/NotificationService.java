package vn.r2s.training.api.service;


import vn.r2s.training.api.request.SendNotificationRequestDto;

public interface NotificationService {
  void sendNotification(SendNotificationRequestDto requestDto);
}
