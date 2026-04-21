package vn.r2s.training.api.entity;


import lombok.*;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notification_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory {

  @Id
  private String id;

  private String orderId;
  private String to;
  private String subject;
  private String content;
  private String verificationCode;

  private NotificationStatus  status;
  private Instant createdAt;
}