package com.concordance.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
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

import com.concordance.services.util.BibleUtil;
import com.concordance.services.util.FileUtils;
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
import com.concordance.services.vo.interlineal.StrongReferenceVo;
import com.concordance.services.vo.interlineal.StrongVo;

public class InterlinealService {

    private static String base = "Interlineal";
    private static SQLUtil db = SQLUtil.getInstance();

    public static List<InterlinealVo> interlineal(int book, int chapter, int verse) throws SQLException {
        List<InterlinealVo> res = new ArrayList<>();
        ResultSet result = null;
        try (Connection conn = db.connection("Interlineal")) {
            String sql = "select i.chapter, i.verse, i.strong_id, i.word, i.type, i.meaning, i.book_id, v.testament_id " +
                "from interlineal i inner join book v on (i.book_id = v.id) where i.book_id = ? and i.chapter = ? and i.verse = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, book);
                st.setInt(2, chapter);
                st.setInt(3, verse);
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
                    b.setMorfologic(notation(b.getType(), result.getInt(8)).summary());

                    res.add(b);
                }
            }
        }
        
        return res;
    }

    public static String simpleInterlineal(int verseId) throws SQLException {
        RecordVo c = BibleUtil.verse(verseId, "RVR1960");
        ResultSet result = null;
        List<ItemStringVo> res = new ArrayList<>();
        try (Connection conn = db.connection("Interlineal")) {
            String sql = "select i.word, i.meaning from interlineal i where i.book_id = ? and i.chapter = ? and i.verse = ?";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.setInt(1, c.getBookId());
                st.setInt(2, c.getChapterId());
                st.setInt(3, c.getVerse());
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

    public static void createInterlineal(List<InterlinealVo> process) throws SQLException, IOException {
        String sql = "insert into interlineal2 (id, book_id, chapter, verse, strong_id, word, type, meaning) values (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.connection(base)) {
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                int z = max("interlineal2", base) + 1;
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

    public static void createStrong(List<StrongVo> process) throws SQLException, IOException {
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

    public static List<String> notations() throws SQLException, IOException {
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

    public static List<StrongReferenceVo> strongReference(int strongId, int testamentId, String baseVerse) throws SQLException, IOException {
        if(strongId == 0)
            return new ArrayList<>();

        List<StrongReferenceVo> res = new ArrayList<>();
        ResultSet result = null;
        try (Connection conn = db.connection(base)) {
            String sql = String.format("attach database '%s' as db2", SQLUtil.bases.get(baseVerse));
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                st.executeUpdate();
            }

            sql = String.format("select i.chapter, i.verse, i.strong_id, i.word, i.type, i.meaning, i.book_id, " +
                "trim(v.text) || ' ' || '(' || a.name || ' ' || v.chapter || ':' || v.verse || ' %s)', v.id " +
                "from interlineal i inner join db2.book a on (i.book_id = a.id) inner join db2.verse v on (a.id = v.book_id) " +
                "where i.strong_Id = %s and a.testament_id = %s and v.chapter = i.chapter and v.verse = i.verse ", 
                baseVerse, strongId, testamentId);
            try (PreparedStatement st = conn.prepareStatement(sql)) {
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

    public static StrongDetailVo strongDetail(int strongId, String language) throws SQLException, IOException {
        ResultSet result = null;
        try (Connection conn = db.connection(base)) {
            String sql = "select i.language, i.def, i.def_alterna, i.type, i.deriva, i.def_rv, i.word, i.def_global " +
                "from strong i where i.strong_Id = " + strongId + " and i.language = '" + language + "'";
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                result = st.executeQuery();
                if(result.next()) {
                    StrongDetailVo o = new StrongDetailVo();
                    o.setStrongId(strongId);
                    o.setLanguage(result.getString(1));
                    o.setDefGlobal(result.getString(8));
                    o.setDetails(Arrays.asList(new String[]{"Palabra original:", result.getString(7)},
                        new String[]{"Definición:", result.getString(2)},
                        new String[]{"Definición2:", result.getString(3)},
                        new String[]{"Tipo:", result.getString(4)},
                        new String[]{"Derivacion:", formatDeriva(result.getString(5))},
                        new String[]{"Def. en RV1909", result.getString(6)}));

                    return o;
                } else return null;
            }
        }
    }

    private static String formatDeriva(String in) {
        if(in == null) return null;

        return in.replaceAll("(\\d+)", "<a href=\"#\" onclick=\"ldStrong([{name: 'param1', value: this.textContent}]);\">$1</a>");
    }

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

            createInterlineal(res);
            for (InterlinealVo x : res) {
                writer.println(x.getBookId() + ">>" + x.getChapter() + ">>" + x.getVerse() + ">>" + 
                    x.getStrongId() + ">>" + x.getWord() + ">>" + x.getType() + ">>"+ x.getMeaning());
            }

            System.out.println("Interlineal creado con exito");
        }
    }

    public static List<InterlinealVo> procesarTexto(int bookId, int chapter, int verse, String texto) {
        /*return Arrays.stream(texto.split("\n"))
                .map(String::trim)  // Eliminar espacios de cada línea
                .filter(linea -> !linea.isEmpty())  // Eliminar líneas vacías
                .collect(Collectors.groupingBy(linea -> Arrays.asList("codigo", "letra", "tipo", "significado").indexOf("tipo") % 4))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<String> partes = entry.getValue();
                    return new InterlinealVo(bookId, chapter, verse, Integer.parseInt(partes.get(0)), 
                        partes.get(1), partes.get(2), partes.get(3));
                })
                .collect(Collectors.toList());*/
        List<String> lineas = Arrays.stream(texto.split("\n"))
            .map(String::trim)
            //.filter(linea -> !linea.isEmpty())
            .collect(Collectors.toList());
        
        List<InterlinealVo> palabras = new ArrayList<>();
        for(int i = 0; i < lineas.size(); i ++) {
            if(lineas.get(i).isEmpty()) continue;
            //System.out.println(lineas.get(i) + " " + lineas.get(i + 2) + " " + lineas.get(i+4) + " " + lineas.get(i + 6));
            palabras.add(new InterlinealVo(bookId, chapter, verse, Integer.parseInt(lineas.get(i)), 
                    lineas.get(i + 2), lineas.get(i + 4), lineas.get(i + 6)));
            
            i += 6;
        }

        
        /*for (int i = 0; i < lineas.size(); i += 4) {
            if (i + 3 < lineas.size()) {
                System.out.println(lineas.get(i) + " " + lineas.get(i + 1) + " " + lineas.get(i+2) + " " + lineas.get(i + 3));
                palabras.add(new InterlinealVo(bookId, chapter, verse, Integer.parseInt(lineas.get(i)), 
                    lineas.get(i + 1), lineas.get(i + 2), lineas.get(i + 3)));
            }
        }*/

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
                            //BibleUtil.createStrong(b.getCodigo(), process, base);
                        }
                        //writer.println("\n");

                        //BibleUtil.createStrong(b.getCodigo(), process, base);
                    }
                }
            }
        }
    }

    public static void loadStrong() throws MalformedURLException, IOException, SQLException {
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

    public static void interlineal() throws SQLException, IOException {
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

        createInterlineal(sts);
    }

    public static void strong() throws SQLException, IOException {
        List<String> res = FileUtils.readFile("D:\\Desarrollo\\strong.txt");
        List<StrongVo> sts = new ArrayList<>();
        for (String z : res) {
            StrongVo o = fromText(z);
            sts.add(o);
        }

        createStrong(sts);
    }

    public static NotationVo notation(String in, int testamentId) throws SQLException {
        NotationVo ext = extractNotacion(in, testamentId);
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

    public static NotationDetailVo notationDetail(String in, int testamentId) throws SQLException {
        NotationVo ext = extractNotacion(in, testamentId);
        if(ext == null)
            return null;

        List<ItemStringVo> res = new ArrayList<>();
        String notation = "Notacion: " + in;
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
			String sql = String.format("select name from notation where code = '%s' and type = '%s'", code, tipo);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
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
			String sql = String.format("select code, description, upper(type) from notation where code = '%s' and type = '%s'", code, tipo);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return new ItemStringVo(result.getString(1), result.getString(2), result.getString(3));
				} else return null;
			}
		}
	}

    private static NotationVo extractNotacion(String in, int testamentId) {
        if(testamentId == 1) 
            return extractHebNotacion(in, testamentId);

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
            nt.setTestamentId(testamentId);
            nt.setSufijo(s1);
            return nt;
        }

        regex = "([VNARCDTKIXQFSP])-([A-Za-z0-9]+)(?:-([A-Za-z0-9]+))?(?:-([A-Za-z0-9]+))?"; //"V-([A-Za-z0-9]+)(-[A-Za-z0-9]+)*";
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
        nt.setTestamentId(testamentId);
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
            nt.setGenero(String.valueOf(s1.charAt(2)));

            if(s2 != null) {
                nt.setSufijo(s2);
            }
        }
        return nt;
    }

    //verbo.etpe.perf.p3.f.pl
    private static NotationVo extractHebNotacion(String in, int testamentId) {
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
        nt.setTipo(s[0]);
        nt.setTestamentId(testamentId);
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

    public static List<InterlinealVo> dividirTexto(int bookId, int chapter, int verse, String texto) {
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
			String sql = String.format("select max(id) from " + table);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return (int)result.getLong(1);
				} else return 0;
			}
		}
	}
    public static void main(String[] args) {
        try {
            //strong();
            //loadStrong();
            //interlineal();
            //loadInterlineal();

            /*String[] patrones = {"V-ADP-APM", "V-2RPP-ASM", "V-AAM-3S", "V-AAN", "V-AOI-1P-ATT", "A-APF-C", "N-VPF", "A-NUI-ABB", 
                "V-PPA-NMP", "CONJ", "N-LI", "N-NSM"};*/
            String[] patrones = {"adjv.pual.ptcp.u.f.sg.c", "verbo.tif.perf.p1.u.sg.prs.p2.m.pl", "advb"};
            for (String o : patrones) { //notations()) {
                System.out.println(o);
                NotationVo nt = extractNotacion(o,1);
                if(nt != null)
                    System.out.println("   " + nt.summary());
                
                NotationDetailVo dt = notationDetail(o,1);
                System.out.println("   " + dt);
            }

            //strongReference(1, 1, "RVR1960");

            loadInterlinealKL();

            //strongReference(1732, 1, "RVR1960");
            //System.out.println("fin consulta");

            //System.out.println("Creacion de interlineal exitosa");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
