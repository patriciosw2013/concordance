package com.concordance.controller;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import com.concordance.services.ConcordanceService;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.ResultsVo;

import lombok.Getter;
import lombok.Setter;

@Named("concordanciaController")
@ViewScoped
@Getter
@Setter
public class ConcordanciaController implements Serializable {

    private List<String> bases;
    private List<RecordVo> verses;

    private String title;
    private String base;
    private String input;
    private String txt;
    private String notes;
    private String verseId;

    @PostConstruct
    public void init() {
        bases = ConcordanceService.bases();
        base = bases.get(0);
    }

    public void search() {
        try {
            verses = ConcordanceService.concordance(input, base, true);
            if (verses.isEmpty())
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "Sin resultados"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void onSelect() {
        try {
            ResultsVo res = ConcordanceService.readContentsForVerse(Integer.parseInt(verseId), base, input, true);
            title = res.getResults().getChapter();
            txt = res.getResults().getContents().stream().collect(Collectors.joining("\n"));
            notes = res.getNotes();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }
}
