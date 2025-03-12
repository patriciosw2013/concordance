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
import com.concordance.services.util.BibleUtil;
import com.concordance.services.util.TextUtils;
import com.concordance.services.vo.ResultsVo;
import com.concordance.services.vo.bible.CitaVo;

import lombok.Getter;
import lombok.Setter;

@Named("searchController")
@ViewScoped
@Getter
@Setter
public class SearchController implements Serializable {

    private List<String> bases;
    private String input;
    private CitaVo cita;
    private CitaVo citaPar;

    @PostConstruct
    public void init() {
        bases = ConcordanceService.basesBible();
        cita = new CitaVo(bases.get(0));
        citaPar = new CitaVo(bases.get(1));
    }

    public void search() {
        try {
            cita = BibleUtil.cita(input.concat(" ").concat(cita.getVersion()));
            citaPar = BibleUtil.cita(input.concat(" ").concat(citaPar.getVersion()));
            loadContents(1);
            loadContents(2);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void navigate(int citaId, int direction) {
        try {
            CitaVo ct = citaId == 1 ? cita : citaPar;
            if(direction == 0) {
                if(ct.getChapter() > 1)
                    ct.setChapter(ct.getChapter() - 1);
                else return;
            } else {
                if(ct.getChapter() < ct.getChapters().get(ct.getChapters().size() - 1).getCodigo())
                    ct.setChapter(ct.getChapter() + 1);
                else return;
            }
            loadContents(citaId);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
	}

    public void loadContents(int citaId) {
        try {
            CitaVo ct = citaId == 1 ? cita : citaPar;
            if(TextUtils.isEmpty(ct.getBook())) {
                return;
            }
            System.out.println("Cargando contenido de " + ct.cita());
            ct.setTitle(null);
            ct.setTxt(null);
            ct.setNotes(null);
            ct.setChapters(null);
            ct.setBookId(BibleUtil.bookId(ct.getBook(), ct.getVersion()));
            input = ct.citaSimple();

            if(ct.getBookId() == 0) return;
            ResultsVo res = ConcordanceService.readContents(ct);
            ct.setTitle(res.getResults().getChapter());
            ct.setTxt(res.getResults().getContents().stream().collect(Collectors.joining("\n")));
            ct.setChapters(res.getChapters());
            ct.setNotes(res.getNotes());
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }
}
