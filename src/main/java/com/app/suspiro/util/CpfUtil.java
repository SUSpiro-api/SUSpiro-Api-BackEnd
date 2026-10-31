package com.app.suspiro.util;

/** Utilitários de CPF sem registrar ou expor o valor recebido. */
public final class CpfUtil {

    private CpfUtil() {}

    public static String normalizar(String cpf) {
        return cpf == null ? "" : cpf.trim().replaceAll("[.\\-\\s]", "");
    }

    public static boolean valido(String cpf) {
        String valor = normalizar(cpf);
        if (!valor.matches("\\d{11}") || valor.chars().distinct().count() == 1) {
            return false;
        }

        int primeiro = calcularDigito(valor, 9, 10);
        if (Character.getNumericValue(valor.charAt(9)) != primeiro) return false;
        int segundo = calcularDigito(valor, 10, 11);
        return Character.getNumericValue(valor.charAt(10)) == segundo;
    }

    private static int calcularDigito(String cpf, int tamanho, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += Character.getNumericValue(cpf.charAt(i)) * (pesoInicial - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
