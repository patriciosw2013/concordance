package com.concordance.services.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.concordance.services.AutoresService;
import com.concordance.services.vo.RecordVo;

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
			createNotes(Arrays.asList(text.split("\r\n")), id, "Patristica");
		}
		System.out.println("Notas creadas con exito");
	}
}
