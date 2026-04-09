package vn.r2s.training.api.client.config;

import feign.Logger.Level;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NotificationClientConfig {

  @Bean
  Level notifcationClientLoggerLevel() {
    return Level.FULL;
  }
}
