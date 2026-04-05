package vn.r2s.training.api.config;

import java.util.Map;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.r2s.training.api.exception.NotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<?> handleNotFound(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(Map.of("message", ex.getMessage(), "code", "NOT_FOUND"));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("message", ex.getMessage(), "code", "BAD_REQUEST"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleUnknown(Exception ex) {
    // intentionally simple + not great: fresher can improve structure
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of("message", "Unknown error: " + ex.getMessage(), "code", "INTERNAL_ERROR"));
  }
}