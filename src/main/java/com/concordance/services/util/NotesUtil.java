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
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.bible.CitaVo;
import com.concordance.services.vo.bible.NoteBibleVo;

public class NotesUtil extends AutoresService {
	
	public static List<RecordVo> concordancia(String in, String base, boolean highlight) throws SQLException, IOException {
		if(in == null || in.trim().length() == 0)
			return null;
		
		List<RecordVo> res = new ArrayList<>();
		ResultSet result = null;
		try (Connection conn = db.connection(base)) {
			String sql = "select v.book_id, v.book_id, v.text || ' (' || b.parent || ':' || b.name || ')' "
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

	public static void readNotesFromURL() throws IOException, SQLException {
		List<List<String>> notes = FileUtils.readXLS("D:\\Desarrollo\\Notas obras agustin.xlsx");
		int cont = 1;
		List<NoteBibleVo> res = new ArrayList<>();
		for (List<String> o : notes) {
			int id = Integer.parseInt(o.get(0));
			String noteUrl = o.get(4);
			String text = null;
			System.out.println(String.format("Insertando %s de %s notas para %s: %s", 
				cont++, notes.size(), o.get(1), o.get(2)));
			try {
				text = WebUtil.readURL("https://www.augustinus.it" + noteUrl + ".htm", StandardCharsets.ISO_8859_1.name());
			} catch (Exception e) {
				System.out.println("URL sin resultados: " + noteUrl + ": " + e.getMessage());
			}

			if(text == null) continue;
			if(text.indexOf("1") < 0) {
				System.out.println("URL con error de inicio: " + noteUrl);
				continue;
			}
			text = text.substring(text.indexOf("1"));
			text = text.replace(",", ":");
			res.add(new NoteBibleVo(id, 0, 0, 0, text));
			//createNotes(Arrays.asList(text.split("\r\n")), id, 0, "Patristica");
		}
		createNotes(res, "Patristica");
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
					res.add(new NoteBibleVo(c.getBookId(), c.getChapter(), c.getVerseIni(), type, aux[1]));
				} else {
					/*List<String> foots = TextUtils.segmentos(b, "^([A-Za-záéíóúÁÉÍÓÚ0-9\s]+\s\d+:\d+)\s(.+)$");
					CitaVo c = BibleUtil.extractRef(foots.get(0));
					res.add(new NoteBibleVo(c.getBookId(), c.getChapter(), c.getVerseIni(), type, foots.get(1)));*/
					res.add(new NoteBibleVo(bookId, chapter, 0, type, b));
				}

			}
			for (NoteBibleVo o : res) {
				writer.println(o);
			}
			//createNotes(res, "RVR1960");
		}
	}

	public static void loadNotesFile(String base) throws IOException, SQLException {
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			List<String> nts = Files.readAllLines(new File("D:\\Desarrollo\\Notas.txt").toPath(), StandardCharsets.UTF_8);
			List<NoteBibleVo> res = new ArrayList<>();
			int bookId = 0;
			for(String x : nts) {
				if(x.trim().isEmpty()) continue;
				if(x.startsWith(">>")) {
					String[] key = x.substring(2).split(" - ");
					String autor = key[0];
					String obra = key[1];
					String seccion = obra;
					if(key.length > 2)
						seccion = key[2];
					
					bookId = AutoresService.bookId(seccion, obra, autor, base);
					continue;
				}
				res.add(new NoteBibleVo(bookId, 0, 0, 0, x));
			}

			createNotes2(res, base);
			for(NoteBibleVo x: res) {
				writer.println(x.getBookId() + " " + x.getText());
			}
			
			System.out.println("Notas creadas con exito");
		}
	}

	public static void main(String[] args) {
		try {
			loadNotesFile("Patristica");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
