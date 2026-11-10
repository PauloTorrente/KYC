package com.donjuan.kyc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;

/**
 * Checagem contra listas restritivas internacionais — sancoes (OFAC/ONU/UE) e PEP
 * (Pessoa Exposta Politicamente) — via OpenSanctions, base aberta que agrega essas
 * fontes. E' o tipo de verificacao que o BACEN/COAF espera de uma exchange de ativos
 * virtuais pra prevencao de lavagem de dinheiro (PLD/FT).
 *
 * Diferente do Open Finance, essa e' uma consulta publica: nao precisa de nenhuma
 * autorizacao do cliente, so' usa nome/nascimento/nacionalidade que ja estao no
 * cadastro. Mesmo assim e' sempre uma sugestao — um nome parecido nao significa que e'
 * a mesma pessoa; cabe ao operador humano confirmar antes de qualquer decisao.
 */
@Service
public class ListaRestritivaService {

    private static final String API_URL = "https://api.opensanctions.org/match/default";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${kyc.opensanctions.api-key:}")
    private String apiKey;

    public static class ListaRestritivaException extends RuntimeException {
        public ListaRestritivaException(String m) { super(m); }
        public ListaRestritivaException(String m, Throwable c) { super(m, c); }
    }

    /** Usado pela re-checagem periodica pra pular o lote todo sem gerar erro repetido se a chave nao estiver definida. */
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public JsonNode buscar(String nome, LocalDate dataNascimento, String nacionalidade) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ListaRestritivaException(
                    "checagem de listas restritivas nao configurada (defina OPENSANCTIONS_API_KEY)");
        }
        if (nome == null || nome.isBlank()) {
            throw new ListaRestritivaException("cliente sem nome cadastrado, nao da' pra checar");
        }

        ObjectNode props = MAPPER.createObjectNode();
        props.set("name", umElemento(nome));
        if (dataNascimento != null) props.set("birthDate", umElemento(dataNascimento.toString()));
        if (nacionalidade != null && !nacionalidade.isBlank()) props.set("nationality", umElemento(nacionalidade));

        ObjectNode query = MAPPER.createObjectNode();
        query.put("schema", "Person");
        query.set("properties", props);

        ObjectNode queries = MAPPER.createObjectNode();
        queries.set("cliente", query);

        ObjectNode body = MAPPER.createObjectNode();
        body.set("queries", queries);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?threshold=0.7&limit=5"))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "ApiKey " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new ListaRestritivaException("falha ao chamar a API de listas restritivas", e);
        }

        if (response.statusCode() >= 400) {
            throw new ListaRestritivaException(
                    "servico de listas restritivas retornou status " + response.statusCode());
        }

        try {
            return MAPPER.readTree(response.body()).path("responses").path("cliente");
        } catch (Exception e) {
            throw new ListaRestritivaException("falha ao interpretar resposta da API de listas restritivas", e);
        }
    }

    private ArrayNode umElemento(String valor) {
        ArrayNode arr = MAPPER.createArrayNode();
        arr.add(valor);
        return arr;
    }
}
