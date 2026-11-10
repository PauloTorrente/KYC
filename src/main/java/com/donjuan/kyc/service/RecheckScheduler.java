package com.donjuan.kyc.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Re-checagem periodica de todos os clientes contra listas restritivas (sancoes/PEP).
 * Listas mudam com o tempo — um cliente aprovado hoje pode aparecer numa lista depois,
 * e o KYC so' percebe isso rodando a checagem de novo, nao so' uma vez no cadastro.
 */
@Component
public class RecheckScheduler {

    private final KycMasterService kycMasterService;

    public RecheckScheduler(KycMasterService kycMasterService) {
        this.kycMasterService = kycMasterService;
    }

    @Scheduled(cron = "${kyc.lista-restritiva.recheck-cron}")
    public void recheckListasRestritivas() {
        kycMasterService.recheckListasRestritivasTodos();
    }
}
