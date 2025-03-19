package com.concordance.controller;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import com.concordance.exceptions.GenericException;
import com.concordance.services.ConcordanceService;
import com.concordance.services.InterlinealService;
import com.concordance.services.util.BibleUtil;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.Verse;
import com.concordance.services.vo.bible.CitaVo;
import com.concordance.services.vo.interlineal.InterlinealVo;
import com.concordance.services.vo.interlineal.NotationDetailVo;
import com.concordance.services.vo.interlineal.StrongDetailVo;
import com.concordance.services.vo.interlineal.StrongFindVo;
import com.concordance.services.vo.interlineal.StrongReferenceVo;

import lombok.Getter;
import lombok.Setter;

@Named("interlinealController")
@ViewScoped
@Getter
@Setter
public class InterlinealController implements Serializable {

    private String input;
    private String reference;
    private String simpleInterlineal;
    private StrongDetailVo strong;
    private CitaVo cita;
    private int testamentId;
    private String base;
    private String strongInput;
    private List<String> bases;
    private List<ItemVo> verses;
    private List<StrongFindVo> strongs;
    private List<InterlinealVo> words;
    private List<StrongReferenceVo> references;
    private NotationDetailVo notationDetail;

    @PostConstruct
    public void init() {
        try {
            bases = ConcordanceService.basesInterlineal();
            base = bases.get(0);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void evaluate() {
        try {
            cita = BibleUtil.cita(input.concat(" ").concat(base));
            if (cita == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cita no existe"));
                return;
            }

            System.out.println(cita.cita());
            testamentId = BibleUtil.testamentId(cita.getBookId(), cita.getVersion());
            verses = BibleUtil.verses(cita.getBookId(), cita.getChapter(), cita.getVersion());
            search();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void search() throws SQLException {
        List<Verse> vs = BibleUtil.verses(cita.cita());
        if (vs.isEmpty())
            throw new GenericException("No existen datos para la cita");
            
        reference = vs.get(0).getText();
        words = InterlinealService.interlineal(cita.getBookId(), cita.getChapter(), cita.getVerseIni(), base);
        input = null;
    }

    public void navigate(int direction) {
        try {
            if (direction == 0) {
                if (cita.getVerseIni() > 1)
                    cita.setVerseIni(cita.getVerseIni() - 1);
                else
                    return;
            } else {
                if (cita.getVerseIni() < verses.get(verses.size() - 1).getCodigo())
                    cita.setVerseIni(cita.getVerseIni() + 1);
                else
                    return;
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
            references = InterlinealService.strongReference(strongId, testamentId, "RVR1960", base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadReferences(int strongId, int testamentId) {
        try {
            System.out.println("Cargando referencias strongId: " + strongId);
            references = InterlinealService.strongReference(strongId, testamentId, "RVR1960", base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadReferences(String word) {
        try {
            System.out.println("Cargando referencias palabra: " + word);
            references = InterlinealService.strongReference(word, base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadStrong(int strongId) {
        try {
            loadStrongDet(strongId);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadStrongParam() {
        try {
            int strongId = Integer.parseInt(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("param1"));
            loadStrongDet(strongId);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void loadStrongDet(int strongId) throws SQLException, IOException {
        System.out.println("Cargando strong strongId: " + strongId);
        strong = InterlinealService.strongDetail(strongId, testamentId, base);
    }

    public void loadNotationDetail(String code) {
        try {
            System.out.println("Cargando morfologia: " + code);
            notationDetail = InterlinealService.notationDetail(code, testamentId, base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadSimpleInterlineal(int verseId) {
        try {
            simpleInterlineal = InterlinealService.simpleInterlineal(verseId, base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void findStrong() {
        try {
            strongs = InterlinealService.findStrong(strongInput, base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }
}
