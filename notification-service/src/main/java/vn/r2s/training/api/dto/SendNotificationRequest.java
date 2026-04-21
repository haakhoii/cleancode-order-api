package vn.r2s.training.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import vn.r2s.training.api.model.NotificationChannel;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SendNotificationRequest {
  @NotBlank
  private String orderId;

  @Email
  @NotBlank
  private String to;

  @NotBlank
  private String subject;

  @NotBlank
  private String content;

  @NotBlank
  private String verificationCode;

  @NotNull
  private NotificationChannel channel;
}
