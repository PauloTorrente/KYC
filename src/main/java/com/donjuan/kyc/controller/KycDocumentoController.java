package com.donjuan.kyc.controller;

import com.donjuan.kyc.dto.kyc.KycDocumentoResponse;
import com.donjuan.kyc.dto.kyc.KycExtracaoResponse;
import com.donjuan.kyc.dto.kyc.KycVerificacaoIaResponse;
import com.donjuan.kyc.service.KycDocumentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc/clientes/{clienteId}/documentos")
public class KycDocumentoController {

    private final KycDocumentoService service;

    public KycDocumentoController(KycDocumentoService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<KycDocumentoResponse> upload(
            @PathVariable Integer clienteId,
            @RequestParam String tipo,
            @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(clienteId, tipo, arquivo));
    }

    @GetMapping
    public List<KycDocumentoResponse> listar(@PathVariable Integer clienteId) {
        return service.listar(clienteId);
    }

    @GetMapping("/{documentoId}/url")
    public Map<String, String> url(@PathVariable Integer clienteId, @PathVariable Integer documentoId) {
        return Map.of("url", service.urlVisualizacao(clienteId, documentoId));
    }

    /**
     * Le os dados do documento via OCR. Por padrao so retorna a sugestao;
     * passe aplicar=true para sobrescrever o cadastro do cliente com o que foi lido.
     */
    @PostMapping("/{documentoId}/extrair")
    public KycExtracaoResponse extrair(
            @PathVariable Integer clienteId,
            @PathVariable Integer documentoId,
            @RequestParam(defaultValue = "false") boolean aplicar) {
        return service.extrairDados(clienteId, documentoId, aplicar);
    }

    /**
     * Segunda camada de verificacao (Claude), cruzando cadastro + OCR + imagem do
     * documento. Assim como /extrair, so devolve uma sugestao para o operador revisar.
     */
    @PostMapping("/{documentoId}/verificar-ia")
    public KycVerificacaoIaResponse verificarIa(@PathVariable Integer clienteId, @PathVariable Integer documentoId) {
        return service.verificarComIa(clienteId, documentoId);
    }

    @DeleteMapping("/{documentoId}")
    public ResponseEntity<Void> remover(@PathVariable Integer clienteId, @PathVariable Integer documentoId) {
        service.remover(clienteId, documentoId);
        return ResponseEntity.noContent().build();
    }
}
