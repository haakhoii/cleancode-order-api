package vn.r2s.training.api.client;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import vn.r2s.training.api.enums.NotificationChannel;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SendOrderNotificationRequest {
  private String orderId;
  private String to;
  private String subject;
  private String content;
  private String verificationCode;
  @NotNull
  private NotificationChannel channel;
}
