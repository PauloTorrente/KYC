package com.donjuan.kyc.config;

import com.donjuan.kyc.service.ClienteService;
import com.donjuan.kyc.service.KycMasterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    private Map<String, Object> body(HttpStatus status, String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("timestamp", Instant.now().toString());
        m.put("status", status.value());
        m.put("error", msg);
        return m;
    }

    @ExceptionHandler(ClienteService.ValidationException.class)
    public ResponseEntity<?> validation(ClienteService.ValidationException e) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(ClienteService.DuplicateException.class)
    public ResponseEntity<?> duplicate(ClienteService.DuplicateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body(HttpStatus.CONFLICT, e.getMessage()));
    }

    @ExceptionHandler(KycMasterService.NotFoundException.class)
    public ResponseEntity<?> kycNotFound(KycMasterService.NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> beanValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("dados invalidos");
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, msg));
    }
}
