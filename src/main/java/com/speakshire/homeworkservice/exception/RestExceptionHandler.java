package com.speakshire.homeworkservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<?> nf(NotFoundException e){
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err("NOT_FOUND", e.getMessage()));
  }

  @ExceptionHandler(ForbiddenException.class)
  ResponseEntity<?> fb(BadRequestException e){
    return ResponseEntity.badRequest().body(err("FORBIDDEN", e.getMessage()));
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<?> br(BadRequestException e){
    return ResponseEntity.badRequest().body(err("BAD_REQUEST", e.getMessage()));
  }

  private Map<String,Object> err(String code, String msg){
    return Map.of("timestamp", Instant.now().toString(), "code", code, "message", msg);
  }
}
