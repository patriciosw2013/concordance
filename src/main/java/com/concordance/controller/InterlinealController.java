package com.concordance.controller;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import com.concordance.services.InterlinealService;
import com.concordance.services.util.BibleUtil;
import com.concordance.services.vo.ItemStringVo;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.bible.CitaVo;
import com.concordance.services.vo.interlineal.InterlinealVo;
import com.concordance.services.vo.interlineal.NotationDetailVo;
import com.concordance.services.vo.interlineal.StrongDetailVo;

import lombok.Getter;
import lombok.Setter;

@Named("interlinealController")
@ViewScoped
@Getter
@Setter
public class InterlinealController implements Serializable {

    private String input;
    private String reference;
    private StrongDetailVo strong;
    private CitaVo cita;
    private int testamentId;
    private String base;
    private List<ItemVo> chapters;
    private List<InterlinealVo> words;
    private List<ItemStringVo> references;
    private NotationDetailVo notationDetail;

    @PostConstruct
    public void init() {
        base = "RVR1960";
    }

    public void evaluate() {
        try {
            cita = BibleUtil.extractRef(input);
            if(cita == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cita no existe"));
                return;
            }

            testamentId = BibleUtil.testamentId(cita.getBookId(), base);
            chapters = BibleUtil.chapters(cita.getBookId(), base);
            search();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void search() throws SQLException {
        reference = BibleUtil.verses(cita.cita()).get(0).getText();
        words = InterlinealService.interlineal(cita.getBookId(), cita.getChapter(), cita.getVerseIni());
        input = null;
    }

    public void navigate(int direction) {
        try {
            if(direction == 0) {
                if(cita.getVerseIni() > 1)
                    cita.setVerseIni(cita.getVerseIni() - 1);
                else return;
            } else {
                if(cita.getVerseIni() < chapters.get(chapters.size() - 1).getCodigo())
                    cita.setVerseIni(cita.getVerseIni() + 1);
                else return;
            }
            search();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
	}

    public void loadReferences(int strongId) {
        try {
            System.out.println("Cargando referencias strongId: " + strongId);
            references = InterlinealService.strongReference(strongId, testamentId, base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
	}

    public void loadStrong(int strongId) {
        try {
            System.out.println("Cargando strong strongId: " + strongId);
            strong = InterlinealService.strongDetail(strongId, testamentId == 1 ? "hebreo" : "griego");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
	}

    public void loadStrongParam() {
        try {
            int strongId = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("param1"));
            System.out.println("Cargando strong strongId: " + strongId);
            strong = InterlinealService.strongDetail(strongId, testamentId == 1 ? "hebreo" : "griego");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
	}

    public void loadNotationDetail(String code) {
        try {
            System.out.println("Cargando morfologia: " + code);
            notationDetail = InterlinealService.notationDetail(code, testamentId);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
	}
}
