package vn.r2s.training.api.kafka.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.r2s.training.api.model.NotificationChannel;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class NotificationMessage {
  private String notificationId;
  private String orderId;
  private String to;
  private String subject;
  private String content;
  private String verificationCode;

  private NotificationChannel channel;
}