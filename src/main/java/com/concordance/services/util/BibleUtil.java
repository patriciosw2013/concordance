package com.concordance.services.util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.concordance.services.vo.AutorVo;
import com.concordance.services.vo.Book;
import com.concordance.services.vo.ChapterVo;
import com.concordance.services.vo.ContentVo;
import com.concordance.services.vo.ItemIntVo;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.TextExtractedVo;
import com.concordance.services.vo.Verse;
import com.concordance.services.vo.bible.CitaVo;

public class BibleUtil implements Serializable {

	private static SQLUtil db = SQLUtil.getInstance();

	public static CitaVo extractRef(String texto) throws SQLException {
		String regex = "([\\d]*[\\p{L}\\.\\s]+)\\s(\\d+)(?:[:|,](\\d+))?(?:-(\\d+))?";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(texto);

		if (matcher.find()) {
			String libro = matcher.group(1).trim();
			int capitulo = Integer.parseInt(matcher.group(2));
			int vIni = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
			int vFin = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : vIni;
			Book b = book(libro, "RVR1960");
			return new CitaVo(b.getId(), b.getName() == null ? libro : b.getName(), capitulo, vIni, vFin, "RVR1960");
		} else {
			throw new RuntimeException("No se pudo extraer la referencia: " + texto);
		}
	}

	/* Obtiene la cita incluida la version */
	public static CitaVo cita(String texto) throws SQLException {
		String regex = "([\\d]*[\\p{L}\\.\\s]+)\\s(\\d+)(?:[:|,](\\d+))?(?:-(\\d+))?(?: (\\w+))?";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(texto);

		if (matcher.find()) {
			String libro = matcher.group(1).trim();
			int capitulo = Integer.parseInt(matcher.group(2));
			int vIni = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
			int vFin = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : vIni;
			String version = matcher.group(5) != null ? matcher.group(5) : "RVR1960";
			Book b = book(libro, version);
			return new CitaVo(b.getId(), b.getName() == null ? libro : b.getName(), capitulo, vIni, vFin, version);
		} else {
			throw new RuntimeException("No se pudo extraer la referencia: " + texto);
		}
	}

	public static List<Verse> verses(String cita) {
		CitaVo vr = null;
		try {
			vr = cita(cita);
		} catch (Exception e) {
			//e.printStackTrace();
			throw new RuntimeException("No se pudo extraer la cita: " + cita + " " + e.getMessage());
		}
		
		ResultSet result = null;
		String book = vr.getBook().replace(".", "");
		try (Connection conn = db.connection(vr.getVersion())) {
			String sql = "select a.name, b.chapter, b.verse, "
					+ "replace(replace(replace(replace(replace(replace(b.text, char(13), ''), char(10), ''), '*', ''), '«', ''), '»', ''), '—', '') as text "
					+ "from book a inner join verse b on (a.id = b.book_id) "
					+ "where (a.abbreviation = ? collate nocase or a.abb = ? collate nocase or a.name = ? collate nocase) "
					+ "and b.chapter = ? and b.verse between ? and ? ";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setString(1, book);
				st.setString(2, book);
				st.setString(3, book);
				st.setInt(4, vr.getChapter());
				st.setInt(5, vr.getVerseIni());
				st.setInt(6, vr.getVerseFin());
				result = st.executeQuery();
				List<Verse> res = new ArrayList<>();
				while (result.next()) {
					res.add(new Verse(result.getInt(2), result.getInt(3), 
						result.getString(4).trim().replaceAll("\\[\\d+\\]", "")));
				}
				return res;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.err.println("Error en base: " + vr.getVersion());
			new RuntimeException(ex.getMessage());
		}

		return null;
	}

