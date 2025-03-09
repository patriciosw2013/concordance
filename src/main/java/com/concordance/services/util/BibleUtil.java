package com.concordance.services.util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.ItemIntVo;
import com.concordance.services.vo.ItemStringVo;
import com.concordance.services.vo.TextExtractedVo;
import com.concordance.services.vo.Verse;
import com.concordance.services.vo.bible.BibleBook;
import com.concordance.services.vo.bible.CitaVo;
import com.concordance.services.vo.interlineal.InterlinealVo;

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
					res.add(new Verse(result.getInt("chapter"), result.getInt("verse"), result.getString("text").trim()));
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

	public static List<Verse> verse(int bookId, int chapter, int verse, String base) {
		ResultSet result = null;
		List<Verse> res = new ArrayList<>();
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select v.chapter, v.verse, trim(v.text) || ' ' || '(' || a.name || ' ' || v.chapter || ':' || v.verse || ' %s)' "
					+ "from book a inner join verse v on (a.id = v.book_id) "
					+ "where a.id = %s and v.chapter = %s and v.verse = %s ", 
					base, bookId, chapter, verse);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while (result.next()) {
					res.add(new Verse(result.getInt(1), result.getInt(2), result.getString(3).trim()));
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return res;
	}

	public static List<ItemStringVo> verse(List<InterlinealVo> ids, String base) {
		ResultSet result = null;
		List<ItemStringVo> res = new ArrayList<>();
		try (Connection conn = db.connection(base)) {
			StringBuilder consulta = new StringBuilder();
        	for (int i = 0; i < ids.size(); i++) {
				if (i > 0) {
					consulta.append(" UNION ALL ");
				}

				consulta.append(String.format("SELECT %s id, %s chp, %s vr, %s word", 
					ids.get(i).getBookId(), ids.get(i).getChapter(), ids.get(i).getVerse(), ""));
			}

			String sql = String.format("select trim(v.text) || ' ' || '(' || a.name || ' ' || v.chapter || ':' || v.verse || ' %s)', p.word "
					+ "from book a inner join verse v on (a.id = v.book_id) INNER JOIN (%s) p on (p.id = a.id and p.chp = b.chapter and p.vr = b.verse) ", 
					base, consulta.toString());
			System.out.println(sql);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				result = st.executeQuery();
				while (result.next()) {
					res.add(new ItemStringVo(result.getString(2), result.getString(1).trim()));
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return res;
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
	
	public static void read(String folder) throws SQLException, IOException {
		String base = "D:\\workspace\\bible\\src\\main\\resources\\db\\DHH.sqlite";
		for(File file : new File(folder).listFiles()) {
			if(!file.getName().endsWith(".txt"))
				continue;
			
			List<String> res = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			String txt = res.stream().collect(Collectors.joining(" "));
			List<String> chp = Arrays.asList(txt.split("[0-9]+:1 ")).stream().filter(i -> i.length() > 0).collect(Collectors.toList());
			
			String name = file.getName().replace(".txt", "");
			String[] metadata = name.split("-");
			
			BibleBook b = new BibleBook();
			b.setKey(Integer.parseInt(metadata[0]));
			b.setAbbr(metadata[1]);
			b.setTitle(metadata[2]);
			b.setTestament(Integer.parseInt(metadata[3]));
			b.setVerses(new ArrayList<Verse>());
			System.out.println(String.format("Lib: %s # capitulos: %s", b.toString(), chp.size()));
			
			int i = 1;
			int j = 1;
			for(String z: chp) {
				System.out.println("Cap. " + i);
				String[] w = z.split("[0-9]+ (.*?)");
				for(String u:w) {
					b.getVerses().add(new Verse(i, j, u));
					System.out.println(j + " " + u);
					j++;
				}
				
				System.out.println();
				j = 1;
				i++;
			}
			
			try (Connection conn = db.connection(base)) {
				String sql = "delete from verse where book_id = " + b.getKey();
				try (PreparedStatement st = conn.prepareStatement(sql)) {
					st.execute();
				}
				
				sql = String.format("delete from book where id = '%s'", b.getKey());
				try (PreparedStatement st = conn.prepareStatement(sql)) {
					st.execute();
				}
				
				int z = max("verse", base) + 1;
				sql = "insert into book (id, testament_id, name, abbreviation) values (?, ?, ?, ?)";
				try (PreparedStatement st = conn.prepareStatement(sql)) {
					st.setInt(1, b.getKey());
					st.setInt(2, b.getTestament());
					st.setString(3, b.getTitle());
					st.setString(4, b.getAbbr());
					
					st.executeUpdate();
				}
				
				sql = "insert into verse (id, book_id, chapter, verse, text) values (?, ?, ?, ?, ?)";
				try (PreparedStatement st = conn.prepareStatement(sql)) {
					for (Verse o : b.getVerses()) {
						st.setInt(1, z++);
						st.setInt(2, b.getKey());
						st.setInt(3, o.getChapter());
						st.setInt(4, o.getVerse());
						st.setString(5, o.getText());
						
						st.addBatch();
					}
					st.executeBatch();
				}
			}
		}
		
		System.out.println("Insercion exitosa");
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
					b.setText(result.getString(3));

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
	
	public static ContentVo readContentsForVerse(int verseId, String base) throws SQLException, IOException {
		if(verseId == 0)
			return null;
		
		List<String> res = new ArrayList<>();
		ResultSet result = null;
		int chp = 0;
		try (Connection conn = db.connection(base)) {
			String sql = String.format("select v.chapter, v.verse || ' ' || trim(v.text) "
					+ "from verse v where v.book_id in (select x.book_id from verse x where x.id = %s) " 
					+ "and v.chapter in (select x.chapter from verse x where x.id = %s)", verseId, verseId);
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
	
	public static void main(String[] args) {
		String res = BibleUtil.verses("Rom. 15:30 RVR1960").stream().map(Verse::getText)
				.collect(Collectors.joining(" "));
		System.out.println(res);
		
		String par = "Hay controversia cuando se (expresa) que Dios endurece el corazón del faraón: “Y yo endureceré el corazón de Faraón, y (multiplicaré) en la tierra de Egipto mis señales y mis maravillas” (Éxodo 7:3 RVR1960), pero esto no significa que Dios hizo que el faraón se volviera desobediente para cumplir sus propósitos, ya que Dios no tienta a nadie: “Cuando sean tentados, acuérdense de no decir: Dios me está tentando. Dios nunca es tentado a hacer el mal y jamás tienta a nadie” (Santiago 1:13 NTV), el faraón terminó endurecido por haber rechazado a Dios y creído en engaños: “Y los hechiceros de Egipto hicieron lo mismo con sus encantamientos; y el corazón de Faraón se endureció, y no los escuchó; como Jehová lo había dicho” (Éxodo 7:22 RVR1960)";
		
		for(String s : TextUtils.texts(par, "“(.*?)RVR1960\\)|“(.*?)NTV\\)|“(.*?)NVI\\)|“(.*?)JBS\\)|“(.*?)TLA\\)"))
			System.out.println(s);
		
		/*try {
			read("D:\\Desarrollo\\books");
		} catch(Exception e) {
			e.printStackTrace();
		}*/
	}
}
