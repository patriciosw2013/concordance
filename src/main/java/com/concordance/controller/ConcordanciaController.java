package com.concordance.controller;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
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

    @Inject
    private InterlinealController interlinealController;

    @PostConstruct
    public void init() {
        try {
            bases = ConcordanceService.bases();
            base = bases.get(0);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
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
            System.out.println("Consultando verseId: " + verseId);
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

    public void navigateItl() {
        interlinealController.readParam(verseId);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        NavigationHandler nav = facesContext.getApplication().getNavigationHandler();
        nav.handleNavigation(facesContext, null, "interlineal.xhtml");
    }
}
