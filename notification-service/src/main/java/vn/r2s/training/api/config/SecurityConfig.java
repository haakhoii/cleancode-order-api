package vn.r2s.training.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Value("${springdoc.swagger-ui.enabled:false}")
  private boolean swaggerEnabled;

  @Value("${app.security.permit-all:false}")
  private boolean permitAll;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> {
          if (permitAll) {
            auth.anyRequest().permitAll();
            return;
          }
          if (swaggerEnabled) {
            auth.requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).permitAll();
          }
          auth
              .requestMatchers("/actuator/health/**").permitAll()
              .anyRequest().authenticated();
        });
    return http.build();
  }
}