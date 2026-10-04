package com.donjuan.kyc.controller;

import com.donjuan.kyc.dto.kyc.KycExtracaoResponse;
import com.donjuan.kyc.service.KycDocumentoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Leitura de documento (OCR) antes de o cliente existir no sistema — usado pra
 * pre-preencher o formulario de cadastro de um cliente novo.
 */
@RestController
@RequestMapping("/api/kyc/documentos")
public class KycDocumentoPreviewController {

    private final KycDocumentoService service;

    public KycDocumentoPreviewController(KycDocumentoService service) {
        this.service = service;
    }

    @PostMapping(value = "/extrair-preview", consumes = "multipart/form-data")
    public KycExtracaoResponse extrairPreview(@RequestParam("arquivo") MultipartFile arquivo) {
        return service.extrairPreview(arquivo);
    }
}
