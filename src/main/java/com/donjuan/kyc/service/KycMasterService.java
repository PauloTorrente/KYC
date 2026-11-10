package com.donjuan.kyc.service;

import com.donjuan.kyc.dto.kyc.KycClienteDetail;
import com.donjuan.kyc.dto.kyc.KycClienteListItem;
import com.donjuan.kyc.dto.kyc.KycClienteRequest;
import com.donjuan.kyc.dto.kyc.KycListaRestritivaResponse;
import com.donjuan.kyc.dto.kyc.KycValidacaoCpfResponse;
import com.donjuan.kyc.model.KycMaster;
import com.donjuan.kyc.repository.KycMasterRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class KycMasterService {

    private static final Logger log = LoggerFactory.getLogger(KycMasterService.class);

    private final KycMasterRepository repo;
    private final KycDocumentoService documentoService;
    private final ListaRestritivaService listaRestritivaService;
    private final ReceitaFederalCpfService receitaFederalCpfService;
    private final AuditLogService auditLog;

    public KycMasterService(KycMasterRepository repo, KycDocumentoService documentoService,
                             ListaRestritivaService listaRestritivaService,
                             ReceitaFederalCpfService receitaFederalCpfService, AuditLogService auditLog) {
        this.repo = repo;
        this.documentoService = documentoService;
        this.listaRestritivaService = listaRestritivaService;
        this.receitaFederalCpfService = receitaFederalCpfService;
        this.auditLog = auditLog;
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
        auditLog.registrar("CLIENTE_VISUALIZADO", id, null);
        return KycClienteDetail.from(c);
    }

    public KycClienteDetail atualizarStatus(Integer id, String estadoKyc) {
        KycMaster c = repo.findById(id).orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));
        c.setEstadoKyc(estadoKyc);
        auditLog.registrar("STATUS_KYC_ALTERADO", id, estadoKyc);
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

    /**
     * Exclui o cadastro do titular e, em cascata, todos os documentos associados
     * (arquivo no S3 + registro), para que nenhum dado pessoal fique orfao apos a
     * exclusao (LGPD art. 18, VI - direito a eliminacao dos dados pessoais).
     */
    public void remover(Integer id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Cliente nao encontrado");
        }
        documentoService.removerTodosDoCliente(id);
        repo.deleteById(id);
        auditLog.registrar("CLIENTE_REMOVIDO", id, null);
    }

    /**
     * Checa o cliente contra listas restritivas internacionais (sancoes OFAC/ONU/UE) e
     * PEP (Pessoa Exposta Politicamente), via OpenSanctions. Consulta publica, nao exige
     * autorizacao do cliente. Sempre uma sugestao — nome parecido nao e' a mesma pessoa,
     * cabe ao operador confirmar. O resultado fica salvo no cadastro (alerta + data da
     * ultima checagem), tanto quando chamado manualmente quanto pela re-checagem
     * periodica automatica (ver recheckListasRestritivasTodos).
     */
    public KycListaRestritivaResponse verificarListasRestritivas(Integer id) {
        KycMaster cliente = repo.findById(id).orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));
        KycListaRestritivaResponse r = checarListaRestritiva(cliente);

        cliente.setListaRestritivaAlerta(r.possivelMatch);
        cliente.setListaRestritivaVerificadoEm(LocalDateTime.now());
        repo.save(cliente);

        auditLog.registrar("LISTA_RESTRITIVA_VERIFICADA", id, "possivel_match=" + r.possivelMatch);
        return r;
    }

    private KycListaRestritivaResponse checarListaRestritiva(KycMaster cliente) {
        JsonNode resposta = listaRestritivaService.buscar(
                cliente.getNombre(), cliente.getDataNascimento(), cliente.getNacionalidade());

        List<KycListaRestritivaResponse.Resultado> resultados = new ArrayList<>();
        for (JsonNode item : resposta.path("results")) {
            boolean bateu = item.path("match").asBoolean(false);
            double score = item.path("score").asDouble(0);
            if (!bateu && score < 0.7) continue;

            KycListaRestritivaResponse.Resultado r = new KycListaRestritivaResponse.Resultado();
            r.nome = item.path("caption").asText(null);
            r.score = score;

            List<String> listas = new ArrayList<>();
            item.path("datasets").forEach(d -> listas.add(d.asText()));
            r.listas = listas;

            List<String> topicos = new ArrayList<>();
            item.path("properties").path("topics").forEach(t -> topicos.add(t.asText()));
            r.topicos = topicos;

            resultados.add(r);
        }

        KycListaRestritivaResponse r = new KycListaRestritivaResponse();
        r.resultados = resultados;
        r.possivelMatch = !resultados.isEmpty();
        r.parecer = r.possivelMatch
                ? "Encontrada(s) " + resultados.size() + " possivel(is) correspondencia(s) em listas de sancao/PEP — revisar manualmente antes de prosseguir."
                : "Nenhuma correspondencia encontrada em listas de sancao ou PEP.";
        return r;
    }

    /**
     * Re-checagem periodica automatica (ver RecheckScheduler): listas de sancao/PEP
     * mudam com o tempo, entao um cliente aprovado hoje pode entrar numa lista depois.
     * Roda pra todo cliente com nome cadastrado; erros em um cliente (ex: nome vazio,
     * falha pontual na API) nao interrompem o lote, so' ficam registrados no log.
     */
    public void recheckListasRestritivasTodos() {
        if (!listaRestritivaService.configurado()) {
            log.info("re-checagem periodica de listas restritivas pulada: OPENSANCTIONS_API_KEY nao definida");
            return;
        }

        List<KycMaster> clientes = repo.findAll();
        int verificados = 0;
        int comAlerta = 0;
        for (KycMaster cliente : clientes) {
            if (cliente.getNombre() == null || cliente.getNombre().isBlank()) continue;
            try {
                KycListaRestritivaResponse r = checarListaRestritiva(cliente);
                cliente.setListaRestritivaAlerta(r.possivelMatch);
                cliente.setListaRestritivaVerificadoEm(LocalDateTime.now());
                repo.save(cliente);
                verificados++;
                if (r.possivelMatch) {
                    comAlerta++;
                    auditLog.registrar("LISTA_RESTRITIVA_ALERTA_AUTOMATICO", cliente.getId(), "recheck periodico");
                }
            } catch (Exception e) {
                log.warn("falha na re-checagem periodica do cliente {}: {}", cliente.getId(), e.getMessage());
            }
        }
        log.info("re-checagem periodica de listas restritivas concluida: {} clientes verificados, {} com alerta",
                verificados, comAlerta);
    }

    /**
     * Confirma, direto na base da Receita Federal (via Serpro), se o CPF do cliente
     * existe, esta' regular e se o nome bate. Nao e' antecedente criminal nem score de
     * risco — so' confirma que o CPF e' real e os dados conferem. Sempre uma sugestao
     * pro operador revisar.
     */
    public KycValidacaoCpfResponse verificarCpfReceitaFederal(Integer id) {
        KycMaster cliente = repo.findById(id).orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        JsonNode resposta = receitaFederalCpfService.consultar(cliente.getNumero(), cliente.getDataNascimento());

        KycValidacaoCpfResponse r = new KycValidacaoCpfResponse();
        r.situacaoCadastral = resposta.path("situacaoCadastral").asText(null);
        r.nomeReceita = resposta.path("nome").asText(null);

        boolean regular = "Regular".equalsIgnoreCase(r.situacaoCadastral);
        boolean nomeBate = r.nomeReceita != null && cliente.getNombre() != null
                && normalizarNome(r.nomeReceita).equals(normalizarNome(cliente.getNombre()));

        r.consistente = regular && nomeBate;
        if (!regular) {
            r.parecer = "Situacao cadastral do CPF na Receita Federal: " + r.situacaoCadastral
                    + " — atencao antes de aprovar.";
        } else if (!nomeBate) {
            r.parecer = "CPF regular, mas o nome cadastrado ('%s') diverge do nome na Receita Federal ('%s')."
                    .formatted(cliente.getNombre(), r.nomeReceita);
        } else {
            r.parecer = "CPF regular e nome confere com a Receita Federal.";
        }

        auditLog.registrar("CPF_VALIDADO_RECEITA_FEDERAL", id, "situacao=" + r.situacaoCadastral);
        return r;
    }

    private String normalizarNome(String nome) {
        return nome.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String proximoCodigo() {
        Double maior = repo.maiorCodigoNumerico();
        double proximo = (maior == null ? 0 : maior) + 1;
        return (Math.floor(proximo) == proximo ? (long) proximo + ".0" : String.valueOf(proximo));
    }
}
