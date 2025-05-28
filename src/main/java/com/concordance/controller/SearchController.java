package com.concordance.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private CitaVo[] citas;
    private List<String> ops;
    private List<String> selectedOps;

    @PostConstruct
    public void init() {
        try {
            ops = new ArrayList<>();
            ops.add("Versos");
            ops.add("Títulos");
            ops.add("Referencias");
            selectedOps = new ArrayList<>(ops);
            bases = ConcordanceService.basesBible();
            citas = new CitaVo[2];
            citas[0] = new CitaVo(bases.get(0));
            citas[1] = new CitaVo(bases.get(1));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void search() {
        try {
            for(int i = 0; i < citas.length; i++) {
                citas[i] = BibleUtil.cita(input.concat(" ").concat(citas[i].getVersion()));
                loadContents(i);
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void navigate(int citaId, int direction) {
        try {
            CitaVo ct = citas[citaId];
            if (direction == 0) {
                if (ct.getChapter() > 1)
                    ct.setChapter(ct.getChapter() - 1);
                else
                    return;
            } else {
                if (ct.getChapter() < ct.getChapters().get(ct.getChapters().size() - 1).getCodigo())
                    ct.setChapter(ct.getChapter() + 1);
                else
                    return;
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
            CitaVo ct = citas[citaId];
            if (TextUtils.isEmpty(ct.getBook())) {
                return;
            }
            System.out.println("Cargando contenido de " + input + " " + ct.getVersion());
            ct.setTitle(null);
            ct.setTxt(null);
            ct.setNotes(null);
            ct.setChapters(null);
            ct.setBookId(BibleUtil.bookId(ct.getBook(), ct.getVersion()));
            //input = ct.citaSimple();

            if (ct.getBookId() == 0)
                return;
            Set<String> op = new HashSet<>(selectedOps);
            ResultsVo res = ConcordanceService.readContents(ct, op.contains("Versos"), op.contains("Referencias"), 
                op.contains("Títulos"), true);
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
