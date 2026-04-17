package vn.r2s.training.api.client;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SendOrderNotificationRequest {
  private String orderId;
  private String customerEmail;
  private String subject;
  private String content;
  private String verificationCode;
}
