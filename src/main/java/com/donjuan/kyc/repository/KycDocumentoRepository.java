package com.donjuan.kyc.repository;

import com.donjuan.kyc.model.KycDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycDocumentoRepository extends JpaRepository<KycDocumento, Integer> {

    List<KycDocumento> findByKycMasterIdOrderByUploadedAtDesc(Integer kycMasterId);

    Optional<KycDocumento> findByIdAndKycMasterId(Integer id, Integer kycMasterId);
}
