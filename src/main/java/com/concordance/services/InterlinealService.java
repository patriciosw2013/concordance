package com.concordance.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.concordance.services.util.BibleUtil;
import com.concordance.services.util.FileUtils;
import com.concordance.services.util.ListUtils;
import com.concordance.services.util.SQLUtil;
import com.concordance.services.util.TextUtils;
import com.concordance.services.util.WebUtil;
import com.concordance.services.vo.ChapterVo;
import com.concordance.services.vo.ItemStringVo;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.bible.CitaVo;
import com.concordance.services.vo.interlineal.InterlinealVo;
import com.concordance.services.vo.interlineal.NotationDetailVo;
import com.concordance.services.vo.interlineal.NotationVo;
import com.concordance.services.vo.interlineal.StrongDetailVo;
import com.concordance.services.vo.interlineal.StrongFindVo;
import com.concordance.services.vo.interlineal.StrongReferenceVo;
import com.concordance.services.vo.interlineal.StrongVo;

public class InterlinealService {

    private static String base = "Interlineal";
    private static SQLUtil db = SQLUtil.getInstance();

    public static List<InterlinealVo> interlineal(int book, int chapter, int verse, String version) throws SQLException {
        List<InterlinealVo> res = new ArrayList<>();
        ResultSet result = null;
        try (Connection conn = db.connection("Interlineal")) {
            String sql = "select i.chapter, i.verse, st.strong_id, i.word, i.type, i.meaning, i.book_id, coalesce(st.id, 0), coalesce(st.language, '') " +
                "from interlineal i left join strong st on (st.id = i.word_id) inner join version vr on (vr.id = i.version) " + 
                "where i.book_id = ? and i.chapter = ? and i.verse = ? and vr.name = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, book);
                st.setInt(2, chapter);
                st.setInt(3, verse);
                st.setString(4, version);
                result = st.executeQuery();
                while(result.next()) {
                    InterlinealVo b = new InterlinealVo();
                    b.setChapter(result.getInt(1));
                    b.setVerse(result.getInt(2));
                    b.setStrongId(result.getInt(3));
                    b.setWord(result.getString(4));
                    b.setType(result.getString(5));
                    b.setMeaning(result.getString(6));
                    b.setBookId(result.getInt(7));
                    b.setWordId(result.getInt(8));
                    b.setLanguage(result.getString(9));
                    b.setMorfologic("hebreo".equals(b.getLanguage()) ? simpleDetailNot(b.getType()) : 
                        notation(b.getType(), b.getLanguage()).summary());

                    res.add(b);
                }
            }
        }
        
