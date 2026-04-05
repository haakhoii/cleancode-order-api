package vn.r2s.training.api.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import vn.r2s.training.api.model.NotificationChannel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SendNotificationRequestDto {
  private String content;
  private String to;
  private NotificationChannel channel;
}
