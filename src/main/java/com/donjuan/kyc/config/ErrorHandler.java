package com.donjuan.kyc.config;

import com.donjuan.kyc.service.AnthropicVerificationService;
import com.donjuan.kyc.service.ClienteService;
import com.donjuan.kyc.service.KycDocumentoExtracaoService;
import com.donjuan.kyc.service.KycDocumentoService;
import com.donjuan.kyc.service.KycMasterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

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

    @ExceptionHandler(KycDocumentoService.ValidationException.class)
    public ResponseEntity<?> documentoValidation(KycDocumentoService.ValidationException e) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    @ExceptionHandler(KycDocumentoExtracaoService.ExtracaoException.class)
    public ResponseEntity<?> extracaoFalhou(KycDocumentoExtracaoService.ExtracaoException e) {
        log.warn("falha na extracao por OCR: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body(HttpStatus.BAD_GATEWAY, e.getMessage()));
    }

    @ExceptionHandler(AnthropicVerificationService.VerificacaoIaException.class)
    public ResponseEntity<?> verificacaoIaFalhou(AnthropicVerificationService.VerificacaoIaException e) {
        log.warn("falha na verificacao por IA: {}", e.getMessage(), e.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body(HttpStatus.BAD_GATEWAY, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> beanValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("dados invalidos");
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, msg));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "parametro '" + e.getName() + "' invalido"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> arquivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(body(HttpStatus.PAYLOAD_TOO_LARGE, "arquivo excede o tamanho maximo permitido"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, "corpo da requisicao invalido ou malformado"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> metodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(body(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<?> parametroFaltando(Exception e) {
        return ResponseEntity.badRequest().body(body(HttpStatus.BAD_REQUEST, e.getMessage()));
    }

    /** Ultimo recurso: nunca devolve mensagem/stacktrace interna ao cliente, so loga no servidor. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> erroInesperado(Exception e) {
        log.error("erro nao tratado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "erro interno inesperado"));
    }
}