	public static RecordVo verse(int verseId, String base) throws SQLException {
		if(verseId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, '', v.chapter, v.verse from verse v where v.id = " + verseId;
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

	public static List<String> extractVerses(String in) {
		List<String> res = new ArrayList<>();
		TextExtractedVo txt = null;
		do {
			txt = TextUtils.extract(in, "“(.*?)” \\((.*?)\\d+(.*?)\\)");
			if(txt.getEnd() > 0) {
				res.add(in.substring(txt.getStart(), txt.getEnd()));
				in = in.substring(txt.getEnd());
			}
		} while(txt.getEnd() > 0);

		return res;
	}

	public static List<Verse> readBook(String key) {
		List<String> res = FileUtils.readResource(String.format("/files/%s.txt", key));
		if (res == null)
			throw new RuntimeException("Error lectura de archivo " + key);

		List<Verse> verses = new ArrayList<>();
		int chapter = 1, verse = 1;
		for (String o : res) {
			if ("***".equals(o)) {
				chapter++;
				verse = 1;
				continue;
			}

			verses.add(new Verse(chapter, verse++, o));
		}
		return verses;
	}

	public static int bookId(String name, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select id from book where name = ? collate nocase");
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setString(1, name);
				result = st.executeQuery();
				if(result.next()) {
					return result.getInt(1);
				} else throw new RuntimeException(String.format("Libro %s no existe en version %s", name, base));
			}
		}
	}

