package com.donjuan.kyc.service;

/**
 * Regras de mascaramento do modulo KYC (tabela kyc_master).
 * Os dados de origem sao heterogeneos (CPF, CNPJ, carteiras de
 * estrangeiro tipo "E-xxxxx", numeros corrompidos por exportacao de
 * planilha terminando em ".0" etc.), entao a mascara cai para um
 * formato generico quando o valor nao parece CPF/CNPJ.
 */
public final class KycMaskUtil {

    private KycMaskUtil() {}

    /** Mascara um documento para exibicao, no formato ***.###.###-** para CPF. */
    public static String maskDocumento(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        if (value.contains("*")) return value;

        if (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        }
        String digits = value.replaceAll("\\D", "");

        if (digits.length() == 11) {
            return "***." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-**";
        }
        if (digits.length() == 14) {
            return "**." + digits.substring(2, 5) + "." + digits.substring(5, 8) + "/****-**";
        }
        return genericMask(value);
    }

    private static String genericMask(String value) {
        int len = value.length();
        if (len <= 4) return "*".repeat(len);
        return value.substring(0, 2) + "*".repeat(len - 4) + value.substring(len - 2);
    }

    /** Codigo do cliente (ex.: "55.0") convertido para double; NaN vira infinito (nao entra na faixa 1-54). */
    public static double codigoNumerico(String codigo) {
        if (codigo == null) return Double.POSITIVE_INFINITY;
        try {
            return Double.parseDouble(codigo.trim());
        } catch (NumberFormatException e) {
            return Double.POSITIVE_INFINITY;
        }
    }

    /** Clientes com codigo 1-54 exibem telefone; 55+ tem o telefone ocultado na resposta. */
    public static String maskTelefone(String codigo, String telefone) {
        double n = codigoNumerico(codigo);
        if (n >= 1 && n <= 54) {
            return telefone;
        }
        return "";
    }

    /** WhatsApp e cadastro direto (pessoa fisica indicada) -> ALTA; leads vindos de exchange -> MEDIA. */
    public static String confianca(String plataforma) {
        if ("WHATSAPP".equalsIgnoreCase(plataforma)) return "ALTA";
        return "MEDIA";
    }

    public static String confiancaLabel(String confianca) {
        return "ALTA".equals(confianca) ? "Alta Confiabilidade" : "Média Confiabilidade";
    }

    public static String origem(String plataforma) {
        if (plataforma == null) return "—";
        if ("WHATSAPP".equalsIgnoreCase(plataforma)) return "Pessoa Física";
        String p = plataforma.trim();
        return p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
    }
}
