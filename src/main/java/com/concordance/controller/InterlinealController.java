package com.concordance.controller;

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
import com.concordance.services.vo.Book;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.RecordVo;
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
    private String base;
    private String language;
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
            evaluateParam();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void evaluateParam() throws SQLException {
        FacesContext context = FacesContext.getCurrentInstance();
        String param = context.getExternalContext().getRequestParameterMap().get("in");
        if(param != null) {
            System.out.println("Cargando itl verseid: " + param);
            int id = Integer.parseInt(param);
            RecordVo r = BibleUtil.verse(id, base);
            Book b = BibleUtil.book(r.getBookId(), base);
            input = b.getName() + " " + r.getChapterId() + ":" + r.getVerse();
            evaluate();
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
        language = words.get(0).getLanguage();
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

    public void loadReferences(int wordId) {
        try {
            System.out.println("Cargando referencias strongId: " + wordId);
            references = InterlinealService.strongReference(wordId, base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadReferencesForWord(String word) {
        try {
            System.out.println("Cargando referencias palabra: " + word);
            references = InterlinealService.strongReference(word, base);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadStrong(int wordId) {
        try {
            System.out.println("Cargando strong strongId: " + wordId);
            strong = InterlinealService.strongDetail(wordId);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadStrongParam() {
        try {
            int strongId = Integer.parseInt(
                    FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("num"));
            String language = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("lg");
            strong = InterlinealService.strongDetail(strongId, language);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void loadNotationDetail(String code) {
        try {
            System.out.println("Cargando morfologia: " + code);
            notationDetail = InterlinealService.notationDetail(code, language);
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
            strongs = InterlinealService.findStrong(strongInput);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void readParam(String id) {
        System.out.println(id);
        input = id;
    }
}
