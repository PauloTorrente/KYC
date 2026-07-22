package com.donjuan.kyc.repository;

import com.donjuan.kyc.model.KycMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KycMasterRepository extends JpaRepository<KycMaster, Integer> {

    @Query(value = "select coalesce(max(cast(regexp_replace(codigo, '[^0-9.]', '', 'g') as double precision)), 0) " +
            "from kyc_master", nativeQuery = true)
    Double maiorCodigoNumerico();
}
