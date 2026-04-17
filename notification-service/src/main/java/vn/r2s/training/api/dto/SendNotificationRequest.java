package vn.r2s.training.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SendNotificationRequest {
  @NotBlank private String orderId;
  @Email @NotBlank private String customerEmail;
  @NotBlank private String subject;
  @NotBlank private String content;
  @NotBlank private String verificationCode;
}
