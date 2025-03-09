package com.concordance.services.util;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {

    public static List<List<String>> split(List<String> in, String divisor) {
        List<List<String>> resultado = new ArrayList<>();
        List<String> subLista = new ArrayList<>();
        for (String elemento : in) {
            if (elemento.equals(divisor)) {
                if (!subLista.isEmpty()) {
                    resultado.add(new ArrayList<>(subLista));
                    subLista.clear();
                }
            } else {
                subLista.add(elemento);
            }
        }

        if (!subLista.isEmpty()) {
            resultado.add(subLista);
        }

        return resultado;
    }
}
