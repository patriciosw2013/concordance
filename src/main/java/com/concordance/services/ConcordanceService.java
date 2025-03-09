package com.concordance.services;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.concordance.services.util.AutoresUtil;
import com.concordance.services.util.BibleUtil;
import com.concordance.services.util.NotesUtil;
import com.concordance.services.util.PatristicaUtil;
import com.concordance.services.util.TextUtils;
import com.concordance.services.vo.AutorVo;
import com.concordance.services.vo.Book;
import com.concordance.services.vo.ContentVo;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.ResultsVo;
import com.concordance.services.vo.ItemVo;

public class ConcordanceService {

    public static List<String> bases() {
        List<String> vr = new ArrayList<>();
        vr.add("RVR1960");
		vr.add("NTV");
		vr.add("NVI");
		vr.add("TLA");
        vr.add("DHH");
		vr.add("Patristica");
        vr.add("Autores");
        vr.add("Notas");

        return vr;
    }

    public static List<RecordVo> concordance(String in, String base, boolean highlight) {
        try {
            if("Patristica".equals(base)) {
                return PatristicaUtil.concordancia(in, base, highlight);
            } else if("Autores".equals(base)) {
                return AutoresUtil.concordancia(in, base, highlight);
            } else if("Notas".equals(base)) { 
                return NotesUtil.concordancia(in, base, highlight);
            } else {
                return BibleUtil.concordancia(in, base, highlight);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        return new ArrayList<>();
    }

    public static ResultsVo readContentsForVerse(int verseId, String base, String key, boolean highlight) {
        ContentVo contents = null;
        Book b = new Book();
        String label = null;
        RecordVo r = null;
        String notes = null;
        List<ItemVo> chapters = null;
        if("Patristica".equals(base) || "Autores".equals(base)) {
            try {
                b = AutoresService.bookForVerse(verseId, base);
                r = AutoresService.verse(verseId, base);
                contents = AutoresService.readContents(b.getId(), r.getChapterId(), base);
                chapters = AutoresService.chaptersForVerse(b.getId(), base);
                notes = AutoresService.readNotes(b.getId(), base, r.getChapter());
                label = formatLabel(TextUtils.value(b.getAutor(), "Anonimo") + ", " + b.getParent() + " " + b.getName() + ": " + 
                    (TextUtils.isEmpty(b.getTitle()) ? " " : b.getTitle().length() < 60 ? b.getTitle().concat(" ") : " ") + 
                    r.getChapter());  
            } catch (SQLException | IOException e1) {
                e1.printStackTrace();
            }
        } else if("Notas".equals(base)) {
            try {
                b = NotesUtil.book(verseId, base);
                contents = AutoresUtil.readContents(b.getId(), 0, base);
                label = b.getAutor() + ", " + b.getParent() + " " + b.getName() + ": " + 
                    (TextUtils.isEmpty(b.getTitle()) ? " " : b.getTitle().length() < 60 ? b.getTitle().concat(" ") : " ");
            } catch (SQLException | IOException e1) {
                e1.printStackTrace();
            }
        } else {
            try {
                b = BibleUtil.bookForVerse(verseId, base);
                contents = BibleUtil.readContentsForVerse(verseId, base);
                label = b.getName() + ": " + contents.getChapter();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        if(highlight) {
            for (int i = 0; i < contents.getContents().size(); i++) {
                contents.getContents().set(i, contents.getContents().get(i).replaceAll(key, "<b><mark>"+key+"</mark></b>"));
            }
        }

        contents.setChapter(label);
        return new ResultsVo(b, contents, notes, chapters);
    }

    public static ResultsVo readContents(int bookId, int chapter, String base) {
        ContentVo contents = null;
        Book b = new Book();
        String label = null;
        String notes = null;
        List<ItemVo> chapters = null;
        if("Patristica".equals(base) || "Autores".equals(base)) {
            try {
                b = AutoresService.book(bookId, base);
                chapters = AutoresService.chapters(bookId, base);
                int chapterId = chapters.isEmpty() ? 0 : chapters.get(0).getCodigo();
                contents = AutoresService.readContents(bookId, chapter == 0 ? chapterId : chapter, base);
                notes = AutoresService.readNotes(bookId, base, null);
                label = TextUtils.value(b.getAutor(), "Anonimo") + ", " + b.getParent() + " " + b.getName() + ": " + 
                    (TextUtils.isEmpty(b.getTitle()) ? " " : b.getTitle().length() < 110 ? b.getTitle().concat(" ") : " ") +
                    TextUtils.value(contents.getChapter(), "");
                label = formatLabel(label);
            } catch (SQLException | IOException e1) {
                e1.printStackTrace();
            }
        } else {
            try {
                b = BibleUtil.book(bookId, base);
                chapters = BibleUtil.chapters(bookId, base);
                int chapterId = chapters.get(0).getCodigo();
                contents = BibleUtil.readContents(bookId, chapter == 0 ? chapterId : chapter, base);
                label = b.getName() + ": " + contents.getChapter();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        contents.setChapter(label);
        return new ResultsVo(b, contents, notes, chapters);
    }

    public static String formatLabel(String label) {
        label = label.replaceAll("SALMO ", "");
        label = label.replaceAll("SERMÓN", "sermón");
        label = label.replaceAll("CARTA", "carta");
        label = label.replaceAll("TRATADO ", "");
        label = label.replaceAll("CATEQUESIS", "catequesis");
        label = label.replaceAll("HOMILÍA", "homilía");
        label = label.replaceAll("LIBRO", "Lib.");
        label = label.replaceAll("CAPÍTULO", "Cap.");

        return label;
    }

    public static List<AutorVo> obras(String base) {
        try {
            if("Patristica".equals(base) || "Autores".equals(base)) {
                return AutoresService.books(base);
            } else {
                return BibleUtil.books(base);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        return new ArrayList<>();
    }

    public static void main(String[] args) {
        try {
            List<AutorVo> obras = obras("Patristica");
            for (AutorVo o : obras) {
                System.out.println(o.getAutor());
                for(Entry<String, List<ItemVo>> x: o.getBooks().entrySet()) {
                    System.out.println(x.getKey() + ": " + x.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
