package com.app.suspiro.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class GeradorDeSenha {

    private static final String MAIUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String ESPECIAIS = "!@#$%&*?";

    private static final int TAMANHO = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    private GeradorDeSenha() {
    }

    public static String gerar() {

        String todos = MAIUSCULAS + MINUSCULAS + NUMEROS + ESPECIAIS;

        List<Character> caracteres = new ArrayList<>(TAMANHO);

        
        caracteres.add(sortear(MAIUSCULAS));
        caracteres.add(sortear(MINUSCULAS));
        caracteres.add(sortear(NUMEROS));
        caracteres.add(sortear(ESPECIAIS));

        while (caracteres.size() < TAMANHO) {
            caracteres.add(sortear(todos));
        }

        Collections.shuffle(caracteres, RANDOM);

        StringBuilder senha = new StringBuilder(TAMANHO);

        caracteres.forEach(senha::append);

        return senha.toString();
    }

    private static char sortear(String alfabeto) {

        return alfabeto.charAt(RANDOM.nextInt(alfabeto.length()));
    }
}
