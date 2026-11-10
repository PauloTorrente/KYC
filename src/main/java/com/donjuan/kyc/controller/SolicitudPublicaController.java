package com.donjuan.kyc.controller;

import com.donjuan.kyc.dto.kyc.KycClienteDetail;
import com.donjuan.kyc.dto.kyc.KycClienteRequest;
import com.donjuan.kyc.dto.kyc.SolicitudPublicaRequest;
import com.donjuan.kyc.service.KycMasterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pre-cadastro vindo do site publico (sem login). Cria o cliente como
 * plataforma SITE e estado PENDENTE, para o operador revisar no painel KYC
 * (meu-projeto). Nao devolve dados do cliente, so o codigo de acompanhamento.
 */
@RestController
@RequestMapping("/api/public/solicitudes")
public class SolicitudPublicaController {

    private static final int MAX_POR_HORA = 5;
    private static final long JANELA_MS = 60 * 60 * 1000L;

    private final KycMasterService service;
    private final Map<String, Deque<Long>> tentativasPorIp = new ConcurrentHashMap<>();

    public SolicitudPublicaController(KycMasterService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody SolicitudPublicaRequest req, HttpServletRequest http) {
        // Atras de proxy (Render etc.) o IP real vem no X-Forwarded-For
        String fwd = http.getHeader("X-Forwarded-For");
        String ip = fwd != null && !fwd.isBlank() ? fwd.split(",")[0].trim() : http.getRemoteAddr();
        if (!permitido(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "timestamp", Instant.now().toString(),
                    "status", 429,
                    "error", "Demasiadas solicitudes. Intenta nuevamente en una hora."));
        }

        KycClienteRequest k = new KycClienteRequest();
        k.setNome(req.getNombre().trim());
        k.setDocumento(req.getTipoDocumento());
        k.setNumero(req.getNumeroDocumento());
        k.setNacionalidade(req.getNacionalidad());
        k.setTelefone(req.getTelefono());
        k.setCidade(req.getCiudad());
        k.setPaisFiscal(req.getPais());
        k.setPlataforma("SITE");
        k.setEstadoKyc("PENDENTE");
        k.setDataCadastro(LocalDate.now());
        k.setExtra(extra(req));

        KycClienteDetail criado = service.cadastrar(k);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "codigo", criado.codigo,
                "estado", "PENDENTE"));
    }

    private static String extra(SolicitudPublicaRequest req) {
        StringBuilder sb = new StringBuilder("Solicitud web");
        if (req.getEmail() != null && !req.getEmail().isBlank()) sb.append(" | email: ").append(req.getEmail().trim());
        if (req.getOperacion() != null) sb.append(" | operacion: ").append(req.getOperacion());
        if (req.getMonto() != null && !req.getMonto().isBlank()) sb.append(" | monto: ").append(req.getMonto().trim());
        return sb.toString();
    }

    /** Limite simples em memoria: no maximo MAX_POR_HORA cadastros por IP por hora. */
    private boolean permitido(String ip) {
        long agora = System.currentTimeMillis();
        Deque<Long> fila = tentativasPorIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && agora - fila.peekFirst() > JANELA_MS) fila.pollFirst();
            if (fila.size() >= MAX_POR_HORA) return false;
            fila.addLast(agora);
            return true;
        }
    }
}
