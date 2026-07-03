package com.donjuan.kyc.service;

/**
 * Validacao de CPF puramente offline: limpa, checa formato e valida
 * os dois digitos verificadores. Nao consulta nenhuma base externa.
 */
public final class CpfValidator {

    private CpfValidator() {}

    /** Remove tudo que nao for digito. */
    public static String onlyDigits(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\D", "");
    }

    /** Mascara para exibicao: 123.***.***-09 (esconde o miolo). */
    public static String mask(String cpfDigits) {
        if (cpfDigits == null || cpfDigits.length() != 11) return "***";
        return cpfDigits.substring(0, 3) + ".***.***-" + cpfDigits.substring(9);
    }

    public static boolean isValid(String raw) {
        String cpf = onlyDigits(raw);
        if (cpf.length() != 11) return false;
        // rejeita sequencias repetidas (00000000000, 11111111111, ...)
        if (cpf.chars().distinct().count() == 1) return false;

        int d1 = checkDigit(cpf, 9, 10);
        int d2 = checkDigit(cpf, 10, 11);
        return d1 == (cpf.charAt(9) - '0') && d2 == (cpf.charAt(10) - '0');
    }

    private static int checkDigit(String cpf, int length, int startWeight) {
        int sum = 0;
        int weight = startWeight;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * weight;
            weight--;
        }
        int mod = sum % 11;
        return (mod < 2) ? 0 : 11 - mod;
    }
}
