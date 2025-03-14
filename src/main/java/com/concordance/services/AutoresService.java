package com.concordance.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import com.concordance.services.util.FileUtils;
import com.concordance.services.util.SQLUtil;
import com.concordance.services.util.TextUtils;
import com.concordance.services.vo.AutorVo;
import com.concordance.services.vo.Book;
import com.concordance.services.vo.BookMetadata;
import com.concordance.services.vo.ContentVo;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.Paragraph;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.bible.NoteBibleVo;

public class AutoresService {

    protected static SQLUtil db = SQLUtil.getInstance();
	public static Map<String, BookMetadata> complex;

	static {
		complex = new HashMap<>();
		complex.put("Agustin de Hipona - Sermones", BookMetadata.builder().keySplit("^(SERMÓN).*")
			.indexTitle(1)
			.joiningKey("\n").build());
        complex.put("Agustin de Hipona - Cartas", BookMetadata.builder().keySplit("^(CARTA).*")
			.indexTitle(1)
			.indexDestination(2)
			.indexDate(3).joiningKey("\n").build());
		complex.put("Agustin de Hipona - Cuestiones sobre el Heptateuco", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(0)
			.chapter(false).verses(true)
			.regexChapter(null).regexVerses("^\\d+[ ].*?")
			.chapterKey(null).verseKey(" ").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Comentarios a los Salmos", BookMetadata.builder().keySplit("^(SALMO).*")
			.indexTitle(-1)
			.chapter(false).verses(true)
			.regexChapter(null).regexVerses("^\\d+[.].*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("A Diogneto", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^[MDCLXVI]+.*?").regexVerses(null)
			.chapterKey(".").joiningKey("\n").build());
		complex.put("Epistola de Bernabe", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^[MDCLXVI]+[.].*?").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Gregorio Magno - Libros morales", BookMetadata.builder().keySplit("^(LIBRO|PREFACIO).*")
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^([MDCLXVI]+\\.|Prefacio).*$").regexVerses("^\\d+(\\. ).*?")
			.chapterKey(". ").verseKey(".").joiningKey("\n").chpTogether(true).build());
		complex.put("Ireneo de Lyon - Contra las herejias", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1).build());
		complex.put("Origenes de Alejandria - Contra Celso", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(false).verses(true).build());
		complex.put("Didache", BookMetadata.builder().keySplit("^(PARTE).*")
			.indexTitle(0)
			.chapter(true).verses(false)
			.regexChapter("^[MDCLXVI]+.*?").regexVerses(null)
			.chapterKey(".").verseKey(null).joiningKey(" ").build());
		complex.put("Ignacio de Antioquia - Epistolas", BookMetadata.builder().keySplit("^(EPÍSTOLA).*")
			.indexTitle(0)
			.chapter(true).verses(false)
			.regexChapter("^[MDCLXVI]+.*?").regexVerses(null)
			.chapterKey(".").verseKey(null).joiningKey(" ").build());
		complex.put("Gregorio de Nisa - La gran catequesis", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^([MDCLXVI]+\\.|Prólogo).*$").regexVerses("^\\d+(\\. ).*?")
			.chapterKey(". ").verseKey(".").joiningKey("\n").chpTogether(true).build());
		complex.put("Tertuliano de Cartago - Sobre la oracion", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(CAPÍTULO).*$").regexVerses(null)
			.chapterKey(" - ").verseKey(null).joiningKey("\n").build());
		complex.put("Cirilo de Jerusalén - Catequesis", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(CATEQUESIS).*$").regexVerses("^\\d+(\\. ).*?")
			.chapterKey(".").verseKey(". ").joiningKey(" ").build());
		complex.put("Agustin de Hipona - Sermon de la montaña", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(CAPÍTULO).*$").regexVerses("^\\d+(\\. ).*?")
			.chapterKey(null).verseKey(". ").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Cuestiones a Simpliciano", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(CUESTIÓN|PREFACIO).*$").regexVerses("^\\d+(\\. ).*?")
			.chapterKey(null).verseKey(".").joiningKey(" ").build());
		complex.put("Cipriano de Cartago - Cartas", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(CARTA).*$").regexVerses(null)
			.chapterKey(null).verseKey(null).joiningKey("\n").build());
		complex.put("Agustin de Hipona - Confesiones", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(0)
			.indexDestination(1)
			.indexDate(2)
			.chapter(true).verses(true)
			.regexChapter("^(CAPÍTULO).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Homilias sobre 1 Juan", BookMetadata.builder().keySplit("^(HOMILÍA).*")
			.indexTitle(-1)
			.chapter(false).verses(true)
			.regexChapter(null).regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Tratados sobre el evangelio de Juan", BookMetadata.builder().keySplit("^(TRATADO).*")
			.indexTitle(-1)
			.chapter(false).verses(true)
			.regexChapter(null).regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - La ciudad de Dios", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(0)
			.chapter(true).verses(true)
			.regexChapter("^(PRÓLOGO|CAPÍTULO).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Exposicion de la carta a los Galatas", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(false).verses(true)
			.regexChapter(null).regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - La Trinidad", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(0)
			.chapter(true).verses(true)
			.regexChapter("^(CAPÍTULO|PROLOGO|PREFACIO).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Sermón a los catecúmenos sobre el Símbolo", BookMetadata.builder().keySplit(null)
			.indexTitle(0)
			.chapter(true).verses(true)
			.regexChapter("^[MDCLXVI]+\\s.*?").regexVerses("^\\d+(\\.).*?")
			.chapterKey(" ").verseKey(".").joiningKey("\n").chpTogether(true).build());
		complex.put("Juan Damasceno - Exposicion de la fe", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(false).verses(true)
			.regexChapter(null).regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey(" ").build());
		complex.put("Basilio de Cesárea - Reglas morales", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(PRESENTACIÓN|SOBRE EL JUICIO DE DIOS|SOBRE LA FE|SUMARIO|Regla \\d+).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey(" ").build());
		complex.put("Basilio de Cesárea - El Espíritu Santo", BookMetadata.builder().keySplit(null)
			.indexTitle(0)
			.chapter(true).verses(true)
			.regexChapter("^(Capítulo ).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey(" ").build());
		complex.put("Ambrosio de Milán - Sobre la fe", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(Dedicatoria|(\\d+\\.)$|Prólogo|Carta dedicatoria).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey(" ").build());
		complex.put("Agustin de Hipona - El don de la perseverancia", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(CAPÍTULO).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - La predestinacion de los santos", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(CAPÍTULO).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Contra dos cartas de los pelagianos", BookMetadata.builder().keySplit("^(LIBRO.*)")
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(Capítulo.*)").regexVerses("^(\\d+)\\..*")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Agustin de Hipona - Contra Juliano", BookMetadata.builder().keySplit("^(LIBRO.*)")
			.indexTitle(0)
			.chapter(true).verses(true)
			.regexChapter("^([MDCLXVI]+)\\..*").regexVerses("^(\\d+)\\..*")
			.chapterKey(null).verseKey(null).chpTogether(true).joiningKey("\n").build());
		complex.put("Rufino de Aquileya - Comentario al símbolo apostólico", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter(null).regexVerses("^(\\d+)\\..*")
			.chapterKey(null).verseKey(null).joiningKey("\n").build());

		/* Autores */
		complex.put("Tomas de Kempis - IMITACIÓN DE CRISTO", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(-1)
			.chapter(true).verses(true).build());
		complex.put("Juan Calvino - Institucion de la religion cristiana", BookMetadata.builder().keySplit("^(LIBRO).*")
			.indexTitle(0)
			.chapter(true).verses(true)
			.regexChapter("^(CAPÍTULO).*$").regexVerses("^\\d+(\\.).*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		/*complex.put("Catecismo de Heidelberg", new BookMetadata(null, -1, 0, 0, 
			true, true, "^(Día del Señor \\d+)$", "^\\d+(\\.).*?", 
			null, ".", "\n"));
		complex.put("Confesión Belga", new BookMetadata(null, -1, 0, 0, 
			true, false, "^(ARTÍCULO).*$", null, 
			null, null, "\n"));
		complex.put("Cánones de Dort", new BookMetadata("CAPÍTULO", -1, 0, 0, 
			true, false, "^(ARTÍCULO).*$", null, 
			null, null, "\n"));
		complex.put("Confesión de Fe de Westminster", new BookMetadata(null, -1, 0, 0, 
			true, false, "^(Capítulo).*$", null, 
			null, null, "\n"));
		complex.put("Confesión Bautista de Fe de Londres de 1689", new BookMetadata(null, -1, 0, 0, 
			true, true, "^(CAPÍTULO).*$", "^\\d+(\\.).*?", 
			null, ".", "\n"));
		complex.put("Los treinta y nueve artículos", new BookMetadata(null, -1, 0, 0, 
			true, false, "^(ARTÍCULO).*$", null, 
			null, null, "\n"));
		complex.put("Catecismo Mayor de Westminster", new BookMetadata(null, -1, 0, 0, 
			true, false, "^([¿].*[P.\\d+.]).*$", null, 
			null, null, "\n"));
		complex.put("Catecismo Menor de Westminster", new BookMetadata(null, -1, 0, 0, 
			true, false, "^([¿].*[P.\\d+.]).*$", null, 
			null, null, "\n"));*/
		complex.put("Catecismo romano", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^((PARTE|PRÓLOGO).*)$").regexVerses("^(\\d+)\\s.*")
			.chapterKey(null).verseKey(" ").joiningKey("\n").build());
		complex.put("Articulos de Esmalcalda", BookMetadata.builder().keySplit(null)
			.indexTitle(0)
			.chapter(true).verses(true)
			.regexChapter("^(PARTE|Prólogo|ARTICULO).*$").regexVerses("^\\d+[.].*?")
			.chapterKey(null).verseKey(".").joiningKey("\n").build());
		complex.put("Confesion de fe de Augsburgo 1530", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(Prefacio|Artículo).*$").regexVerses(null)
			.chapterKey(null).verseKey(null).joiningKey("\n").build());
		complex.put("Martin Lutero - Catecismos", BookMetadata.builder().keySplit("^(CATECISMO M).*")
			.indexTitle(0)
			.indexDestination(1)
			.indexDate(2)
			.chapter(true).verses(false)
			.regexChapter("^(PREFACIO|PRÓLOGO|CATECISMO BREVE|PARTE).*$").regexVerses(null)
			.chapterKey(null).verseKey(null).joiningKey("\n").build());
		complex.put("Tomas de Aquino - Suma teologica", BookMetadata.builder().keySplit("^(PARTE).*")
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^(Cuestión )\\d+[.].*$").regexVerses("^(Artículo )\\d+[:].*?")
			.chapterKey(".").verseKey("").joiningKey("\n").build());
		complex.put("Charles Stanley - Mensajes", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(MENSAJE).*$").regexVerses(null)
			.chapterKey(null).verseKey("").joiningKey("\n").build());
		complex.put("Baruc Korman - Estudios", BookMetadata.builder().keySplit("^(EXPOSICION).*")
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(CAPÍTULO).*$").regexVerses(null)
			.chapterKey(null).verseKey("").joiningKey("\n").build());
		complex.put("Fabian Liendo - Predicas", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(MENSAJE).*$").regexVerses(null)
			.chapterKey(null).verseKey("").joiningKey("\n").build());
		complex.put("Mishnah", BookMetadata.builder().keySplit("^(DIVISIÓN).*$")
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(CAPÍTULO).*$").regexVerses(null)
			.chapterKey(null).verseKey(null).joiningKey("\n").build());
		complex.put("Talmud", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(Talmud|Mishna).*$").regexVerses(null)
			.chapterKey(null).verseKey(null).joiningKey("\n").build());
		complex.put("C. S. Lewis - Cartas del diablo a su sobrino", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(false)
			.regexChapter("^(PREFACIO|[MDCLXVI])+$").regexVerses(null)
			.chapterKey(null).verseKey(null).joiningKey("\n").build());
		complex.put("C. S. Lewis - Mero cristianismo", BookMetadata.builder().keySplit(null)
			.indexTitle(-1)
			.chapter(true).verses(true)
			.regexChapter("^((LIBRO|PREFACIO).*)").regexVerses("^(\\d+)\\..*")
			.chapterKey(null).verseKey(null).joiningKey("\n").build());
	}
    
    public static RecordVo verse(int verseId, String base) throws SQLException, IOException {
		if(verseId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, coalesce(c.name, ''), c.id, v.verse from verse v left join chapter c on (c.id = v.chapter_id) where v.id = " + verseId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				result.next();
				RecordVo b = new RecordVo();
				b.setBookId(result.getInt(1));
				b.setChapter(result.getString(2));
				b.setChapterId(result.getInt(3));
				b.setVerse(result.getInt(4));
				b.setRecordId(verseId);

				return b;
			}
		}
	}

    public static List<RecordVo> concordancia(String in, String base, boolean highlight) throws SQLException, IOException {
		if(in == null || in.trim().length() == 0)
			return null;
		
		List<RecordVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, v.id, c.name, coalesce(v.chapter_id, 0), v.verse, substr(v.text, " +
				"CASE when instr(lower(v.text), 'siempre') - 30 < 0 THEN instr(lower(v.text),'siempre') ELSE instr(lower(v.text),'siempre') - 30 end, " + 
				"CASE WHEN length('siempre') + 70 <= length(v.text) THEN length('siempre') + 70 ELSE length(v.text) end) " +
				"|| ' ' || '(' || (case when c.name is null then '' else c.name || ', ' end) || b.parent || ', ' || " +
				"(case when b.name = b.parent then '' else b.name end) || ' ' || coalesce(c.name,'') || ' ' || " +
				"(case when v.verse > 0 then v.verse else '' end) || ')'" +
				"from verse v inner join book b on (v.book_id = b.id) left join autor c on (c.longname = b.autor) " +
				"left join chapter cp on (cp.id = v.chapter_id) " +
				"where v.text like '% " + in + "%' order by b.autor, b.parent, b.name";
			sql = sql.replaceAll("siempre", in.toLowerCase());
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					RecordVo b = new RecordVo();
					b.setBookId(result.getInt(1));
					b.setRecordId(result.getInt(2));
					b.setChapter(result.getString(3));
					b.setChapterId(result.getInt(4));
					b.setVerse(result.getInt(5));
					b.setText(result.getString(6));

					if(highlight)
						b.setText(b.getText().replaceAll(in, "<b style=\"color:red;\">" + in + "</b>"));

					res.add(b);
				}
			}
		}
		
		return res;
	}

    public static ContentVo readContents(int bookId, int chapter, String base) throws SQLException, IOException {
		if(bookId == 0)
			return null;
		
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.text from verse v where v.book_id = " + bookId;
			if(chapter > 0)
				sql = sql + " and v.chapter_id = '" + chapter + "'";
			System.out.println(sql);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(result.getString(1));
				}
			}
		}
		
		return new ContentVo(chapter, chapterName(chapter, base), res);
	}

	public static String chapterName(int id, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select name from chapter where id = " + id);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return result.getString(1);
				} else return null;
			}
		}
	}

	public static Book bookForVerse(int verseId, String base) throws SQLException, IOException {
		if(verseId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.title, coalesce(c.name, ''), case when v.name = v.parent then '' else v.name end, v.parent, v.id " +
				"from book v inner join verse vr on (vr.book_id = v.id) left join autor c on (c.longname = v.autor) where vr.id = " + verseId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				result.next();
				Book b = new Book();
				b.setTitle(result.getString(1));
				b.setAutor(result.getString(2));
				b.setName(result.getString(3));
				b.setParent(result.getString(4));
				b.setId(result.getInt(5));

				return b;
			}
		}
	}

	public static List<ItemVo> chapters(int bookId, String base) throws SQLException, IOException {
		if(bookId == 0)
			return null;

		List<ItemVo> res = new ArrayList<>();
		//res.add(new SelectItem(0, "TODOS"));
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select c.id, c.name from chapter c where c.book_id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemVo(result.getInt(1), result.getString(2)));
				}
			}
		}
		return res;
	}

	public static List<ItemVo> chaptersForVerse(int verseId, String base) throws SQLException, IOException {
		if(verseId == 0)
			return null;

		List<ItemVo> res = new ArrayList<>();
		res.add(new ItemVo(0, "TODOS"));
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select c.id, c.name from verse vr inner join chapter c on (vr.book_id = c.book_id) where vr.id = " + verseId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemVo(result.getInt(1), result.getString(2)));
				}
			}
		}
		return res;
	}

	public static List<ItemVo> booksForVerse(int verseId, String base) throws SQLException, IOException {
		if(verseId == 0)
			return null;

		List<ItemVo> res = new ArrayList<>();
		res.add(new ItemVo(0, "TODOS"));
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select x.id, x.name from book x where x.parent = (select b.parent from verse vr inner join book b on (vr.book_id = b.id) where vr.id = " + verseId + ")";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemVo(result.getInt(1), result.getString(2)));
				}
			}
		}
		return res;
	}

    public static Book book(int bookId, String base) throws SQLException, IOException {
		if(bookId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.title, v.autor, v.name, v.parent, v.destination, v.bookDate "
					+ "from book v where v.id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				result.next();
				Book b = new Book();
				b.setId(bookId);
				b.setTitle(result.getString(1));
				b.setAutor(result.getString(2));
				b.setName(result.getString(3));
				b.setParent(result.getString(4));
				b.setDestination(result.getString(5));
				b.setBookDate(result.getString(6));

				return b;
			}
		}
	}

	public static int bookId(String name, String parent, String autor, String base) throws SQLException, IOException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select id from book where parent = '%s' and name = '%s' and autor = '%s'", parent, name, autor);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next())
					return result.getInt(1);
				else throw new RuntimeException("No existe id de libro");
			}
		}
	}

	public static void deleteVerses(int bookId, String base) throws SQLException, IOException {
		System.out.println("Borrando versos libro " + bookId);
		try (Connection conn = db.connection(base)) {
			String sql = "delete from verse where book_id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.executeUpdate();
			}
		}
	}

	public static void deleteChapters(int bookId, String base) throws SQLException, IOException {
		System.out.println("Borrando capitulos libro " + bookId);
		try (Connection conn = db.connection(base)) {
			String sql = "delete from chapter where book_id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.executeUpdate();
			}
		}
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

	public static boolean existBook(String name, String parent, String autor, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select count(*) from book where parent = '%s' and name = '%s' and autor = '%s'", parent, name, autor);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return result.getLong(1) > 0;
				}
			}
		}

		return false;
	}

	public static List<AutorVo> books(String base) throws SQLException {
		List<AutorVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select distinct coalesce(autor, 'Anonimo') from book v order by autor";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					String autor = result.getString(1);
					List<Book> books = readBooks(autor, base);
					/*Map<String, List<SelectItem>> list = readBooks(autor, base).stream().collect(Collectors.toMap(Book::getParent,
						i -> new SelectItem(i.getId(), i.getName()),  
						(i, j) -> i, 
						LinkedHashMap::new
					));*/

					Map<String, List<ItemVo>> map = books.stream()
						.collect(Collectors.groupingBy(
							Book::getParent,
							LinkedHashMap::new,
							Collectors.mapping(
								z -> new ItemVo(z.getId(), z.getName()),
								Collectors.toList()
							)
						));

					res.add(new AutorVo(TextUtils.value(autor, "Anonimo"), map));
				}
			}
		}
		
		return res;
	}

	public static List<Book> readBooks(String autor, String base) throws SQLException {
		List<Book> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.id, v.title, v.autor, v.name, v.parent, v.destination, v.bookDate "
					+ "from book v where v.autor = '" + autor + "' order by v.parent, v.id";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					Book b = new Book();
					b.setId(result.getInt(1));
					b.setTitle(result.getString(2));
					b.setAutor(result.getString(3));
					b.setName(result.getString(4));
					b.setParent(result.getString(5));
					b.setDestination(result.getString(6));
					b.setBookDate(result.getString(7));

					res.add(b);
				}
			}
		}
		
		return res;
	}

	public static void createNotes2(List<NoteBibleVo> notes, String base) throws SQLException {
		String sql = "insert into notes (id, book_id, text, chapter) values (?, ?, ?, ?)";
		try (Connection conn = db.connection(base)) {
			conn.setAutoCommit(false);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				int z = max("notes", base) + 1;
				int i = 0;
				for (NoteBibleVo o : notes) {
					st.setInt(1, z++);
					st.setInt(2, o.getBookId());
					st.setString(3, o.getText());
					st.setInt(4, o.getChapterId());
					
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

	public static void createNotes(List<NoteBibleVo> notes, String base) throws SQLException {
		String sql = "insert into notes (id, book_id, text, chapter, verse, type) values (?, ?, ?, ?, ?, ?)";
		try (Connection conn = db.connection(base)) {
			conn.setAutoCommit(false);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				int z = max("notes", base) + 1;
				int i = 0;
				for (NoteBibleVo o : notes) {
					st.setInt(1, z++);
					st.setInt(2, o.getBookId());
					st.setString(3, o.getText());
					st.setInt(4, o.getChapterId());
					st.setInt(5, o.getVerse());
					st.setInt(6, o.getType());
					
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

	public static String readNotes(int bookId, String base, String chapter) throws SQLException, IOException {
		if(bookId == 0)
			return null;
		
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.text from notes v where v.book_id = " + bookId + " and v.chapter = '" + chapter + "'";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next())
					res.add(result.getString(1));
			}

			if(res.isEmpty()) {
				sql = "select v.text from notes v where v.book_id = " + bookId;
				try (PreparedStatement st = conn.prepareStatement(sql)) {
					result = st.executeQuery();
					while(result.next())
						res.add(result.getString(1));
				}
			}
		}
		
		return res.stream().collect(Collectors.joining("\n"));
	}

	public static void read(String path, String base, boolean sim) throws SQLException, IOException {
		String bookName = FilenameUtils.removeExtension(new File(path).getName());
		System.out.println("Creando libro: " + bookName);
		BookMetadata meta = complex.get(bookName);
		/*List<Book> books = FileUtils.splitDocx(bookName, path, meta);
		if(meta != null) {
			if(meta.isChapter())
				books = FileUtils.chapterText(books, meta);
			else if(meta.isVerses())
				books = FileUtils.verseText(books, meta);
		}*/

		List<Book> books = FileUtils.processDoc(bookName, path, meta);
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			for(Book b : books) {
				if(b.getTitle() != null && b.getTitle().length() > 100)
					System.out.println(">>>>>>>>>>>>>>> ALERTA <<<<<<<<<<<<<<<<<" + b.getName() + ": " + b.getTitle().substring(0,100));

				writer.println(b);
				writer.println("========");
				for(Paragraph p : b.getParagraphs())
					writer.println(p);
			}
		}

		if(sim) return;

		int total = books.size();
		int cont = 1;
        for(Book b : books) {
			boolean exist = existBook(b.getName(), b.getParent(), b.getAutor(), base);
			boolean chps = b.getParagraphs().stream().filter(i -> !TextUtils.isEmpty(i.getChapter())).count() > 0;
            try (Connection conn = db.connection(base)) {
				conn.setAutoCommit(false);
				if(exist) {
					System.out.println("Actualizando: " + b.getParent() + ": " + b.getName() + " " + (cont++) + " de " + total);
					b.setId(bookId(b.getName(), b.getParent(), b.getAutor(), base));
					deleteVerses(b.getId(), base);
					deleteChapters(b.getId(), base);
				} else {
					System.out.println("Insertando: " + b.getParent() + ": " + b.getName() + " " + (cont++) + " de " + total);
					b.setId(max("book", base) + 1);
					String sql = "insert into book (id, name, parent, title, autor, destination, bookDate) values (?, ?, ?, ?, ?, ?, ?)";
					try (PreparedStatement st = conn.prepareStatement(sql)) {
						st.setInt(1, b.getId());
						st.setString(2, b.getName());
						st.setString(3, b.getParent());
						st.setString(4, b.getTitle());
						st.setString(5, b.getAutor());
						st.setString(6, b.getDestination());
						st.setString(7, b.getBookDate());
						
						st.executeUpdate();
					}
				}

				int i = 0;
				if(chps) {
					String sql = "insert into chapter (id, book_id, name) values (?, ?, ?)";
					int z = max("chapter", base);
					Set<String> chapters = new LinkedHashSet<>();
					try (PreparedStatement st = conn.prepareStatement(sql)) {
						for (Paragraph o : b.getParagraphs()) {
							if(chapters.add(o.getChapter())) {
								o.setChapterId(++z);
							} else {
								o.setChapterId(z);
								continue;
							}
							System.out.println("Insertando capitulo: " + o.getChapter() + " " + o.getChapterId());
							st.setInt(1, o.getChapterId());
							st.setInt(2, b.getId());
							st.setString(3, o.getChapter());
							
							st.addBatch();
							if(i%5000 == 0)
								st.executeBatch();
							
							i++;
						}
						st.executeBatch();
					}
				}
				
				String sql = "insert into verse (id, book_id, text, verse, chapter, chapter_id) values (?, ?, ?, ?, ?, ?)";
				int z = max("verse", base) + 1;
				i = 0;
				try (PreparedStatement st = conn.prepareStatement(sql)) {
					for (Paragraph o : b.getParagraphs()) {
						st.setInt(1, z++);
						st.setInt(2, b.getId());
						st.setString(3, o.getText());
						st.setInt(4, o.getVerse());
						st.setString(5, o.getChapter());
						st.setInt(6, o.getChapterId());
						
						st.addBatch();
						if(i%5000 == 0)
							st.executeBatch();
						
						i++;
					}
					st.executeBatch();
				}

				conn.commit();
			}
		}
		
		System.out.println("Insercion exitosa");
	}
}
