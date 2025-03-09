package com.concordance.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import com.concordance.services.util.BibleUtil;
import com.concordance.services.util.TextUtils;
import com.concordance.services.vo.Verse;

import lombok.Getter;
import lombok.Setter;

@Named("evaluateController")
@ViewScoped
@Getter
@Setter
public class EvaluateController implements Serializable {

    private String input;
    private List<String> issues;

    @PostConstruct
    public void init() {
        
    }

    public void evaluate() {
        try {
            compare(input);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void compare(String input) {
        issues = new ArrayList<>();
		List<String> txts = BibleUtil.extractVerses(input);
		for(String in : txts) {
            try {
                String verso = TextUtils.textBetween(in, "“", "”");
                String cita = TextUtils.textBetween(in, "(", ")", in.lastIndexOf("("));
                List<Verse> verses = BibleUtil.verses(cita);
                if(verses == null) {
                    issues.add("No se pudo extraer versos: " + cita);
                    continue;
                }

                String versoOriginal = verses.isEmpty() ? "No se cuenta con esta traducción" : verses.stream().map(Verse::getText)
                        .collect(Collectors.joining(" "));
                
                if(!TextUtils.phraseEqual(versoOriginal, verso)) {
                    System.out.println(cita);
                    System.out.println();
                    System.out.println(verso);
                    System.out.println(versoOriginal);
                    System.out.println();
                    issues.add("Diferencia " + cita + "\n\n* " + verso + "\n*" + versoOriginal);
                }
            } catch (Exception e) {
                e.printStackTrace();
                issues.add("Linea con problemas: " + in);
            }
		}
	}
}
