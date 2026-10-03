package com.donjuan.kyc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Trilha de auditoria de acesso a dados pessoais do KYC (LGPD art. 37/46 - dever de
 * prestacao de contas e de demonstrar a adocao de medidas de seguranca). Cada acesso a
 * documento ou dado sensivel de um titular fica registrado com quem, o que e quando,
 * para permitir responder a incidentes ou solicitacoes de titulares sobre quem tratou
 * seus dados. Usa um logger dedicado ("AUDIT") para poder ser roteado para um destino
 * separado (arquivo/indice proprio) na configuracao de logging sem mexer no codigo.
 */
@Service
public class AuditLogService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    public void registrar(String acao, Integer kycMasterId, String detalhe) {
        audit.info("acao={} ator={} kyc_master_id={} detalhe={}",
                acao, atorAtual(), kycMasterId, detalhe == null ? "" : detalhe);
    }

    public void registrar(String acao, Integer kycMasterId, Integer documentoId, String detalhe) {
        audit.info("acao={} ator={} kyc_master_id={} documento_id={} detalhe={}",
                acao, atorAtual(), kycMasterId, documentoId, detalhe == null ? "" : detalhe);
    }

    private String atorAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? String.valueOf(auth.getPrincipal()) : "desconhecido";
    }
}
