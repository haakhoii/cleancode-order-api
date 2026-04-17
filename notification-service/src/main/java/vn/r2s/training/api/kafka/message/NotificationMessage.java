package vn.r2s.training.api.kafka.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class NotificationMessage {
  private String orderId;
  private String customerEmail;
  private String subject;
  private String content;
  private String verificationCode;
}