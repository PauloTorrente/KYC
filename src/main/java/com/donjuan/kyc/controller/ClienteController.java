package com.donjuan.kyc.controller;

import com.donjuan.kyc.dto.ClienteRequest;
import com.donjuan.kyc.dto.ClienteResponse;
import com.donjuan.kyc.model.Cliente;
import com.donjuan.kyc.repository.ClienteRepository;
import com.donjuan.kyc.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService service;
    private final ClienteRepository repo;

    public ClienteController(ClienteService service, ClienteRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody ClienteRequest req) {
        Cliente c = service.cadastrar(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ClienteResponse.from(c));
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return repo.findAll().stream().map(ClienteResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> porId(@PathVariable Long id) {
        return repo.findById(id)
                .map(c -> ResponseEntity.ok(ClienteResponse.from(c)))
                .orElse(ResponseEntity.notFound().build());
    }
}
