package com.donjuan.kyc.controller;

import com.donjuan.kyc.dto.kyc.KycClienteDetail;
import com.donjuan.kyc.dto.kyc.KycClienteListItem;
import com.donjuan.kyc.dto.kyc.KycClienteRequest;
import com.donjuan.kyc.dto.kyc.KycStatusUpdateRequest;
import com.donjuan.kyc.service.KycMasterService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kyc/clientes")
public class KycClienteController {

    private final KycMasterService service;

    public KycClienteController(KycMasterService service) {
        this.service = service;
    }

    @GetMapping
    public List<KycClienteListItem> listar(
            @RequestParam(required = false) String plataforma,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String confianca,
            @RequestParam(required = false) String search) {
        return service.listar(plataforma, status, confianca, search);
    }

    @GetMapping("/{id}")
    public KycClienteDetail porId(@PathVariable Integer id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}/status")
    public KycClienteDetail atualizarStatus(@PathVariable Integer id, @Valid @RequestBody KycStatusUpdateRequest req) {
        return service.atualizarStatus(id, req.getEstadoKyc());
    }

    @PostMapping
    public ResponseEntity<KycClienteDetail> cadastrar(@Valid @RequestBody KycClienteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
