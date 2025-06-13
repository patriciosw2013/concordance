package com.concordance.services;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.concordance.constants.Const;
import com.concordance.services.util.BibleUtil;
import com.concordance.services.util.DBUtil;
import com.concordance.services.util.NotesUtil;
import com.concordance.services.util.TextUtils;
import com.concordance.services.util.WebUtil;
import com.concordance.services.vo.AutorVo;
import com.concordance.services.vo.Book;
import com.concordance.services.vo.ContentVo;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.ResultsVo;
import com.concordance.services.vo.bible.CitaVo;

public class ConcordanceService {

    public static List<String> bases() throws SQLException {
        return DBUtil.baseList(true, false, false);
    }

    public static List<String> basesBible() throws SQLException {
        return DBUtil.baseList(false, false, true);
    }

    public static List<String> basesInterlineal() throws SQLException {
        return DBUtil.baseList(false, true, false);
    }

    public static List<RecordVo> concordance(String in, String base, boolean highlight) throws SQLException {
        if (DBUtil.type(base) == Const.TYPE_AUTORS) {
            return AutoresService.concordancia(in, base, highlight);
        } else if (DBUtil.type(base) == Const.TYPE_NOTES) {
            return NotesUtil.concordancia(in, base, highlight);
        } else {
            return BibleUtil.concordancia(in, base, highlight);
        }
    }

    public static ResultsVo readContentsForVerse(int verseId, String base, String key, boolean highlight)
            throws SQLException {
        System.out.println("Leyendo contenido verso " + verseId);
        ContentVo contents = null;
        Book book = new Book();
        String label = null;
        RecordVo vr = null;
        String notes = null;
        List<ItemVo> chapters = null;
        if (DBUtil.type(base) == Const.TYPE_AUTORS) {
            book = AutoresService.bookForVerse(verseId, base);
            vr = AutoresService.verse(verseId, base);
            contents = AutoresService.readContents(book.getId(), vr.getChapterId(), base);
            chapters = AutoresService.chaptersForVerse(book.getId(), base);
            notes = AutoresService.readNotes(book.getId(), vr.getChapterId(), base);
            label = label(book, contents.getChapter());
        } else if (DBUtil.type(base) == Const.TYPE_NOTES) {
            book = NotesUtil.bookForVerse(verseId, base);
            vr = NotesUtil.verse(verseId, base);
            contents = AutoresService.readContents(book.getId(), vr.getChapterId(), base);
            notes = AutoresService.readNotes(book.getId(), vr.getChapterId(), base);
            label = label(book, contents.getChapter());
        } else {
            book = BibleUtil.bookForVerse(verseId, base);
            vr = BibleUtil.verse(verseId, base);
            contents = BibleUtil.readContentsForVerse(verseId, base);
            notes = BibleUtil.readNotes(book.getId(), vr.getChapterId(), base).stream().collect(Collectors.joining("\n"));
            label = book.getName() + " " + contents.getChapter();
        }

        if (highlight) {
            String numRex = DBUtil.type(base) == Const.TYPE_NOTES ? "(\\d+)" : "(^\\d+)";
            String numFormat = "<span style=\"font-weight: bold; color: #007ad9;\">$1</span>";
            String keyFormat = "<b><mark>" + key + "</mark></b>";
            for (int i = 0; i < contents.getContents().size(); i++) {
                contents.getContents().set(i,
                        contents.getContents().get(i).replaceAll(key, keyFormat));
                contents.getContents().set(i,
                        contents.getContents().get(i).replaceAll(numRex, numFormat));
                contents.getContents().set(i,
                        WebUtil.replaceLinks(contents.getContents().get(i)));
            }
        }

        contents.setChapter(label);
        return new ResultsVo(book, contents, notes, chapters);
    }

    public static ResultsVo readContents(int bookId, int chapter, String base) throws SQLException {
        ContentVo contents = null;
        Book book = new Book();
        String label = null;
        String notes = null;
        List<ItemVo> chapters = null;
        if (DBUtil.type(base) == Const.TYPE_AUTORS) {
            book = AutoresService.book(bookId, base);
            chapters = AutoresService.chapters(bookId, base);
            int chapterId = chapters.isEmpty() ? 0 : chapters.get(0).getCodigo();
            contents = AutoresService.readContents(bookId, chapter == 0 ? chapterId : chapter, base);
            notes = AutoresService.readNotes(bookId, chapter == 0 ? chapterId : chapter, base);
            label = label(book, contents.getChapter());
        } else {
            book = BibleUtil.book(bookId, base);
            chapters = BibleUtil.chapters(bookId, base);
            int chapterId = chapters.get(0).getCodigo();
            contents = BibleUtil.readContents(bookId, chapter == 0 ? chapterId : chapter, base);
            notes = BibleUtil.readNotes(bookId, chapterId, base).stream().collect(Collectors.joining("\n"));
            label = book.getName() + " " + contents.getChapter();
        }

        contents.setChapter(label);
        return new ResultsVo(book, contents, notes, chapters);
    }

    public static ResultsVo readContents(CitaVo in, boolean verse, boolean cross, boolean title, boolean highlight) {
        ContentVo contents = null;
        Book b = new Book();
        int bookId = in.getBookId();
        String base = in.getVersion();
        String label = null;
        String notes = null;
        List<ItemVo> chapters = null;
        try {
            b = BibleUtil.book(bookId, base);
            chapters = BibleUtil.chapters(bookId, base);
            contents = BibleUtil.readContents(in, verse, cross, title);
            notes = BibleUtil.readNotes(bookId, in.getChapter(), base).stream().collect(Collectors.joining("\n"));
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        if (highlight) {
            String numRex = "^(\\d+)";
            String numFormat = "<span style=\"font-weight: bold; color: #007ad9;\">$1</span>";
            for (int i = 0; i < contents.getContents().size(); i++) {
                contents.getContents().set(i,
                        contents.getContents().get(i).replaceAll(numRex, numFormat));
            }
        }

        label = b.getName() + " " + contents.getChapter();
        contents.setChapter(label);
        return new ResultsVo(b, contents, notes, chapters);
    }

    public static String label(Book b, String chapter) {
        Set<String> res = new LinkedHashSet<>();
        res.add(TextUtils.value(b.getAutor(), "Anonimo"));
        res.add(b.getParent());
        res.add(b.getName());
        res.add(b.getTitle());
        res.add(chapter);

        Set<String> excep = new HashSet<>();
        excep.add("Sermones");
        excep.add("Cartas");

        return formatLabel(
                res.stream().filter(i -> !TextUtils.isEmpty(i) && !excep.contains(i))
                        .map(i -> i.length() > 60 ? i.substring(0, 60) : i)
                        .collect(Collectors.joining(", ")));
    }

    public static String formatLabel(String label) {
        label = label.replaceAll("SALMO ", "");
        label = label.replaceAll("SERMÓN", "sermón");
        label = label.replaceAll("CARTA", "carta");
        label = label.replaceAll("TRATADO ", "");
        label = label.replaceAll("CATEQUESIS", "");
        label = label.replaceAll("HOMILÍA", "homilía");
        label = label.replaceAll("LIBRO", "Lib.");
        label = label.replaceAll("CAPÍTULO", "Cap.");
        label = label.replaceAll("Capítulo", "Cap.");
        label = label.replaceAll("Libro", "Lib.");

        return label;
    }

    public static List<AutorVo> obras(String base) {
        try {
            if (DBUtil.type(base) == Const.TYPE_AUTORS) {
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
            readContentsForVerse(23146, "RVR1960", "", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