        return res;
    }

    public static String simpleInterlineal(int verseId, String version) throws SQLException {
        RecordVo c = BibleUtil.verse(verseId, version);
        ResultSet result = null;
        List<ItemStringVo> res = new ArrayList<>();
        try (Connection conn = db.connection("Interlineal")) {
            String sql = "select i.word, i.meaning from interlineal i inner join version v on (v.id = i.version) " +
            "where i.book_id = ? and i.chapter = ? and i.verse = ? and v.name = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, c.getBookId());
                st.setInt(2, c.getChapterId());
                st.setInt(3, c.getVerse());
                st.setString(4, version);
                result = st.executeQuery();
                while(result.next()) {
                    res.add(new ItemStringVo(result.getString(1), result.getString(2)));
                }
            }
        }

        return res.stream()
            .map(i -> String.format("<span style=\"font-weight: bold; color: #007ad9;\">%s</span> <span style=\"color: #d9534f;\">(%s)</span>", i.getCodigo(), i.getValor()))
            .collect(Collectors.joining(" "));
    }

    public static void createInterlineal(List<InterlinealVo> process, int version) throws SQLException {
        String sql = "insert into interlineal (id, book_id, chapter, verse, strong_id, word, type, meaning, version) values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.connection(base)) {
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                int z = max("interlineal", base) + 1;
                int i = 0;
                for (InterlinealVo o : process) {
                    st.setInt(1, z++);
                    st.setInt(2, o.getBookId());
                    st.setInt(3, o.getChapter());
                    st.setInt(4, o.getVerse());
                    st.setInt(5, o.getStrongId());
                    st.setString(6, o.getWord());
                    st.setString(7, o.getType());
                    st.setString(8, o.getMeaning());
                    st.setInt(9, version);
                    
                    st.addBatch();
                    if(i%5000 == 0)
                        st.executeBatch();
                    
                    i++;
                }
                st.executeBatch();
                conn.commit();
            }
        }
    }

    public static void createStrong(List<StrongVo> process) throws SQLException {
        String sql = "insert into strong (id, strong_id, language, word, def, def_alterna, type, deriva, def_rv, def_global) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.connection(base)) {
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                int z = max("strong", base) + 1;
                int i = 0;
                for (StrongVo o : process) {
                    st.setInt(1, z++);
                    st.setInt(2, o.getStrongId());
                    st.setString(3, o.getLanguage());
                    st.setString(4, o.getWord());
                    st.setString(5, o.getDef());
                    st.setString(6, o.getDefAlterna());
                    st.setString(7, o.getType());
                    st.setString(8, o.getDeriva());
                    st.setString(9, o.getDefRV());
                    st.setString(10, o.getDefGlobal());
                    
                    st.addBatch();
                    if(i%5000 == 0)
                        st.executeBatch();
                    
                    i++;
                }
                st.executeBatch();
                conn.commit();
            }
        }
    }

    public static List<String> notations() throws SQLException {
        List<String> refs = new ArrayList<>();
        ResultSet result = null;
        try (Connection conn = db.connection(base)) {
            String sql = "select distinct type from interlineal a inner join book b on (a.book_id = b.id and b.testament_id = 1) order by type";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                result = st.executeQuery();
                while(result.next()) {
                    refs.add(result.getString(1));
                }
            }
        }
        
        return refs;
    }

    public static List<StrongReferenceVo> strongReference(int wordId, String baseVerse, String version) throws SQLException {
        if(wordId == 0)
            return new ArrayList<>();

        List<StrongReferenceVo> res = new ArrayList<>();
        ResultSet result = null;
        try (Connection conn = db.connection(base)) {
            String sql = String.format("attach database '%s' as db2", SQLUtil.bases.get(baseVerse));
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.executeUpdate();
            }

            sql = "select i.chapter, i.verse, st.strong_id, i.word, i.type, i.meaning, i.book_id, " +
                "trim(v.text) || ' ' || '(' || a.name || ' ' || v.chapter || ':' || v.verse || ' ' || ? || ')', v.id " +
                "from interlineal i inner join strong st on (st.id = i.word_id) inner join version vr on (vr.id = i.version) "+
                "inner join db2.book a on (i.book_id = a.id) " +
                "inner join db2.verse v on (v.book_id = a.id and v.chapter = i.chapter and v.verse = i.verse) " +
                "where i.word_Id = ? and vr.name = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, baseVerse);
                st.setInt(2, wordId);
                st.setString(3, version);
                result = st.executeQuery();
                while (result.next()) {
                    StrongReferenceVo b = new StrongReferenceVo();
                    b.setVerseId(result.getInt(9));
                    b.setStrongId(result.getInt(3));
                    b.setWord(result.getString(4));
                    b.setType(result.getString(5));
                    b.setMeaning(result.getString(6));
                    b.setReference(result.getString(8));
                    res.add(b);
                }
            }
        }
        return res;
    }

    public static List<StrongReferenceVo> strongReference(String word, String base) throws SQLException {
        List<StrongReferenceVo> res = new ArrayList<>();
        ResultSet result = null;
        try (Connection conn = db.connection(base)) {
            String sql = "select v.chapter, v.verse, 0, ?, '', '', v.book_id, " +
                "trim(v.text) || ' ' || '(' || a.name || ' ' || v.chapter || ':' || v.verse || ' ' || ? || ')', v.id " +
                "from book a inner join verse v on (v.book_id = a.id) " +
                "where v.text like '%' || ? || '%'";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, word);
                st.setString(2, base);
                st.setString(3, word);
                result = st.executeQuery();
                while (result.next()) {
                    StrongReferenceVo b = new StrongReferenceVo();
                    b.setVerseId(result.getInt(9));
                    b.setStrongId(result.getInt(3));
                    b.setWord(result.getString(4));
                    b.setType(result.getString(5));
                    b.setMeaning(result.getString(6));
                    b.setReference(result.getString(8));
                    res.add(b);
                }
            }
        }
        return res;
    }

    public static StrongDetailVo strongDetail(int wordId) throws SQLException {
        ResultSet result = null;
        try (Connection conn = db.connection(base)) {
            String sql = "select i.language, i.def, i.def_alterna, i.type, i.deriva, i.def_rv, i.word, i.def_global, i.strong_id, i.id " +
                "from strong i where i.id = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, wordId);
                result = st.executeQuery();
                if(result.next()) {
                    StrongDetailVo o = new StrongDetailVo();
                    o.setWordId(result.getInt(10));
                    o.setStrongId(result.getInt(9));
                    o.setLanguage(result.getString(1));
                    o.setDefGlobal(result.getString(8));
                    o.setDetails(Arrays.asList(new String[]{"Palabra original:", result.getString(7)},
                        new String[]{"Definición:", result.getString(2)},
                        new String[]{"Definición2:", result.getString(3)},
                        new String[]{"Tipo:", result.getString(4)},
                        new String[]{"Derivacion:", formatDeriva(result.getString(5), o.getLanguage())},
                        new String[]{"Def. en RV1909", result.getString(6)}));

                    return o;
                } else return null;
            }
        }
    }

    public static StrongDetailVo strongDetail(int strongId, String language) throws SQLException {
        int wordId = wordId(strongId, language);
        return strongDetail(wordId);
    }

    public static int wordId(int strongId, String language) throws SQLException {
        ResultSet result = null;
        try (Connection conn = db.connection(base)) {
            String sql = "select o.id from strong o where o.strong_id = ? and o.language = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, strongId);
                st.setString(2, language);
                result = st.executeQuery();
                if(result.next()) {
                    return result.getInt(1);
                }
            }
        }
        return 0;
    }

    public static List<StrongFindVo> findStrong(String in) throws SQLException {
        if(TextUtils.isEmpty(in))
            return new ArrayList<>();

        ResultSet result = null;
        List<StrongFindVo> res = new ArrayList<>();
        try (Connection conn = db.connection(base)) {
            String sql = "select o.strong_id, o.word || ' (' || o.language || ')', o.def_global, o.id from strong o " +
                "where (o.word like ? || '%' or o.definition like ? || '%')";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, in);
                st.setString(2, in);
                result = st.executeQuery();
                while(result.next()) {
                    res.add(new StrongFindVo(result.getInt(1), result.getString(2), 
                    result.getString(3), result.getInt(4)));
                }
            }
        }
        return res;
    }

    private static String formatDeriva(String in, String language) {
        if(in == null) return null;

        if(in.contains("hebreo")) language = "hebreo";

        return in.replaceAll("(\\d+)", 
            "<a href=\"#\" onclick=\"ldStrong([{name: 'num', value: this.textContent}, {name: 'lg', value: '" + language + "'}]);\">$1</a>");
    }

    @Deprecated
    public static void loadInterlinealKL() throws SQLException, IOException {
        String urlgl = "https://www.logosklogos.com/interlinear/%s/%s/%s/%s";
        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", StandardCharsets.UTF_8.name())) {
            List<InterlinealVo> res = new ArrayList<>();
            System.out.println("Cargando desde web...");
            for(int testamentId : new int[]{1}) {
                String prefix = testamentId == 1 ? "AT" : "NT";
                BibleUtil.booksList(testamentId, base).stream().forEach(b -> {         
                    System.out.println("Leyendo libro: " + b.getValor() + " id: " + b.getCodigo());
                    int num = "Job".equals(b.getValor()) ? 3 : 2;
                    try {
                        BibleUtil.chaptersDetail(b.getCodigo(), base).forEach(o -> {
                            System.out.println("Leyendo capitulo: " + o.getChapterId());
                            o.getVerses().stream().forEach(v -> {
                                String url = String.format(urlgl, prefix, b.getDescripcion(), o.getChapterId(), v); 
                                String vr = String.format(" %s %s:%s", b.getValor(), o.getChapterId(), v);
                                String txt = null;
                                try {
                                    txt = WebUtil.readURL(url, StandardCharsets.UTF_8.name());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                for(int z = 1; z <= num; z++)
                                    txt = txt.substring(txt.indexOf(vr) + vr.length());
                                txt = txt.substring(0, txt.indexOf(vr));
                                res.addAll(procesarTexto(b.getCodigo(), o.getChapterId(), v, txt));
                            });
                        });
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            }

            createInterlineal(res, 1);
            for (InterlinealVo x : res) {
                writer.println(x.getBookId() + ">>" + x.getChapter() + ">>" + x.getVerse() + ">>" + 
                    x.getStrongId() + ">>" + x.getWord() + ">>" + x.getType() + ">>"+ x.getMeaning());
            }

            System.out.println("Interlineal creado con exito");
        }
    }

    public static void interlinealKL(boolean existHTML) throws SQLException, IOException {
        String fileHTML = "D:\\Desarrollo\\interlinealKL.txt";
        if (!existHTML) {
            String urlgl = "https://www.logosklogos.com/interlinear/%s/%s/%s/%s";
            try (PrintWriter writer = new PrintWriter(fileHTML, StandardCharsets.UTF_8.name())) {
                System.out.println("Cargando desde web...");
                for (int testamentId : new int[] { 1, 2 }) {
                    String prefix = testamentId == 1 ? "AT" : "NT";
                    for (ItemVo b : BibleUtil.booksList(testamentId, base)) {
                        System.out.println("Leyendo libro: " + b.getValor() + " id: " + b.getCodigo());
                        for (ChapterVo o : BibleUtil.chaptersDetail(b.getCodigo(), base)) {
                            System.out.println("Leyendo capitulo: " + o.getChapterId());
                            for (Integer v : o.getVerses()) {
                                String url = String.format(urlgl, prefix, b.getDescripcion(), o.getChapterId(), v);
                                String txt = WebUtil.readWeb(url, "table.table-condensed", false);
                                writer.println(">>>>>>");
                                writer.println(b.getCodigo() + " " + o.getChapterId() + " " + v);
                                writer.println(txt);
                            }
                        }
                    }
                }
            }
        }

        try (PrintWriter writer = new PrintWriter("D:\\Desarrollo\\versPrev.txt", "UTF-8")) {
            List<InterlinealVo> rds = new ArrayList<>();
            for (List<String> res : ListUtils
                    .split(Files.readAllLines(new File(fileHTML).toPath(), StandardCharsets.UTF_8), ">>>>>>")) {
                String[] codes = res.get(0).split(" ");
                int bookId = Integer.parseInt(codes[0]);
                int chp = Integer.parseInt(codes[1]);
                int vr = Integer.parseInt(codes[2]);
                String classWord = bookId < 40 ? "span.gw.cursor-pointer.hebrew-ezra-bold.interl.text-success"
                        : "span.gw.cursor-pointer.greek-bold.interl.text-success";
                String classMean = bookId < 40 ? "span.text-danger.translation-hebrew" : "span.text-danger";
                String classMorfo = bookId < 40 ? "span.gw.parsing.cursor-pointer.text-warning"
                        : "span.parsing.cursor-pointer.text-warning";

                String txt = res.subList(1, res.size()).stream().collect(Collectors.joining(" "));
                Elements els = WebUtil.readNodeTags(txt, "td");
                for (Element el : els) {
                    String morfo = null, word = null, meaning = null, morfoDet = null;
                    int strong = 0;
                    Elements ins = WebUtil.readNodeTags(el.html(), classWord);
                    if (!ins.isEmpty())
                        word = ins.text();

                    ins = WebUtil.readNodeTags(el.html(), classMorfo);
                    if (!ins.isEmpty()) {
                        morfo = ins.text();
                        morfoDet = ins.attr("title");
                    }

                    ins = WebUtil.readNodeTags(el.html(), classMean);
                    if (!ins.isEmpty())
                        meaning = ins.text();

                    ins = WebUtil.readNodeTags(el.html(), "a");
                    if (!ins.isEmpty())
                        strong = Integer.parseInt(ins.text());
                    InterlinealVo it = new InterlinealVo(bookId, chp, vr, strong, word, morfo, meaning);
                    it.setMorfologic(morfoDet);
                    rds.add(it);
                }
            }

            for (InterlinealVo it : rds)
                writer.println(it);
        }
    }

    public static List<InterlinealVo> procesarTexto(int bookId, int chapter, int verse, String texto) {
        List<String> lineas = Arrays.stream(texto.split("\n"))
            .map(String::trim)
            .collect(Collectors.toList());
        
        List<InterlinealVo> palabras = new ArrayList<>();
        for(int i = 0; i < lineas.size(); i ++) {
            if(lineas.get(i).isEmpty()) continue;
            palabras.add(new InterlinealVo(bookId, chapter, verse, Integer.parseInt(lineas.get(i)), 
                    lineas.get(i + 2), lineas.get(i + 4), lineas.get(i + 6)));
            
            i += 6;
        }

        return palabras;
    }

    public static void loadInterlineal() throws SQLException, IOException {
        List<ItemVo> books = BibleUtil.booksList(2, base);           
        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", StandardCharsets.UTF_8.name())) {
            System.out.println("Cargando desde web...");
            for(ItemVo b : books) {
                if(b.getCodigo() > 43) continue;
                System.out.println("Leyendo libro: " + b.getValor() + " id: " + b.getCodigo());
                String book = b.getValor();
                List<ChapterVo> chps = BibleUtil.chaptersDetail(b.getCodigo(), base);
                for(ChapterVo o : chps) {
                    System.out.println("Leyendo capitulo: " + o.getChapterId());
                    for(Integer v : o.getVerses()) {
                        String vr = o.getChapterId() + ":" + v;
                        String url = String.format("https://www.bibliatodo.com/interlineal/%s-%s", 
                            TextUtils.quitarEspaciosYTildes(book.toLowerCase()), vr.replace(":", "-")); 
                        String txt = WebUtil.readURL(url, StandardCharsets.UTF_8.name());
                        txt = txt.substring(txt.lastIndexOf(vr) + vr.length());
                        txt = txt.substring(0, txt.indexOf("Ver Capítulo"));
                        if(txt.contains("Sin texto")) {
                            System.out.println("No se tiene texto de " + book + " " + vr);
                            continue;
                        } 
                        List<InterlinealVo> process = dividirTexto(b.getCodigo(), o.getChapterId(), v, txt);
                        writer.println(">>" + book + " " + vr);
                        for (InterlinealVo x : process) {
                            writer.println(x.getStrongId() + ">>" + x.getWord() + ">>" + x.getType() + ">>"+ x.getMeaning());
                        }
                    }
                }
            }
        }
    }

    public static void loadStrong() throws SQLException, IOException {
        Map<Integer, String> lgs = new LinkedHashMap<>();
        lgs.put(9006, "hebreo");
        lgs.put(5624, "griego");

        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", StandardCharsets.UTF_8.name())) {
            List<StrongVo> sts = new ArrayList<>();
            for(Entry<Integer, String> x : lgs.entrySet()) {
                for(int strongId = 9001; strongId <= x.getKey(); strongId++) {
                    System.out.println("Cargando desde web strong: " + strongId + " :" + x.getValue());
                    String url = String.format("https://www.bibliatodo.com/diccionario-strong/%s/%s", x.getValue(), strongId);
                    String txt = WebUtil.readURL(url, StandardCharsets.UTF_8.name());
                    txt = txt.substring(txt.lastIndexOf("Siguiente »«\n") + 13);
                    txt = txt.substring(0, txt.indexOf("......."));
                    txt = txt.replaceAll("\r\n", " ").replaceAll("\n", " ");
                    writer.println(txt);

                    StrongVo o = fromText(txt);
                    o.setLanguage(x.getValue());
                    sts.add(o);
                }
            }

            //System.out.println("Insertando...");
            //BibleUtil.createStrong(sts, base);
        }
    }
    
    public static void loadStrongKlogos(boolean existHTML) throws SQLException, IOException {
        Map<Integer, String> lgs = new LinkedHashMap<>();
        //lgs.put(8674, "strong_hebrew");
        //lgs.put(9006, "strong_hebrew");
        lgs.put(5624, "strongcodes");

        String fileHTML = "D:\\Desarrollo\\htmlStrong.txt";
        if(!existHTML) {
            try(PrintWriter writer = new PrintWriter(fileHTML, StandardCharsets.UTF_8.name())) {
                for(Entry<Integer, String> x : lgs.entrySet()) {
                    for(int strongId = 3303; strongId <= x.getKey(); strongId++) {
                        System.out.println("Cargando desde web strong: " + strongId + " :" + x.getValue());
                        String url = String.format("https://www.logosklogos.com/%s/%s", x.getValue(), strongId);
                        String txt = WebUtil.readHTML(url);
                        txt = WebUtil.readTag(txt, "div.panel-body", false);
                        writer.println(">>>>>>");
                        writer.println(txt);
                    }
                }
            }
        }

        /*int strongId = 1;
        String language = "hebreo";
        List<StrongVo> sts = new ArrayList<>();
        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", StandardCharsets.UTF_8.name())) {
            for(List<String> z : ListUtils.split(Files.readAllLines(new File(fileHTML).toPath(), StandardCharsets.UTF_8), ">>>>>>")) {
                String txt = z.stream().collect(Collectors.joining(" "));
                List<String> lines = WebUtil.readTags(txt, language.equals("griego") ? "h3.greek-strong" : "h3.heb-desc");
                if(lines.isEmpty()) continue;
                StrongVo o = new StrongVo();
                o.setStrongId(strongId++);
                o.setDefinition(lines.get(0));
                o.setLanguage(language);
                sts.add(o);
                writer.println(o);

                if(strongId == 8675) strongId = 9001;
                if(strongId == 9007) break;
            }
        }
        updateStrong(sts);*/

        int strongId = 1;
        String language = "griego";
        List<StrongVo> sts = new ArrayList<>();
        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", StandardCharsets.UTF_8.name())) {
            for(List<String> z : ListUtils.split(Files.readAllLines(new File(fileHTML).toPath(), StandardCharsets.UTF_8), ">>>>>>")) {
                String txt = z.stream().collect(Collectors.joining(" "));
                List<String> lines = WebUtil.readTags(txt, language.equals("griego") ? "h3.greek-strong" : "h3.heb-desc");
                if(lines.isEmpty()) continue;
                StrongVo o = new StrongVo();
                o.setStrongId(strongId++);
                o.setDefinition(lines.get(0));
                o.setLanguage(language);
                sts.add(o);
                writer.println(o);

                if(strongId == 2717) strongId = 2718;
                if(strongId == 3203) strongId = 3303;
            }
        }
        updateStrong(sts);
    }

    public static void updateStrong(List<StrongVo> sts) throws SQLException {
        try (Connection conn = db.connection(base)) {
            String sql = "update strong set definition = ? where strong_id = ? and language = ?";
            conn.setAutoCommit(false);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				int i = 0;
				for (StrongVo o : sts) {
					st.setString(1, o.getDefinition());
                    st.setInt(2, o.getStrongId());
                    st.setString(3, o.getLanguage());
					
					st.addBatch();
					if(i%5000 == 0)
                        st.executeBatch();
                    
                    i++;
				}
				st.executeBatch();
				conn.commit();
			}
        }
    }

    public static StrongVo fromText(String text) {
        String regex = "(Palabra Original:)([^P]+)|" +
                       "(Pronunciacion:)([^D]+)|" +
                       "(Definición:)([^D]+)|" +
                       "(Definición 2:)([^P]+)|" +
                       "(Parte del Discurso:)([^D]+)|" +
                       "(Derivacion:)([^D]+)|" +
                       "(Def\\. en RV1909:)([^N]+)|" +
                       "(RV1909 Número de Palabras:)([^$]+)|#(\\d+)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        StrongVo o = new StrongVo();
        while (matcher.find()) {
            if (matcher.group(1) != null) o.setWord(matcher.group(2).trim());
            if (matcher.group(5) != null) o.setDef(matcher.group(6).trim());
            if (matcher.group(7) != null) o.setDefAlterna(matcher.group(8).trim());
            if (matcher.group(9) != null) o.setType(matcher.group(10).trim());
            if (matcher.group(11) != null) o.setDeriva(matcher.group(12).trim());
            if (matcher.group(13) != null) o.setDefRV(matcher.group(14).trim());
            if (matcher.group(17) != null) o.setStrongId(Integer.parseInt(matcher.group(17).trim()));
        }

        o.setLanguage(text.substring(0, text.indexOf(" ")));
        o.setDefGlobal(text.substring(text.indexOf(" - ") + 3, text.indexOf("Palabra Original")));
        return o;
    }

    public static void interlineal() throws SQLException {
        int book = 0;
        List<String> res = FileUtils.readFile("D:\\Desarrollo\\interlineal2.txt");
        CitaVo c = null;
        List<InterlinealVo> sts = new ArrayList<>();
        for (String z : res) {
            if(z.startsWith(">>")) {
                System.out.println(z);
                c = BibleUtil.extractRef(z.substring(2));
                book = BibleUtil.bookId(c.getBook(), base);
                System.out.println("Insertando " + z + ": " + book);
            } else {
                String[] els = z.split(">>", 4);
                sts.add(new InterlinealVo(book, c.getChapter(), c.getVerseIni(), Integer.parseInt(els[0]), els[1], els[2], els[3]));
            }
        }

        createInterlineal(sts, 1);
    }

    public static void strong() throws SQLException {
        List<String> res = FileUtils.readFile("D:\\Desarrollo\\strong.txt");
        List<StrongVo> sts = new ArrayList<>();
        for (String z : res) {
            StrongVo o = fromText(z);
            sts.add(o);
        }

        createStrong(sts);
    }

    public static NotationVo notation(String in, String language) throws SQLException {
        NotationVo ext = extractNotacion(in, language);
        if(ext == null)
            return new NotationVo();

		NotationVo nt = new NotationVo();
        nt.setCode(in);
        nt.setTipo(descNotation(ext.getTipo(), "tipo"));
        nt.setTiempo(descNotation(ext.getTiempo(), "tiempo"));
        nt.setVoz(descNotation(ext.getVoz(), "voz"));
        nt.setModo(descNotation(ext.getModo(), "modo"));
        nt.setPersona(descNotation(ext.getPersona(), "persona"));
        nt.setCaso(descNotation(ext.getCaso(), "caso"));
        nt.setNumero(descNotation(ext.getNumero(), "numero"));
        nt.setGenero(descNotation(ext.getGenero(), "genero"));
        nt.setSufijo(descNotation(ext.getSufijo(), "V".equals(ext.getTipo()) ? "sufijo verbal" : "sufijo no verbal"));

        return nt;
	}

    private static String simpleDetailNot(String in) {
        return Arrays.asList(in.split("\\.")).stream().map(i -> {
                try {
                    return descNotation(i);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.joining("."));
    }

    public static NotationDetailVo notationDetail(String in, String language) throws SQLException {
        String notation = "Notacion: " + in;
        NotationVo ext = extractNotacion(in, language);
        if(ext == null)
            return null;
        
        List<ItemStringVo> res = new ArrayList<>();
        if("hebreo".equals(language)) {
            res = Arrays.asList(in.split("\\.")).stream().map(i -> {
                try {
                    return detailNotation(i);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }).collect(Collectors.toList());
            return new NotationDetailVo(notation, res);
        }

        for(ItemStringVo o : Arrays.asList(detailNotation(ext.getTipo(), "tipo"),
            detailNotation(ext.getTiempo(), "tiempo"),
            detailNotation(ext.getVoz(), "voz"),
            detailNotation(ext.getModo(), "modo"),
            detailNotation(ext.getPersona(), "persona"),
            detailNotation(ext.getCaso(), "caso"),
            detailNotation(ext.getNumero(), "numero"),
            detailNotation(ext.getGenero(), "genero"),
            detailNotation(ext.getSufijo(), "V".equals(ext.getTipo()) ? "sufijo verbal" : "sufijo no verbal"),
            detailNotation(ext.getTiempoConj(), "tiempo 2"),
            detailNotation(ext.getPersonaConj(), "persona 2"),
            detailNotation(ext.getNumeroConj(), "numero 2"),
            detailNotation(ext.getGeneroConj(), "genero 2"))) {
            if(o != null)
                res.add(o);
        }

        return new NotationDetailVo(notation, res);
    }

    public static String descNotation(String code, String tipo) throws SQLException {
        if(code == null)
            return null;

        ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select name from notation where code = ? and type = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, code);
                st.setString(2, tipo);
				result = st.executeQuery();
				if(result.next()) {
					return result.getString(1);
				} else return null;
			}
		}
	}

    public static String descNotation(String code) throws SQLException {
        if(code == null)
            return null;

        ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select name from notation where code = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, code);
				result = st.executeQuery();
				if(result.next()) {
					return result.getString(1);
				} else return null;
			}
		}
	}

    public static ItemStringVo detailNotation(String code, String tipo) throws SQLException {
        if(code == null)
            return null;

        ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select code, description, upper(type) from notation where code = ? and type = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, code);
                st.setString(2, tipo);
				result = st.executeQuery();
				if(result.next()) {
					return new ItemStringVo(result.getString(1), result.getString(2), result.getString(3));
				} else return null;
			}
		}
	}

    public static ItemStringVo detailNotation(String code) throws SQLException {
        if(code == null)
            return null;

        ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select code, description, name from notation where code = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setString(1, code);
				result = st.executeQuery();
				if(result.next()) {
					return new ItemStringVo(result.getString(1), result.getString(2), result.getString(3));
				} else return null;
			}
		}
	}

    private static NotationVo extractNotacion(String in, String language) {
        if(in == null)
            return null;

        if("hebreo".equals(language)) 
            return extractHebNotacion(in, language);

        String regex = "(ADV|CONJ|COND|PRT|PREP|INJ|ARAM|HEB|N-PRI|A-NUI|N-LI|N-OI)(?:-([A-Z]))?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(in);
        String s0 = null, s1 = null;
        boolean ok = false;
        while (matcher.find()) {
            ok = true;
            s0 = matcher.group(1);
            s1 = matcher.group(2);
        }

        if(ok) {
            NotationVo nt = new NotationVo();
            nt.setTipo(s0);
            nt.setSufijo(s1);
            return nt;
        }

        regex = "([VNARCDTKIXQFSP])-([A-Za-z0-9]+)(?:-([A-Za-z0-9]+))?(?:-([A-Za-z0-9]+))?";
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher(in);
        s0 = null;
        s1 = null;
        String s2 = null, s3 = null;
        while (matcher.find()) {
            ok = true;
            s0 = matcher.group(1);
            s1 = matcher.group(2);
            s2 = matcher.group(3);
            s3 = matcher.group(4);
        }

        if(!ok) {
            return null;
        }

        NotationVo nt = new NotationVo();
        nt.setTipo(s0);
        nt.setLanguage(language);
        if("V".equals(nt.getTipo())) {
            if(s1.length() == 4) {
                nt.setTiempo(s1.substring(0, 1));
                nt.setVoz(String.valueOf(s1.charAt(2)));
                nt.setModo(String.valueOf(s1.charAt(3)));
            } else {
                nt.setTiempo(String.valueOf(s1.charAt(0)));
                nt.setVoz(String.valueOf(s1.charAt(1)));
                nt.setModo(String.valueOf(s1.charAt(2)));
            }

            if(s2 != null) {
                if(s2.length() == 2) {
                    nt.setPersona(String.valueOf(s2.charAt(0)));
                    nt.setNumero(String.valueOf(s2.charAt(1)));
                } else {
                    nt.setCaso(String.valueOf(s2.charAt(0)));
                    nt.setNumero(String.valueOf(s2.charAt(1)));
                    nt.setGenero(String.valueOf(s2.charAt(2)));
                }
            }

            nt.setSufijo(s3);
        } else {
            nt.setCaso(String.valueOf(s1.charAt(0)));
            nt.setNumero(String.valueOf(s1.charAt(1)));
            if(s1.length() == 3)
                nt.setGenero(String.valueOf(s1.charAt(2)));

            if(s2 != null) {
                nt.setSufijo(s2);
            }
        }
        return nt;
    }

    private static NotationVo extractHebNotacion(String in, String language) {
        String regex = "^([a-z]+)" +
            "(?:\\.([a-z]+))?(?:\\.([a-z]+))?(?:\\.([a-z0-9]+))?(?:\\.([a-z]+))?(?:\\.([a-z]+))?" + 
            "(?:\\.([a-z]+))?(?:\\.([a-z0-9]+))?(?:\\.([a-z]+))?(?:\\.([a-z]+))?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(in);
        String[] s = new String[10];
        while (matcher.find()) {
            for(int i = 1; i <= 10; i++)
                s[i - 1] = matcher.group(i);
        }
        //tipo.modo.tiempo.persona.genero.numero.tiempo2.persona2.genero2.numero2
        NotationVo nt = new NotationVo();
        nt.setLanguage(language);
        nt.setTipo(s[0]);
        nt.setModo(s[1]);
        nt.setTiempo(s[2]);
        nt.setPersona(s[3]);
        nt.setGenero(s[4]);
        nt.setNumero(s[5]);
        nt.setTiempoConj(s[6]);
        nt.setPersonaConj(s[7]);
        nt.setGeneroConj(s[8]);
        nt.setNumeroConj(s[9]);

        return nt;
    }

    private static List<InterlinealVo> dividirTexto(int bookId, int chapter, int verse, String texto) {
        String[] lineas = texto.split("\n");
        List<InterlinealVo> elementos = new ArrayList<>();
        int codigo = Integer.parseInt(lineas[0].trim());
        int i = 1;
        while (i < lineas.length) {
            if (i + 2 < lineas.length) {
                String palabra = lineas[i].trim();
                String tipo = lineas[i + 1].trim();
                ItemVo aux = i + 2 == lineas.length - 1 ? new ItemVo(0, lineas[i + 2].trim()) : extraer(lineas[i + 2].trim());
                String significado = aux.getValor();
                elementos.add(new InterlinealVo(bookId, chapter, verse, codigo, palabra, tipo, significado));
                if (i + 3 < lineas.length) {
                    codigo = aux.getCodigo();
                }
                i += 3;
            } else {
                break;
            }
        }

        return elementos;
    }

    private static ItemVo extraer(String input) {
        String texto = null;
        int numero = 0;
        if(input.contains("XXX")) {
            texto = input.replaceAll("XXX", "");
            numero = 5016;
        } else {
            texto = input.replaceAll("[^a-zA-Z]", "");
            numero = Integer.parseInt(input.replaceAll("[^0-9]", ""));
        }

        return new ItemVo(numero, texto);
    }

    public static int max(String table, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select max(id) from " + table;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return (int)result.getLong(1);
				} else return 0;
			}
		}
	}

    public static String language(int testamentId, String version) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select language from version_lg where testament_id = ? and version = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, testamentId);
                st.setString(2, version);
				result = st.executeQuery();
				if(result.next()) {
					return result.getString(1);
				} else return null;
			}
		}
	}

    public static List<String> words(String version) throws SQLException {
        ResultSet result = null;
        List<String> res = new ArrayList<>();
        try (Connection conn = db.connection(version)) {
            String sql = "select replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(lower(text), '.', ''), ',', ''), ':', ''), '?', ''), '¿', ''), ';', ''), '(', ''), ')', ''), '!', ''), '¡', '') from verse";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                result = st.executeQuery();
                while(result.next()) {
                    res.add(result.getString(1));
                }
            }
        }
        return res;
    }

    public static List<RecordVo> wordsDetail(String version) throws SQLException {
        ResultSet result = null;
        List<RecordVo> res = new ArrayList<>();
        try (Connection conn = db.connection(version)) {
            String sql = "select book_id, chapter, verse, replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(text, '.', ''), ',', ''), ':', ''), '?', ''), '¿', ''), ';', ''), '(', ''), ')', ''), '!', ''), '¡', '') from verse";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                result = st.executeQuery();
                while(result.next()) {
                    res.add(new RecordVo(result.getInt(1), 0, result.getInt(2), "", result.getInt(3), result.getString(4)));
                }
            }
        }
        return res;
    }

    public static void main(String[] args) {
        try {
            interlinealKL(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
