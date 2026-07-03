package com.donjuan.kyc.repository;

import com.donjuan.kyc.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByCpfHash(String cpfHash);
    boolean existsByCpfHash(String cpfHash);
}
