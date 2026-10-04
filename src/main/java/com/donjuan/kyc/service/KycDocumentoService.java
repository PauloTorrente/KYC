package com.donjuan.kyc.service;

import com.donjuan.kyc.dto.kyc.KycDocumentoResponse;
import com.donjuan.kyc.dto.kyc.KycExtracaoResponse;
import com.donjuan.kyc.dto.kyc.KycVerificacaoIaResponse;
import com.donjuan.kyc.model.KycDocumento;
import com.donjuan.kyc.model.KycMaster;
import com.donjuan.kyc.repository.KycDocumentoRepository;
import com.donjuan.kyc.repository.KycMasterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class KycDocumentoService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "CPF_FRENTE", "CPF_VERSO", "SELFIE", "COMPROVANTE_RESIDENCIA", "CNH"
    );

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            "image/jpeg", "image/png", "application/pdf"
    );

    private final KycDocumentoRepository documentoRepo;
    private final KycMasterRepository masterRepo;
    private final S3StorageService storage;
    private final KycDocumentoExtracaoService extracaoService;
    private final AnthropicVerificationService verificacaoIaService;
    private final AuditLogService auditLog;

    @Value("${kyc.s3.presigned-url-ttl-minutes}")
    private long presignedUrlTtlMinutes;

    public KycDocumentoService(KycDocumentoRepository documentoRepo, KycMasterRepository masterRepo,
                                S3StorageService storage, KycDocumentoExtracaoService extracaoService,
                                AnthropicVerificationService verificacaoIaService, AuditLogService auditLog) {
        this.documentoRepo = documentoRepo;
        this.masterRepo = masterRepo;
        this.storage = storage;
        this.extracaoService = extracaoService;
        this.verificacaoIaService = verificacaoIaService;
        this.auditLog = auditLog;
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String m) { super(m); }
    }

    public KycDocumentoResponse upload(Integer kycMasterId, String tipoDocumento, MultipartFile arquivo) {
        if (!masterRepo.existsById(kycMasterId)) {
            throw new KycMasterService.NotFoundException("Cliente nao encontrado");
        }
        if (tipoDocumento == null || !TIPOS_PERMITIDOS.contains(tipoDocumento.toUpperCase())) {
            throw new ValidationException("tipo_documento invalido; use um de " + TIPOS_PERMITIDOS);
        }
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ValidationException("arquivo obrigatorio");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new ValidationException("tipo de arquivo invalido; use JPEG, PNG ou PDF");
        }

        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("falha ao ler arquivo enviado", e);
        }
        if (!assinaturaConfere(bytes, contentType)) {
            throw new ValidationException("o conteudo do arquivo nao corresponde ao tipo declarado");
        }

        String tipo = tipoDocumento.toUpperCase();
        String extensao = extensaoPara(contentType);
        String key = "kyc/%d/%s/%s.%s".formatted(kycMasterId, tipo, UUID.randomUUID(), extensao);

        storage.upload(key, new ByteArrayInputStream(bytes), bytes.length, contentType);

        KycDocumento doc = new KycDocumento();
        doc.setKycMasterId(kycMasterId);
        doc.setTipoDocumento(tipo);
        doc.setStorageKey(key);
        doc.setContentType(contentType);
        doc.setTamanhoBytes(arquivo.getSize());

        KycDocumento salvo = documentoRepo.save(doc);
        auditLog.registrar("DOCUMENTO_ENVIADO", kycMasterId, salvo.getId(), tipo);
        return KycDocumentoResponse.from(salvo);
    }

    /**
     * Le um documento via OCR antes mesmo de o cliente existir no sistema — usado pra
     * pre-preencher o formulario de cadastro. Nao salva nada (nem o arquivo no storage,
     * nem o resultado); e' so' uma leitura, o operador confirma os dados antes de criar
     * o cliente de verdade.
     *
     * Depois do OCR, passa o resultado pelo Claude pra conferir contra a propria imagem
     * e corrigir erros obvios de extracao (o Textract erra bastante em documento
     * brasileiro). Se a verificacao por IA nao estiver configurada ou falhar, segue so'
     * com o que o OCR leu — nunca bloqueia o preenchimento por causa disso.
     */
    public KycExtracaoResponse extrairPreview(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ValidationException("arquivo obrigatorio");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new ValidationException("tipo de arquivo invalido; use JPEG, PNG ou PDF");
        }

        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("falha ao ler arquivo enviado", e);
        }
        if (!assinaturaConfere(bytes, contentType)) {
            throw new ValidationException("o conteudo do arquivo nao corresponde ao tipo declarado");
        }

        KycExtracaoResponse ocr = extracaoService.extrairDeBytes(bytes);
        try {
            return verificacaoIaService.corrigirExtracao(ocr, bytes, contentType);
        } catch (AnthropicVerificationService.VerificacaoIaException e) {
            return ocr;
        }
    }

    public List<KycDocumentoResponse> listar(Integer kycMasterId) {
        if (!masterRepo.existsById(kycMasterId)) {
            throw new KycMasterService.NotFoundException("Cliente nao encontrado");
        }
        return documentoRepo.findByKycMasterIdOrderByUploadedAtDesc(kycMasterId).stream()
                .map(KycDocumentoResponse::from)
                .toList();
    }

    public String urlVisualizacao(Integer kycMasterId, Integer documentoId) {
        KycDocumento doc = documentoRepo.findByIdAndKycMasterId(documentoId, kycMasterId)
                .orElseThrow(() -> new KycMasterService.NotFoundException("Documento nao encontrado"));
        auditLog.registrar("DOCUMENTO_VISUALIZADO", kycMasterId, documentoId, doc.getTipoDocumento());
        return storage.presignedGetUrl(doc.getStorageKey(), Duration.ofMinutes(presignedUrlTtlMinutes));
    }

    /**
     * Le os dados do documento via OCR (Textract AnalyzeID). Com aplicar=true,
     * sobrescreve nome/numero/data de nascimento do cliente com o que foi lido —
     * so ligue isso depois de validar que a extracao costuma vir correta.
     */
    public KycExtracaoResponse extrairDados(Integer kycMasterId, Integer documentoId, boolean aplicar) {
        KycDocumento doc = documentoRepo.findByIdAndKycMasterId(documentoId, kycMasterId)
                .orElseThrow(() -> new KycMasterService.NotFoundException("Documento nao encontrado"));

        KycExtracaoResponse extraido = extracaoService.extrair(doc);
        auditLog.registrar("DOCUMENTO_EXTRAIDO", kycMasterId, documentoId, "aplicar=" + aplicar);

        if (aplicar) {
            KycMaster cliente = masterRepo.findById(kycMasterId)
                    .orElseThrow(() -> new KycMasterService.NotFoundException("Cliente nao encontrado"));
            if (extraido.nome != null) cliente.setNombre(extraido.nome);
            if (extraido.documento != null) cliente.setNumero(extraido.documento);
            if (extraido.dataNascimento != null) {
                try {
                    cliente.setDataNascimento(LocalDate.parse(extraido.dataNascimento));
                } catch (DateTimeParseException ignored) {
                    // Textract nem sempre normaliza a data em ISO-8601; quando isso
                    // acontece so devolvemos o texto bruto e nao gravamos no cliente.
                }
            }
            masterRepo.save(cliente);
            auditLog.registrar("DADOS_CLIENTE_SOBRESCRITOS_POR_OCR", kycMasterId, documentoId, null);
        }

        return extraido;
    }

    /**
     * Segunda camada de verificacao (Claude), alem do OCR: cruza cadastro, texto lido
     * por OCR e a imagem do documento para apontar divergencias e sinais de possivel
     * adulteracao. Assim como a extracao por OCR, e' sempre uma sugestao — nunca aplica
     * nada no cadastro nem decide aprovacao/reprovacao sozinha.
     */
    public KycVerificacaoIaResponse verificarComIa(Integer kycMasterId, Integer documentoId) {
        KycDocumento doc = documentoRepo.findByIdAndKycMasterId(documentoId, kycMasterId)
                .orElseThrow(() -> new KycMasterService.NotFoundException("Documento nao encontrado"));
        KycMaster cliente = masterRepo.findById(kycMasterId)
                .orElseThrow(() -> new KycMasterService.NotFoundException("Cliente nao encontrado"));

        KycExtracaoResponse ocr = extracaoService.extrair(doc);
        byte[] arquivo = storage.download(doc.getStorageKey());

        KycVerificacaoIaResponse resultado = verificacaoIaService.verificar(cliente, doc, arquivo, ocr);
        auditLog.registrar("VERIFICACAO_IA_EXECUTADA", kycMasterId, documentoId, "risco=" + resultado.risco);
        return resultado;
    }

    public void remover(Integer kycMasterId, Integer documentoId) {
        KycDocumento doc = documentoRepo.findByIdAndKycMasterId(documentoId, kycMasterId)
                .orElseThrow(() -> new KycMasterService.NotFoundException("Documento nao encontrado"));
        storage.delete(doc.getStorageKey());
        documentoRepo.delete(doc);
        auditLog.registrar("DOCUMENTO_REMOVIDO", kycMasterId, documentoId, doc.getTipoDocumento());
    }

    /**
     * Apaga todos os documentos (arquivo no S3 + registro) de um cliente. Usado ao
     * excluir o cadastro do titular, para atender o direito de eliminacao (LGPD art. 18,
     * VI) sem deixar documentos orfaos no armazenamento.
     */
    public void removerTodosDoCliente(Integer kycMasterId) {
        List<KycDocumento> documentos = documentoRepo.findByKycMasterIdOrderByUploadedAtDesc(kycMasterId);
        for (KycDocumento doc : documentos) {
            storage.delete(doc.getStorageKey());
        }
        documentoRepo.deleteAll(documentos);
        if (!documentos.isEmpty()) {
            auditLog.registrar("DOCUMENTOS_REMOVIDOS_EM_CASCATA", kycMasterId, documentos.size() + " documento(s)");
        }
    }

    /** Confere os magic bytes do arquivo contra o Content-Type declarado, que e' controlado pelo cliente. */
    private boolean assinaturaConfere(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                    && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;
            case "application/pdf" -> bytes.length >= 4
                    && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
            default -> false;
        };
    }

    private String extensaoPara(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "application/pdf" -> "pdf";
            default -> "bin";
        };
    }
}
