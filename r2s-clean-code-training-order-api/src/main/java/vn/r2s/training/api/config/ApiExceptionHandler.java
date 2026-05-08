package vn.r2s.training.api.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import feign.FeignException;
import vn.r2s.training.api.dto.response.ErrorResponse;
import vn.r2s.training.api.exception.BadRequestException;
import vn.r2s.training.api.exception.NotFoundException;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  // 500
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
    log.error("[{}] INTERNAL_ERROR {}", traceId(), req.getRequestURI(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        ErrorResponse.builder()
            .errorCode("INTERNAL_ERROR")
            .message("An unexpected error occurred. Please try again later.")
            .traceId(traceId())
            .build());
  }

  // not found
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
      NotFoundException ex, HttpServletRequest req) {
    log.warn("[{}] NOT_FOUND {}: {}", traceId(), req.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ErrorResponse.builder()
            .errorCode("NOT_FOUND")
            .message(ex.getMessage())
            .traceId(traceId())
            .build());
  }

  // bad request
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(
      BadRequestException ex, HttpServletRequest req) {
    log.warn("[{}] BAD_REQUEST {}: {}", traceId(), req.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        ErrorResponse.builder()
            .errorCode("BAD_REQUEST")
            .message(ex.getMessage())
            .traceId(traceId())
            .build());
  }

  // validation
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<ErrorResponse.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> ErrorResponse.FieldError.builder()
            .field(fe.getField())
            .reason(fe.getDefaultMessage())
            .rejectedValue(fe.getRejectedValue())
            .build())
        .toList();
    log.warn("[{}] VALIDATION_FAILED fields={}", traceId(),
        details.stream().map(ErrorResponse.FieldError::getField).toList());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        ErrorResponse.builder()
            .errorCode("VALIDATION_FAILED")
            .message("Request validation failed")
            .details(details)
            .traceId(traceId())
            .build());
  }
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleJsonParse(
      HttpMessageNotReadableException ex, HttpServletRequest req) {
    log.warn("[{}] INVALID_JSON {}: {}", traceId(), req.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
        ErrorResponse.builder()
            .errorCode("INVALID_JSON")
            .message("Request body is malformed or missing")
            .traceId(traceId())
            .build());
  }
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDbConstraint(
      DataIntegrityViolationException ex, HttpServletRequest req) {
    log.warn("[{}] DB_CONSTRAINT {}: {}", traceId(), req.getRequestURI(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(
        ErrorResponse.builder()
            .errorCode("DATA_CONFLICT")
            .message("Data integrity violation. The resource may already exist.")
            .traceId(traceId())
            .build());
  }

  // 4xx từ downstream (notification-service trả lỗi client)
  @ExceptionHandler(FeignException.FeignClientException.class)
  public ResponseEntity<ErrorResponse> handleFeignClient(
      FeignException.FeignClientException ex, HttpServletRequest req) {
    log.warn("[{}] FEIGN_CLIENT_ERROR {} status={}: {}",
        traceId(), req.getRequestURI(), ex.status(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
        ErrorResponse.builder()
            .errorCode("DOWNSTREAM_CLIENT_ERROR")
            .message("Downstream service rejected the request")
            .traceId(traceId())
            .build());
  }

  // 5xx từ downstream (notification-service lỗi server)
  @ExceptionHandler(FeignException.FeignServerException.class)
  public ResponseEntity<ErrorResponse> handleFeignServer(
      FeignException.FeignServerException ex, HttpServletRequest req) {
    log.error("[{}] FEIGN_SERVER_ERROR {} status={}: {}",
        traceId(), req.getRequestURI(), ex.status(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
        ErrorResponse.builder()
            .errorCode("DOWNSTREAM_SERVER_ERROR")
            .message("Downstream service is temporarily unavailable")
            .traceId(traceId())
            .build());
  }

  private static String traceId() {
    String id = MDC.get("traceId");
    return id != null ? id : "N/A";
  }
}