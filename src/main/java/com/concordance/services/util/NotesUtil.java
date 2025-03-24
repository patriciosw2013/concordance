package com.concordance.services.util;

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
import java.util.List;

import com.concordance.services.AutoresService;
import com.concordance.services.vo.Book;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.bible.CitaVo;
import com.concordance.services.vo.bible.NoteBibleVo;

public class NotesUtil extends AutoresService {
	
	public static List<RecordVo> concordancia(String in, String base, boolean highlight) throws SQLException {
		if(in == null || in.trim().length() == 0)
			return null;
		
		List<RecordVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, v.id, v.text || ' (' || b.parent || ':' || b.name || ')' "
					+ "from notes v inner join book b on (b.id = v.book_id) "
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

	public static Book bookForVerse(int verseId, String base) throws SQLException {
		if(verseId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.title, coalesce(a.name, 'Anonimo'), case when v.name = v.parent then '' else v.name end, v.parent, v.id " +
				"from book v inner join notes vr on (vr.book_id = v.id) left join autor a on (a.longname = v.autor) where vr.id = " + verseId;
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

	public static RecordVo verse(int verseId, String base) throws SQLException {
		if(verseId == 0)
			return null;

		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, coalesce(c.name, ''), c.id, v.id from notes v left join chapter c on (c.id = v.chapter) where v.id = " + verseId;
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

	public static void readNotesFromURL() throws IOException, SQLException {
		List<List<String>> notes = FileUtils.readXLS("D:\\Desarrollo\\Notas obras agustin.xlsx");
		int cont = 1;
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			for (List<String> o : notes) {
				String noteUrl = o.get(4);
				List<String> text = null;
				String url = "https://www.augustinus.it" + noteUrl + ".htm";
				System.out.println(String.format("Notas %s de %s para %s: %s, url: %s", 
					cont++, notes.size(), o.get(1), o.get(2), url));
				try {
					//text = WebUtil.readURL(url, StandardCharsets.ISO_8859_1.name());
					text = WebUtil.readTags(WebUtil.readHTML(url), "p");
				} catch (Exception e) {
					System.out.println("URL sin resultados: " + noteUrl + ": " + e.getMessage());
				}

				if(text == null) continue;
				if(text.isEmpty()) continue;
				writer.println(String.format(">>Agustin de Hipona - %s - %s - %s", 
					o.get(1).trim(), o.get(1).trim(), o.get(2).trim()));
				for(String z: text)
					writer.println(z);
			}
		}
		System.out.println("Notas creadas con exito");
	}

	public static void loadBibleNotes() throws IOException, SQLException {
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			List<String> nts = Files.readAllLines(new File("D:\\Desarrollo\\notes.txt").toPath(), StandardCharsets.UTF_8);
			List<NoteBibleVo> res = new ArrayList<>();
			int type = 0;
			int bookId = 0, chapter = 0;
			for(int i = 0; i < nts.size(); i++){
				String b = nts.get(i);
				if(b.startsWith(">>>>>")) {
					bookId = BibleUtil.bookId(b.substring(6, b.lastIndexOf(" ")), "RVR1960");
					chapter = Integer.parseInt(b.substring(b.lastIndexOf(" ") + 1));;
					continue;
				}
				if(b.equals("Footnotes")) {
					type = 1;
					continue;
				} else if(b.equals("Cross references")) {
					type = 2;
					continue;
				}

				if(b.equals("Génesis 2")) continue;
				if(type == 2) {
					String[] aux = b.split(" : ");
					CitaVo c = BibleUtil.extractRef(aux[0]);
					res.add(new NoteBibleVo(c.getBookId(), c.getChapter(), aux[1]));
				} else {
					/*List<String> foots = TextUtils.segmentos(b, "^([A-Za-záéíóúÁÉÍÓÚ0-9\s]+\s\d+:\d+)\s(.+)$");
					CitaVo c = BibleUtil.extractRef(foots.get(0));
					res.add(new NoteBibleVo(c.getBookId(), c.getChapter(), c.getVerseIni(), type, foots.get(1)));*/
					res.add(new NoteBibleVo(bookId, chapter, b));
				}

			}
			for (NoteBibleVo o : res) {
				writer.println(o);
			}
			//createNotes(res, "RVR1960");
		}
	}

	public static void loadNotesFile(String base, boolean simul) throws IOException, SQLException {
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			List<String> nts = Files.readAllLines(new File("D:\\Desarrollo\\Notas.txt").toPath(), StandardCharsets.UTF_8);
			List<NoteBibleVo> res = new ArrayList<>();
			int bookId = 0;
			int chapterId = 0;
			for(String x : nts) {
				if(x.trim().isEmpty()) continue;
				if(x.startsWith(">>")) {
					String[] key = x.substring(2).split(" - ");
					String autor = key[0];
					String obra = key[1];
					String seccion = obra;
					String chapter = null;
					if(key.length > 2)
						seccion = key[2];
					if(key.length > 3)
					chapter = key[3];

					bookId = AutoresService.bookId(seccion, obra, autor, base);
					chapterId = chapter != null ? AutoresService.chapterId(bookId, chapter, base) : 0;
					continue;
				}
				res.add(new NoteBibleVo(bookId, chapterId, x));
			}

			if(!simul) createNotes(res, base);
			for(NoteBibleVo x: res) {
				writer.println(x.getBookId() + " " + x.getChapterId() + " " + x.getText());
			}
			
			System.out.println("Notas creadas con exito");
		}
	}

	public static void createNotes(List<NoteBibleVo> notes, String base) throws SQLException {
		String sql = "insert into notes (id, book_id, text, chapter) values (?, ?, ?, ?)";
		try (Connection conn = db.connection(base)) {
			conn.setAutoCommit(false);
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				int z = DBUtil.max("notes", base) + 1;
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

	public static void main(String[] args) {
		try {
			readNotesFromURL();
			//loadNotesFile("Patristica", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
