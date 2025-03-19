package com.concordance.services.vo.interlineal;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class NotationVo {

    private String code;
    private String tipo;
    private String tiempo; 
    private String voz;
    private String modo;
    private String persona;
    private String caso;
    private String numero;
    private String genero;
    private String sufijo;

    private String tiempoConj; 
    private String personaConj;
    private String numeroConj;
    private String generoConj;
    private String language;

    public String summary() {
        List<String> campos = new ArrayList<>();
        campos.add(tipo);
        if("griego".equals(language)) {
            campos.add(tiempo);
            campos.add(voz);
            campos.add(modo);
            campos.add(persona);
            campos.add(caso);
            campos.add(numero);
            campos.add(genero);
            campos.add(sufijo);
        } else {
            //tipo.compuesto.tiempo.persona.genero.numero.tiempo2.persona2.genero2.numero2
            campos.add(modo);
            campos.add(tiempo);
            campos.add(persona);
            campos.add(genero);
            campos.add(numero);
            campos.add(tiempoConj);
            campos.add(personaConj);
            campos.add(generoConj);
            campos.add(numeroConj);
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true; 
        String delimiter = "griego".equals(language) ? "-" : ".";
        for (String campo : campos) {
            if (campo != null) { 
                if (!first) {
                    sb.append(delimiter);
                }
                sb.append(campo);
                first = false;
            }
        }

        return sb.toString();
    }
}
