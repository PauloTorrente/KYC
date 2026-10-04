package com.donjuan.kyc.service;

import com.donjuan.kyc.dto.kyc.KycExtracaoResponse;
import com.donjuan.kyc.dto.kyc.KycVerificacaoIaResponse;
import com.donjuan.kyc.model.KycDocumento;
import com.donjuan.kyc.model.KycMaster;
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
import java.util.Base64;

/**
 * Segunda camada de verificacao do KYC, alem do OCR (Textract): manda o cadastro
 * declarado pelo cliente, o que o OCR leu e a imagem do proprio documento para o
 * modelo cruzar as tres fontes e apontar divergencias ou sinais de possivel
 * adulteracao. E' sempre uma sugestao para revisao humana, nunca uma decisao
 * automatica (mesmo padrao conservador do restante do fluxo de KYC).
 *
 * Importante (LGPD): esta chamada envia dados pessoais e a imagem do documento do
 * titular para um processador externo (Anthropic). Isso torna a Anthropic uma
 * operadora de dados pessoais do negocio e deve constar na politica de privacidade
 * e no mapeamento de fornecedores/terceiros que tratam dados dos clientes.
 */
@Service
public class AnthropicVerificationService {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    // Limite conservador para o tamanho da imagem enviada (a API da Anthropic recomenda
    // ate ~5MB por imagem em base64); acima disso a verificacao segue sem a imagem.
    private static final int TAMANHO_MAXIMO_IMAGEM_BYTES = 4_500_000;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Value("${kyc.ia.anthropic.api-key:}")
    private String apiKey;

    @Value("${kyc.ia.anthropic.model}")
    private String model;

    public static class VerificacaoIaException extends RuntimeException {
        public VerificacaoIaException(String m) { super(m); }
        public VerificacaoIaException(String m, Throwable c) { super(m, c); }
    }

