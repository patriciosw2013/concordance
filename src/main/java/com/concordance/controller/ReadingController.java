package com.concordance.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

import com.concordance.services.ConcordanceService;
import com.concordance.services.vo.AutorVo;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.ResultsVo;

import lombok.Getter;
import lombok.Setter;

@Named("readingController")
@ViewScoped
@Getter
@Setter
public class ReadingController implements Serializable {

    private List<String> bases;
    private List<ItemVo> chapters;
    private TreeNode<ItemVo> books;

    private String title;
    private String base;
    private String txt;
    private String notes;
    private String chapter;
    private TreeNode<ItemVo> book;

    @PostConstruct
    public void init() {
        try {
            bases = ConcordanceService.bases();
            base = bases.get(0);
            search();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void clear() {
        System.out.println("Limpiando consulta");
        title = null;
        txt = null;
        chapters = null;
        notes = null;
        chapter = null;
    }

    public void search() {
        try {
            clear();
            List<AutorVo> res = ConcordanceService.obras(base);
            if (res.isEmpty())
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Warning", "Sin resultados"));

            books = new DefaultTreeNode<ItemVo>(new ItemVo(0, ""), null);
            for (AutorVo o : res) {
                TreeNode<ItemVo> author = new DefaultTreeNode<ItemVo>(new ItemVo(0, o.getAutor()), books);
                for(Entry<String, List<ItemVo>> x: o.getBooks().entrySet()) {
                    if(x.getValue().size() == 1) {
                        new DefaultTreeNode<ItemVo>(x.getValue().get(0), author);
                    } else {
                        TreeNode<ItemVo> parent = new DefaultTreeNode<ItemVo>(new ItemVo(0, x.getKey()), author);
                        for(ItemVo z : x.getValue()) {
                            new DefaultTreeNode<ItemVo>(z, parent);
                        }
                    }
                }
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void onSelect() {
        try {
            loadContents(0);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void viewChapter() {
        try {
            loadContents(Integer.parseInt(chapter));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            e.printStackTrace();
        }
    }

    private void loadContents(int chapter) throws SQLException {
        if(book.getData().getCodigo() == 0) return;
        ResultsVo res = ConcordanceService.readContents(book.getData().getCodigo(), chapter, base);
        title = res.getResults().getChapter().getName();
        txt = res.getResults().getContents().stream().collect(Collectors.joining("\n"));
        chapters = res.getChapters();
        System.out.println("txt: " + txt.length());
        notes = res.getNotes();
    }

    public void exportarPDF() throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        contentStream.setFont(PDType1Font.HELVETICA, 9);
        contentStream.beginText();
        contentStream.newLineAtOffset(30, 40);

        String[] lineas = txt.split("\n");
        for (String linea : lineas) {
            contentStream.showText(linea);
            contentStream.newLineAtOffset(0, -15);
        }

        contentStream.endText();
        contentStream.close();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        document.save(byteArrayOutputStream);
        document.close();

        HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment;filename=exported_text.pdf");
        response.getOutputStream().write(byteArrayOutputStream.toByteArray());
        response.getOutputStream().flush();
        FacesContext.getCurrentInstance().responseComplete();
    }
}
