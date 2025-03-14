package com.concordance.services.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static List<List<String>> splitRegex(List<String> lista, String regex, boolean removeEmpty) {
        List<List<String>> sublistas = new ArrayList<>();
        if(regex == null) {
            sublistas.add(removeEmpty ? lista.stream().filter(i -> !TextUtils.isEmpty(i)).collect(Collectors.toList()) : lista);
            return sublistas;
        }

        List<String> sublistaActual = new ArrayList<>();
        for (String item : lista) {
            if(removeEmpty && TextUtils.isEmpty(item)) continue;

            if (item.matches(regex)) {
                if (!sublistaActual.isEmpty()) {
                    sublistas.add(new ArrayList<>(sublistaActual));
                }
                sublistaActual.clear();
            }
            sublistaActual.add(item);
        }
        
        if (!sublistaActual.isEmpty()) {
            sublistas.add(sublistaActual);
        }
        
        return sublistas;
    }

    public static Map<String, List<String>> groupByRegex(List<String> lista, String regex) {
        Map<String, List<String>> resultado = new LinkedHashMap<>();
        List<String> sublistaActual = new ArrayList<>();
        boolean existRegex = false;
        for (String item : lista) {
            if (item.matches(regex)) {
                if (!sublistaActual.isEmpty()) {
                    String clave = !existRegex ? "" : sublistaActual.get(0).replaceAll(regex, "$1");
                    resultado.put(clave, new ArrayList<>(sublistaActual));
                }
                sublistaActual.clear();
                existRegex = true;
            }
            sublistaActual.add(item);
        }

        if (!sublistaActual.isEmpty()) {
            String clave = !existRegex ? "" : sublistaActual.get(0).replaceAll(regex, "$1");
            resultado.put(clave, sublistaActual);
        }

        return resultado;
    }
}
