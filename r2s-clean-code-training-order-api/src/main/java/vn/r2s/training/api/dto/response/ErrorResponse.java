package vn.r2s.training.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

  private String errorCode;
  private String message;
  private List<FieldError> details;
  private String traceId;

  @Builder.Default
  private Instant timestamp = Instant.now();

  @Getter
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class FieldError {
    private String field;
    private String reason;
    private Object rejectedValue;
  }
}