	public static int testamentId(int bookId, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select testament_id from book where id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return result.getInt(1);
				} else throw new RuntimeException("Libro no existe");
			}
		}
	}
	
	public static List<RecordVo> concordancia(String in, String base, boolean highlight) throws SQLException {
		if(in == null || in.trim().length() == 0)
			return null;
		
		List<RecordVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, v.id, trim(v.text) || ' ' || '(' || b.name || ' ' || v.chapter || ':' || v.verse || ' ' || ? || ')' "
					+ "from verse v inner join book b on (b.id = v.book_id) "
					+ "where v.text like '%' || ? || '%' order by v.id";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setString(1, base);
				st.setString(2, in);
				result = st.executeQuery();
				while(result.next()) {
					RecordVo b = new RecordVo();
					b.setBookId(result.getInt(1));
					b.setRecordId(result.getInt(2));
					b.setText(result.getString(3));

					if(highlight)
						b.setText(b.getText().replaceAll(in, "<b style=\"color:red;\">" + in + "</b>").replaceAll("\\[\\d+\\]", ""));

					res.add(b);
				}
			}
		}
		
		return res;
	}

	public static Book book(int bookId, String base) throws SQLException {
		if(bookId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.name, '', v.name, '', '', '' from book v where v.id = " + bookId;
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

	public static Book book(String name, String base) throws SQLException {
		ResultSet result = null;
		String book = name.replace(".", "");
		try (Connection conn = db.connection(base)) {
			String sql = "select v.id, v.name from book v where v.name = ? collate nocase or v.abbreviation = ? collate nocase or v.abb = ? collate nocase";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setString(1, book);
				st.setString(2, book);
				st.setString(3, book);
				result = st.executeQuery();
				result.next();
				Book b = new Book();
				b.setId(result.getInt(1));
				b.setName(result.getString(2));
				return b;
			}
		}
	}

	public static Book bookForVerse(int verseId, String base) throws SQLException {
		if(verseId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.name, '', v.name, '', '', '', v.id "
					+ "from book v inner join verse vr on (vr.book_id = v.id) where vr.id = " + verseId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				result.next();
				Book b = new Book();
				b.setTitle(result.getString(1));
				b.setAutor(result.getString(2));
				b.setName(result.getString(3));
				b.setParent(result.getString(4));
				b.setDestination(result.getString(5));
				b.setBookDate(result.getString(6));
				b.setId(result.getInt(7));

				return b;
			}
		}
	}

	public static List<ItemVo> booksList(int testament, String base) throws SQLException {
		List<ItemVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select c.id, c.name, c.abb from book c where c.testament_id = " + testament;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemVo(result.getInt(1), result.getString(2), result.getString(3)));
				}
			}
		}
		return res;
	}

	public static List<ItemVo> chapters(int bookId, String base) throws SQLException {
		List<ItemVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select distinct c.chapter, b.name || ' ' || c.chapter from verse c inner join book b on (c.book_id = b.id) where c.book_id = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setInt(1, bookId);
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemVo(result.getInt(1), result.getString(2)));
				}
			}
		}
		return res;
	}

	public static List<ItemVo> verses(int bookId, int chapter, String base) throws SQLException {
		List<ItemVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select c.verse, cast(c.verse as text) from verse c where c.book_id = ? and c.chapter = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setInt(1, bookId);
				st.setInt(2, chapter);
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemVo(result.getInt(1), result.getString(2)));
				}
			}
		}
		return res;
	}

	public static List<Integer> chaptersIds(int bookId, String base) throws SQLException {
		List<Integer> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select distinct c.chapter from chapter c where c.book_id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(result.getInt(1));
				}
			}
		}
		return res;
	}

	public static List<ChapterVo> chaptersDetail(int bookId, String base) throws SQLException {
		if(bookId == 0)
			return null;

		List<ItemIntVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.chapter, v.verse from chapter v where v.book_id = ? order by 1,2";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setInt(1, bookId);
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemIntVo(result.getInt(1), result.getInt(2)));
				}
			}
		}
		return agruparPorCapitulo(res);
	}

	private static List<ChapterVo> agruparPorCapitulo(List<ItemIntVo> lista) {
		Map<Integer, ChapterVo> mapa = new HashMap<>();

		for (ItemIntVo item : lista) {
			int capitulo = item.getCodigo();
			int verso = item.getValor();
			if (!mapa.containsKey(capitulo))
				mapa.put(capitulo, new ChapterVo(capitulo));

			mapa.get(capitulo).addVerso(verso);
		}
		return new ArrayList<>(mapa.values());
	}

	public static List<AutorVo> books(String base) throws SQLException {
		List<AutorVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select id, name from testament";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					String testament = result.getString(2);
					List<Book> books = readBooks(result.getInt(1), base);

					Map<String, List<ItemVo>> map = books.stream()
						.collect(Collectors.groupingBy(
							Book::getParent,
							LinkedHashMap::new,
							Collectors.mapping(
								z -> new ItemVo(z.getId(), z.getName()),
								Collectors.toList()
							)
						));

					res.add(new AutorVo(testament, map));
				}
			}
		}
		
		return res;
	}

	public static List<Book> readBooks(int testament, String base) throws SQLException {
		List<Book> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.id, v.name, v.abbreviation from book v where v.testament_id = " + testament + " order by v.id";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					Book b = new Book();
					b.setId(result.getInt(1));
					b.setName(result.getString(2));
					b.setParent(result.getString(3));

					res.add(b);
				}
			}
		}
		
		return res;
	}

	public static ContentVo readContents(int bookId, int chapter, String base) throws SQLException {
		if(bookId == 0)
			return null;
		
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		int chp = 0;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.chapter, v.verse || ' ' || trim(v.text) "
					+ "from verse v where v.book_id = " + bookId +
					(chapter > 0 ? " and v.chapter = " + chapter : "");
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					chp = result.getInt(1);
					res.add(result.getString(2));
				}
			}
		}
		
		return new ContentVo(new ChapterVo(chp, String.valueOf(chp), null), res);
	}

	public static ContentVo readContents(CitaVo in, boolean verse, boolean cross, boolean title) throws SQLException {
		if(in.getBookId() == 0)
			return null;
		
		List<String[]> res = new ArrayList<>();
		ResultSet result = null;
		int chp = 0;
		try (Connection conn = db.connection(in.getVersion())) {
			String sql = "select v.chapter, v.verse, v.title, trim(v.text) from verse v where v.book_id = ? and v.chapter = ? " +
				(in.getVerseIni() > 0 && in.getVerseFin() > 0 ? "and v.verse between ? and ?" : "");
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setInt(1, in.getBookId());
				st.setInt(2, in.getChapter());
				if(in.getVerseIni() > 0 && in.getVerseFin() > 0) {
					st.setInt(3, in.getVerseIni());
					st.setInt(4, in.getVerseFin());
				}
				result = st.executeQuery();
				while(result.next()) {
					chp = result.getInt(1);
					res.add(new String[] {result.getString(2), result.getString(3), result.getString(4)});
				}
			}
		}
		
		return new ContentVo(new ChapterVo(chp, String.valueOf(chp), null), res.stream().map(i -> {
				String txt = i[2];
				if(verse) txt = i[0] + " " + txt;
				if(title && !TextUtils.isEmpty(i[1])) txt = String.format("<b>%s</b>\n%s", i[1], txt);
				if(!cross) txt = txt.replaceAll("\\[\\d+\\]", "");
				return txt;
			}).collect(Collectors.toList()));
	}
	
	public static ContentVo readContentsForVerse(int verseId, String base) throws SQLException {
		if(verseId == 0)
			return null;
		
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		int chp = 0;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select b.chapter, b.verse || ' ' || trim(b.text) from book a inner join verse b on (a.id = b.book_id) " + 
				"where (a.id, b.chapter) = (select x.book_id, x.chapter from verse x where x.id = %s)", verseId);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					chp = result.getInt(1);
					res.add(result.getString(2));
				}
			}
		}
		
		return new ContentVo(new ChapterVo(chp, String.valueOf(chp), null), res);
	}

	public static List<String> readNotes(int bookId, int chapter, String base) throws SQLException {
		if(bookId == 0)
			return null;
		
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, v.chapter, v.text from notes v where v.book_id = ? and v.chapter = ?";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				st.setInt(1, bookId);
				st.setInt(2, chapter);
				result = st.executeQuery();
				while(result.next()) {
					res.add(result.getString(3));
				}
			}
		}
		
		return res.stream().map(i -> procesarTextoBiblico(i).toString()).collect(Collectors.toList());
	}

	private static List<String> procesarTextoBiblico(String texto) {
        Pattern inicioValido = Pattern.compile("^\\d+\\.\\s(?:\\d\\s*)?[A-Z][a-z]{0,3}\\s?\\.(\\s*\\d+:\\d+)");
        Matcher inicioMatcher = inicioValido.matcher(texto.trim());

        if (!inicioMatcher.find()) {
            return Collections.singletonList(texto.trim());
        }
        return extraerReferencias(texto);
    }

    private static List<String> extraerReferencias(String texto) {
        List<String> resultado = new ArrayList<>();

        String regex = "((?:[1-3]\\s*)?[A-Z][a-z]{0,3}\\.)?\\s*(\\d+):(\\d+(?:-\\d+)?(?:,\\d+(?:-\\d+)?)*)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(texto);

        String libroActual = "";
		resultado.add(texto); //test
        while (matcher.find()) {
            String libro = matcher.group(1);
            String capitulo = matcher.group(2);
            String versiculos = matcher.group(3);

            if (libro != null) {
                libroActual = libro.trim();
            }

            String[] partesVersiculos = versiculos.split(",");
            for (String v : partesVersiculos) {
                resultado.add(libroActual + " " + capitulo + ":" + v);
            }
        }

        return resultado;
    }

	public static void main(String[] args) {
		try {
			System.out.println(extractRef("1 Cor. 12:1"));
			System.out.println(extractRef("1 Corintios 12:1-5"));
			System.out.println(extractRef("1 Cor. 12"));
			System.out.println(extractRef("Romanos 12:1"));
			System.out.println(extractRef("Rom. 12:1-5"));
			System.out.println(extractRef("Rom. 12"));
			System.out.println(extractRef("Rm 12"));
			System.out.println(extractRef("Rm 12:1"));
			System.out.println(extractRef("Rm 12,2"));

			System.out.println(cita("1 Cor. 12:1 NTV"));
			System.out.println(cita("1 Corintios 12:1-5 Vulgata"));
			System.out.println(cita("1 Cor. 12 RVR1960"));
			System.out.println(cita("Romanos 12:1 Latinoamericana"));
			System.out.println(cita("Rom. 12:1-5 DHH"));
			System.out.println(cita("Romanos 12 TLA"));
			System.out.println(cita("Rm 12 LXX"));
			System.out.println(cita("Rm 12:1 LXX"));
			System.out.println(cita("Rm 12,2 LXX"));

			//System.out.println(verses("Mateo 1:1"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
