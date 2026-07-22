package com.donjuan.kyc.service;

import com.donjuan.kyc.dto.kyc.KycClienteDetail;
import com.donjuan.kyc.dto.kyc.KycClienteListItem;
import com.donjuan.kyc.dto.kyc.KycClienteRequest;
import com.donjuan.kyc.model.KycMaster;
import com.donjuan.kyc.repository.KycMasterRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class KycMasterService {

    private final KycMasterRepository repo;

    public KycMasterService(KycMasterRepository repo) {
        this.repo = repo;
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String m) { super(m); }
    }

    public List<KycClienteListItem> listar(String plataforma, String status, String confianca, String search) {
        List<KycMaster> all = repo.findAll();

        return all.stream()
                .map(KycClienteListItem::from)
                .filter(c -> isBlankOrTodos(plataforma) || c.plataforma != null && c.plataforma.equalsIgnoreCase(plataforma))
                .filter(c -> isBlankOrTodos(status) || c.estadoKyc != null && c.estadoKyc.equalsIgnoreCase(status))
                .filter(c -> isBlankOrTodos(confianca) || c.confianca != null && c.confianca.equalsIgnoreCase(confianca))
                .filter(c -> matchesSearch(c, search))
                .sorted(Comparator.comparingDouble(c -> KycMaskUtil.codigoNumerico(c.codigo)))
                .toList();
    }

    private boolean isBlankOrTodos(String v) {
        return v == null || v.isBlank() || "todos".equalsIgnoreCase(v);
    }

    private boolean matchesSearch(KycClienteListItem c, String search) {
        if (search == null || search.isBlank()) return true;
        String term = search.trim().toLowerCase(Locale.ROOT);
        return (c.nome != null && c.nome.toLowerCase(Locale.ROOT).contains(term))
                || (c.codigo != null && c.codigo.toLowerCase(Locale.ROOT).contains(term));
    }

    public KycClienteDetail buscarPorId(Integer id) {
        KycMaster c = repo.findById(id).orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));
        return KycClienteDetail.from(c);
    }

    public KycClienteDetail atualizarStatus(Integer id, String estadoKyc) {
        KycMaster c = repo.findById(id).orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));
        c.setEstadoKyc(estadoKyc);
        return KycClienteDetail.from(repo.save(c));
    }

    public KycClienteDetail cadastrar(KycClienteRequest req) {
        KycMaster c = new KycMaster();
        c.setCodigo(proximoCodigo());
        c.setNombre(req.getNome().trim());
        c.setDocumento(req.getDocumento());
        c.setNumero(req.getNumero());
        c.setNacionalidade(req.getNacionalidade());
        c.setTelefone(req.getTelefone());
        c.setExtra(req.getExtra());
        c.setPlataforma(req.getPlataforma());
        c.setEstadoKyc(req.getEstadoKyc() != null ? req.getEstadoKyc() : "PENDENTE");
        c.setEnderecoCompleto(req.getEnderecoCompleto());
        c.setCidade(req.getCidade());
        c.setEstadoProvincia(req.getEstadoProvincia());
        c.setCepPostal(req.getCepPostal());
        c.setPaisFiscal(req.getPaisFiscal());
        c.setDocumentoVerificado(req.getDocumentoVerificado());
        c.setSelfieVerificada(req.getSelfieVerificada());
        c.setComprovanteResidencia(req.getComprovanteResidencia());
        c.setIndicadoPor(req.getIndicadoPor());

        // colunas de CNH pedidas na especificacao
        c.setDataCadastro(req.getDataCadastro());
        c.setCnhRegistro(req.getCnhRegistro());
        c.setCnhValidade(req.getCnhValidade());
        c.setCnhEmissao(req.getCnhEmissao());
        c.setCnhPrimeiraHabilitacao(req.getCnhPrimeiraHabilitacao());
        c.setFiliacao(req.getFiliacao());
        c.setDataNascimento(req.getDataNascimento());

        c.setConfianca(KycMaskUtil.confianca(c.getPlataforma()));
        c.setConfiancaLabel(KycMaskUtil.confiancaLabel(c.getConfianca()));
        c.setOrigem(KycMaskUtil.origem(c.getPlataforma()));

        return KycClienteDetail.from(repo.save(c));
    }

    public void remover(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Cliente nao encontrado");
        }
        repo.deleteById(id);
    }

    private String proximoCodigo() {
        Double maior = repo.maiorCodigoNumerico();
        double proximo = (maior == null ? 0 : maior) + 1;
        return (Math.floor(proximo) == proximo ? (long) proximo + ".0" : String.valueOf(proximo));
    }
}
