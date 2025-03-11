package com.concordance.services.util;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.concordance.services.vo.bible.NoteBibleVo;

public class BibleUtil implements Serializable {

	private static SQLUtil db = SQLUtil.getInstance();

	public static CitaVo extractRef(String texto) throws SQLException {
		String regex = "([\\d]*[\\p{L}\\s]+)\\s(\\d+):(\\d+)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(texto);

		if (matcher.find()) {
			String libro = matcher.group(1).trim();
			int capitulo = Integer.parseInt(matcher.group(2));
			int versiculo = Integer.parseInt(matcher.group(3));
			Book b = book(libro, "RVR1960");
			return new CitaVo(b.getId(), b.getName(), capitulo, versiculo, 0, null);
		} else {
			System.out.println("No se pudo extraer la referencia.");
			return null;
		}
	}

	public static List<Verse> verses(String cita) {
		CitaVo vr = null;
		try {
			vr = cita(cita);
		} catch (Exception e) {
			//e.printStackTrace();
			throw new RuntimeException("No se pudo extraer la cita: " + cita);
		}
		
		ResultSet result = null;
		try (Connection conn = db.connection(vr.getVersion())) {
			String sql = String.format("select a.name, b.chapter, b.verse, "
					+ "replace(replace(replace(replace(replace(replace(b.text, char(13), ''), char(10), ''), '*', ''), '«', ''), '»', ''), '—', '') as text "
					+ "from book a inner join verse b on (a.id = b.book_id) "
					+ "where (a.abbreviation = '%s' or a.name = '%s' collate nocase) "
					+ "and b.chapter = %s and b.verse between %s and %s ", 
					vr.getBook(), vr.getBook(), vr.getChapter(), vr.getVerseIni(), vr.getVerseFin());
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				List<Verse> res = new ArrayList<>();
				while (result.next()) {
					res.add(new Verse(result.getInt(2), result.getInt(3), 
						result.getString(4).trim())); //.replaceAll("\s?[\[\(].*?[\]\)]", "").trim()));
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

	public static RecordVo verse(int verseId, String base) throws SQLException, IOException {
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
	
	private static CitaVo cita(String in) {
		int index = TextUtils.indexOf(in, "\\d+");
		if (index == 0)
			index = TextUtils.indexOf(in.substring(index + 1), "\\d+") + 1;

		String bk = in.substring(0, index).trim();
		int cap = Integer.parseInt(in.substring(index, in.indexOf(":")));
		String verStr = TextUtils.textBetween(in, ":", " ");
		String version = in.substring(in.lastIndexOf(" ") + 1);
		
		List<Integer> ver = Arrays.asList(verStr.split("-")).stream()
				.map(i -> Integer.parseInt(i)).collect(Collectors.toList());
		int v1 = ver.get(0);
		int v2 = ver.size() > 1 ? ver.get(1) : v1;
		
		return new CitaVo(0, bk, cap, v1, v2, version);
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

	public static int bookId(String name, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select id from book where name = '" + name + "' collate nocase");
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				if(result.next()) {
					return result.getInt(1);
				} else throw new RuntimeException("Libro no existe");
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
	
	public static List<RecordVo> concordancia(String in, String base, boolean highlight) throws SQLException, IOException {
		if(in == null || in.trim().length() == 0)
			return null;
		
		List<RecordVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select v.book_id, v.id, trim(v.text) || ' ' || '(' || b.name || ' ' || v.chapter || ':' || v.verse || ' %s)' "
					+ "from verse v inner join book b on (b.id = v.book_id) ", base)
					+ "where v.text like '% " + in + "%'";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					RecordVo b = new RecordVo();
					b.setBookId(result.getInt(1));
					b.setRecordId(result.getInt(2));
					b.setText(result.getString(3)); //.replaceAll("\s?[\[\(].*?[\]\)]", ""));

					if(highlight)
						b.setText(b.getText().replaceAll(in, "<b style=\"color:red;\">" + in + "</b>"));

					res.add(b);
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
			String sql = "select v.name, '', v.name, '', '', '' "
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

	public static Book book(String name, String base) throws SQLException {
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.id, v.name from book v where v.name = '" + name + "' collate nocase";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				result.next();
				Book b = new Book();
				b.setId(result.getInt(1));
				b.setName(result.getString(2));
				return b;
			}
		}
	}

	public static Book bookForVerse(int verseId, String base) throws SQLException, IOException {
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

	public static List<ItemVo> booksList(int testament, String base) throws SQLException, IOException {
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

	public static List<ItemVo> chapters(int bookId, String base) throws SQLException, IOException {
		if(bookId == 0)
			return null;

		List<ItemVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select distinct c.chapter, cast(c.chapter as text) from verse c where c.book_id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(new ItemVo(result.getInt(1), result.getString(2)));
				}
			}
		}
		return res;
	}

	public static List<Integer> chaptersIds(int bookId, String base) throws SQLException, IOException {
		List<Integer> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select c.chapter from chapter c where c.book_id = " + bookId;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(result.getInt(1));
				}
			}
		}
		return res;
	}

	public static List<ChapterVo> chaptersDetail(int bookId, String base) throws SQLException, IOException {
		if(bookId == 0)
			return null;

		List<ItemIntVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.chapter, v.verse from chapter v where v.book_id = " + bookId + " order by 1,2";
			try (PreparedStatement st = conn.prepareStatement(sql)) {
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

	public static ContentVo readContents(int bookId, int chapter, String base) throws SQLException, IOException {
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
		
		return new ContentVo(chp, String.valueOf(chp), res);
	}
	
	public static ContentVo readContentsForVerse(int verseId, String base) throws SQLException, IOException {
		if(verseId == 0)
			return null;
		
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		int chp = 0;
		try (Connection conn = db.connection(base)) {
			/*String sql = String.format("select v.chapter, v.verse || ' ' || trim(v.text) "
					+ "from verse v where v.book_id in (select x.book_id from verse x where x.id = %s) " 
					+ "and v.chapter in (select x.chapter from verse x where x.id = %s)", verseId, verseId);*/
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
		
		return new ContentVo(chp, String.valueOf(chp), res);
	}

	public static String readNotes(int bookId, int chapter, String base) throws SQLException {
		List<NoteBibleVo> foot = readNotes(bookId, chapter, 1, base);
		List<NoteBibleVo> cross = readNotes(bookId, chapter, 2, base);
		List<String> res = new ArrayList<>();
		if(!foot.isEmpty()) {
			res.add("* Notas al pie:");
			res.addAll(foot.stream().map(NoteBibleVo::getText).collect(Collectors.toList()));
		}

		if(!cross.isEmpty()) {
			res.add("* Referencias cruzadas:");
			res.addAll(cross.stream().map(i -> String.format("v.%s:%s: %s", i.getChapterId(), i.getVerse(), i.getText()))
				.collect(Collectors.toList()));
		}

		return res.stream().collect(Collectors.joining("\n"));
	}

	public static List<NoteBibleVo> readNotes(int bookId, int chapter, int type, String base) throws SQLException {
		if(bookId == 0)
			return null;
		
		List<NoteBibleVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select v.book_id, v.chapter, v.verse, v.type, v.text from notes v where v.book_id = %s and v.chapter = %s and v.type = %s", 
				bookId, chapter, type);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while(result.next()) {
					res.add(new NoteBibleVo(result.getInt(1), result.getInt(2), result.getInt(3), 
						result.getInt(4), result.getString(5)));
				}
			}
		}
		
		return res;
	}
}
