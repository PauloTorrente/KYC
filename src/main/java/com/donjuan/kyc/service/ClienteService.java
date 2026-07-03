package com.donjuan.kyc.service;

import com.donjuan.kyc.dto.ClienteRequest;
import com.donjuan.kyc.model.Cliente;
import com.donjuan.kyc.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class ClienteService {

    private final ClienteRepository repo;
    private final String pepper;

    public ClienteService(ClienteRepository repo,
                          @Value("${kyc.cpf.pepper}") String pepper) {
        this.repo = repo;
        this.pepper = pepper;
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String m) { super(m); }
    }
    public static class DuplicateException extends RuntimeException {
        public DuplicateException(String m) { super(m); }
    }

    public Cliente cadastrar(ClienteRequest req) {
        if (!CpfValidator.isValid(req.getCpf())) {
            throw new ValidationException("CPF invalido (digito verificador nao confere)");
        }
        String digits = CpfValidator.onlyDigits(req.getCpf());
        String hash = hashCpf(digits);

        if (repo.existsByCpfHash(hash)) {
            throw new DuplicateException("Cliente com este CPF ja cadastrado");
        }

        Cliente c = new Cliente();
        c.setNome(req.getNome().trim());
        c.setCpfHash(hash);
        c.setCpfMascarado(CpfValidator.mask(digits));
        c.setRegistradoPor(req.getRegistradoPor());
        return repo.save(c);
    }

    private String hashCpf(String digits) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((pepper + digits).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }
}