    public KycVerificacaoIaResponse verificar(KycMaster cliente, KycDocumento documento,
                                               byte[] arquivo, KycExtracaoResponse ocr) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VerificacaoIaException(
                    "verificacao por IA nao configurada (defina a variavel de ambiente ANTHROPIC_API_KEY)");
        }

        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.set("tools", ferramentaDeRelato());
        ObjectNode toolChoice = MAPPER.createObjectNode();
        toolChoice.put("type", "tool");
        toolChoice.put("name", "reportar_verificacao");
        body.set("tool_choice", toolChoice);
        body.set("messages", mensagens(cliente, documento, arquivo, ocr));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new VerificacaoIaException("falha ao chamar o servico de verificacao por IA", e);
        }

        if (response.statusCode() != 200) {
            throw new VerificacaoIaException(
                    "servico de verificacao por IA retornou status " + response.statusCode());
        }

        return extrairResultado(response.body(), KycVerificacaoIaResponse.class);
    }

    /**
     * Confere a leitura do OCR (Textract) contra a propria imagem do documento e corrige
     * erros obvios de extracao (ex.: o Textract confundir o rotulo de um campo, tipo
     * "nacionalidade", com o valor de outro, tipo "nome"). Usada antes de sugerir os dados
     * no formulario de cadastro de um cliente novo, pra chegar uma leitura melhor do que
     * o Textract sozinho — mas continua sendo so' uma sugestao pro operador revisar.
     */
    public KycExtracaoResponse corrigirExtracao(KycExtracaoResponse ocrOriginal, byte[] arquivo, String mediaType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VerificacaoIaException(
                    "verificacao por IA nao configurada (defina a variavel de ambiente ANTHROPIC_API_KEY)");
        }

        ObjectNode body = MAPPER.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.set("tools", ferramentaDeCorrecao());
        ObjectNode toolChoice = MAPPER.createObjectNode();
        toolChoice.put("type", "tool");
        toolChoice.put("name", "reportar_extracao_corrigida");
        body.set("tool_choice", toolChoice);
        body.set("messages", mensagensCorrecao(ocrOriginal, arquivo, mediaType));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new VerificacaoIaException("falha ao chamar o servico de verificacao por IA", e);
        }

        if (response.statusCode() != 200) {
            throw new VerificacaoIaException(
                    "servico de verificacao por IA retornou status " + response.statusCode());
        }

        return extrairResultado(response.body(), KycExtracaoResponse.class);
    }

    private ArrayNode ferramentaDeRelato() {
        ArrayNode tools = MAPPER.createArrayNode();
        ObjectNode tool = MAPPER.createObjectNode();
        tool.put("name", "reportar_verificacao");
        tool.put("description", "Reporta o resultado da verificacao cruzada do documento de KYC.");

        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = MAPPER.createObjectNode();

        props.set("consistente", campo("boolean",
                "true se o cadastro, o texto lido por OCR e o documento batem entre si"));
        ObjectNode risco = campo("string", "Nivel de risco percebido para revisao humana");
        ArrayNode riscoEnum = MAPPER.createArrayNode();
        riscoEnum.add("BAIXO").add("MEDIO").add("ALTO");
        risco.set("enum", riscoEnum);
        props.set("risco", risco);

        props.set("divergencias", listaDeTexto(
                "Campos que nao batem entre cadastro, OCR e documento, com a diferenca encontrada; lista vazia se nao houver"));
        props.set("sinais_de_alerta", listaDeTexto(
                "Sinais visuais de possivel adulteracao ou inconsistencia no documento; lista vazia se nao houver"));
        props.set("parecer", campo("string", "Resumo curto em portugues para o operador humano decidir"));

        schema.set("properties", props);
        ArrayNode required = MAPPER.createArrayNode();
        required.add("consistente").add("risco").add("divergencias").add("sinais_de_alerta").add("parecer");
        schema.set("required", required);

        tool.set("input_schema", schema);
        tools.add(tool);
        return tools;
    }

    private ArrayNode ferramentaDeCorrecao() {
        ArrayNode tools = MAPPER.createArrayNode();
        ObjectNode tool = MAPPER.createObjectNode();
        tool.put("name", "reportar_extracao_corrigida");
        tool.put("description",
                "Reporta os dados do documento apos conferir a leitura automatica (OCR) contra a propria imagem.");

        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = MAPPER.createObjectNode();

        props.set("nome", campo("string", "Nome completo do titular, lido da imagem (null se nao conseguir identificar)"));
        props.set("documento", campo("string", "Numero do documento, lido da imagem (null se nao conseguir identificar)"));
        props.set("data_nascimento", campo("string", "Data de nascimento no formato DD/MM/AAAA, lida da imagem (null se nao conseguir identificar)"));
        props.set("endereco", campo("string", "Endereco, lido da imagem, se constar no documento (null se nao houver)"));
        props.set("correcoes_ia", listaDeTexto(
                "Lista curta descrevendo cada correcao feita em relacao a leitura original do OCR; lista vazia se o OCR ja estava correto"));

        schema.set("properties", props);
        ArrayNode required = MAPPER.createArrayNode();
        required.add("nome").add("documento").add("data_nascimento").add("endereco").add("correcoes_ia");
        schema.set("required", required);

        tool.set("input_schema", schema);
        tools.add(tool);
        return tools;
    }

    private ObjectNode campo(String tipo, String descricao) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", tipo);
        node.put("description", descricao);
        return node;
    }

    private ObjectNode listaDeTexto(String descricao) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "array");
        node.put("description", descricao);
        ObjectNode items = MAPPER.createObjectNode();
        items.put("type", "string");
        node.set("items", items);
        return node;
    }

    private ArrayNode mensagens(KycMaster cliente, KycDocumento documento, byte[] arquivo, KycExtracaoResponse ocr) {
        ArrayNode content = MAPPER.createArrayNode();

        ObjectNode textBlock = MAPPER.createObjectNode();
        textBlock.put("type", "text");
        textBlock.put("text", prompt(cliente, documento, ocr));
        content.add(textBlock);

        boolean cabeImagem = arquivo != null && arquivo.length > 0 && arquivo.length <= TAMANHO_MAXIMO_IMAGEM_BYTES;
        String mediaType = documento.getContentType();
        if (cabeImagem && mediaType != null) {
            String tipoBloco = "application/pdf".equals(mediaType) ? "document" : "image";
            ObjectNode fileBlock = MAPPER.createObjectNode();
            fileBlock.put("type", tipoBloco);
            ObjectNode source = MAPPER.createObjectNode();
            source.put("type", "base64");
            source.put("media_type", mediaType);
            source.put("data", Base64.getEncoder().encodeToString(arquivo));
            fileBlock.set("source", source);
            content.add(fileBlock);
        }

        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        message.set("content", content);

        ArrayNode messages = MAPPER.createArrayNode();
        messages.add(message);
        return messages;
    }

    private String prompt(KycMaster cliente, KycDocumento documento, KycExtracaoResponse ocr) {
        return """
                Voce e' um analista de compliance apoiando a revisao de KYC de uma empresa de \
                cambio de ativos virtuais (P2P/OTC). Compare as tres fontes abaixo sobre o mesmo \
                titular e aponte divergencias relevantes (nao aponte diferencas triviais de \
                formatacao/acentuacao). Se a imagem do documento estiver anexa, avalie tambem se \
                ha sinais visuais de possivel adulteracao (fonte inconsistente, alinhamento \
                estranho, recorte/colagem, foto destoante do resto do documento). Nao tome a \
                decisao de aprovar ou reprovar o cliente — apenas relate o que encontrou, para um \
                operador humano decidir. Responda somente atraves da ferramenta fornecida.

                Tipo de documento enviado: %s

                1) Dados cadastrados pelo cliente no sistema:
                nome: %s
                numero do documento: %s
                data de nascimento: %s
                endereco: %s

                2) Dados lidos por OCR (AWS Textract) no mesmo documento:
                nome: %s
                numero do documento: %s
                data de nascimento: %s
                endereco: %s
                """.formatted(
                nullToTraco(documento.getTipoDocumento()),
                nullToTraco(cliente.getNombre()),
                nullToTraco(cliente.getNumero()),
                cliente.getDataNascimento() != null ? cliente.getDataNascimento().toString() : "-",
                nullToTraco(cliente.getEnderecoCompleto()),
                nullToTraco(ocr.nome),
                nullToTraco(ocr.documento),
                nullToTraco(ocr.dataNascimento),
                nullToTraco(ocr.endereco)
        );
    }

    private String promptCorrecao(KycExtracaoResponse ocr) {
        return """
                Voce e' um analista de compliance conferindo a leitura automatica (OCR) de um \
                documento de identidade antes de um operador usar esses dados pra cadastrar um \
                cliente. O OCR (AWS Textract) e' treinado principalmente pra documentos \
                americanos e erra com frequencia em documento brasileiro ou de outros paises — \
                por exemplo, pode confundir o rotulo de um campo (como "nacionalidade") com o \
                valor de outro (como "nome").

                Olhe a imagem do documento anexa e confira cada campo abaixo lido pelo OCR. Se \
                algum estiver claramente errado (ex.: um "nome" que na verdade e' uma \
                nacionalidade ou palavra solta sem sentido como nome, um numero capturado no \
                campo errado), corrija usando o que voce mesmo le' na imagem. Se um campo nao \
                estiver visivel na imagem ou voce nao tiver certeza, repita o valor original do \
                OCR (ou null, se o OCR tambem nao tinha achado nada) — nunca invente um dado \
                que nao esta na imagem. Liste em "correcoes_ia" cada correcao feita, de forma \
                curta; deixe a lista vazia se o OCR ja estava certo. Responda somente atraves da \
                ferramenta fornecida.

                Leitura original do OCR (AWS Textract):
                nome: %s
                numero do documento: %s
                data de nascimento: %s
                endereco: %s
                """.formatted(
                nullToTraco(ocr.nome),
                nullToTraco(ocr.documento),
                nullToTraco(ocr.dataNascimento),
                nullToTraco(ocr.endereco)
        );
    }

    private ArrayNode mensagensCorrecao(KycExtracaoResponse ocr, byte[] arquivo, String mediaType) {
        ArrayNode content = MAPPER.createArrayNode();

        ObjectNode textBlock = MAPPER.createObjectNode();
        textBlock.put("type", "text");
        textBlock.put("text", promptCorrecao(ocr));
        content.add(textBlock);

        boolean cabeImagem = arquivo != null && arquivo.length > 0 && arquivo.length <= TAMANHO_MAXIMO_IMAGEM_BYTES;
        if (cabeImagem && mediaType != null) {
            String tipoBloco = "application/pdf".equals(mediaType) ? "document" : "image";
            ObjectNode fileBlock = MAPPER.createObjectNode();
            fileBlock.put("type", tipoBloco);
            ObjectNode source = MAPPER.createObjectNode();
            source.put("type", "base64");
            source.put("media_type", mediaType);
            source.put("data", Base64.getEncoder().encodeToString(arquivo));
            fileBlock.set("source", source);
            content.add(fileBlock);
        }

        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        message.set("content", content);

        ArrayNode messages = MAPPER.createArrayNode();
        messages.add(message);
        return messages;
    }

    private String nullToTraco(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }

    private <T> T extrairResultado(String responseBody, Class<T> tipo) {
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            for (JsonNode block : root.path("content")) {
                if ("tool_use".equals(block.path("type").asText())) {
                    return MAPPER.treeToValue(block.path("input"), tipo);
                }
            }
            throw new VerificacaoIaException("resposta da IA nao trouxe o resultado esperado");
        } catch (VerificacaoIaException e) {
            throw e;
        } catch (Exception e) {
            throw new VerificacaoIaException("falha ao interpretar a resposta da IA", e);
        }
    }
}
