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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.concordance.services.vo.ItemVo;
import com.concordance.services.vo.RecordVo;
import com.concordance.services.vo.Verse;
import com.concordance.services.vo.bible.BibleBook;
import com.concordance.services.vo.bible.BookRefVo;

public class ImportBibleUtil {

    private static SQLUtil db = SQLUtil.getInstance();

    public static void importNotesBibleGateway() throws IOException {
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			ListUtils.split(Files.readAllLines(new File("D:\\Desarrollo\\1960gateway.txt").toPath(), 
				StandardCharsets.UTF_8), ">>>>>>").stream().forEach(res -> {
				BookRefVo z = new BookRefVo();
				z.setRef(res.get(0));
				writer.println(">>>>>>" + res.get(0));
				Map<String, List<String>> sum = new LinkedHashMap<>();
				sum.put("verses", new ArrayList<>());
				sum.put("Footnotes", new ArrayList<>());
				sum.put("Cross references", new ArrayList<>());
				List<String> aux = sum.get("verses");
				for(int i = 1; i < res.size(); i++) {
					String line = res.get(i).trim();
					if(line.equals("Footnotes")) {
						aux = sum.get("Footnotes");
					} else if(line.equals("Cross references")) {
						aux = sum.get("Cross references");
					}

					aux.add(line);
				}

				for(String c : sum.get("Footnotes"))
					writer.println(c);
				for(String c : sum.get("Cross references"))
					writer.println(c);
			});
		}
	}

	public static void loadBible(String base, boolean existHTML) throws SQLException, IOException {
		String fileHtml = String.format("D:\\Desarrollo\\html-%s.txt", base);
		if(!existHTML) {
			try(PrintWriter writer = new PrintWriter(fileHtml, "UTF-8")) {
				for(int testId : new int[]{1, 2}) {
					for(ItemVo b : BibleUtil.booksList(testId, base)) {
						for(Integer c : BibleUtil.chaptersIds(b.getCodigo(), base)) {
							String url = String.format("https://www.biblegateway.com/passage/?search=%s&version=%s", 
								TextUtils.quitarAcentos(b.getValor()).concat(" ").replaceAll(" ", "%20")
								.concat(c + ""), base);
							System.out.println(url);
							String txt = WebUtil.readHTML(url);
							txt = txt.substring(txt.indexOf("result-text-style-normal text-html\">") + 36);
							txt = txt.substring(0, txt.indexOf("<div class=\"passage-scroller no-sidebar\">"));
							writer.println(">>>>>>");
							writer.println(b.getValor() + " " + c);
							writer.println(txt);
						}
					}
				}
			}
		}
	}

	public static void loadBibleCatolic(String base, boolean existHTML) throws IOException, SQLException {
		String fileHtml = String.format("D:\\Desarrollo\\html-%s.txt", base);
		if(!existHTML) {
			try(PrintWriter writer = new PrintWriter(fileHtml, "UTF-8")) {
				for(int testId : new int[]{2}) {
					for(ItemVo b : BibleUtil.booksList(testId, base)) {
						for(Integer c : BibleUtil.chaptersIds(b.getCodigo(), base)) {
							String url = String.format("https://www.sobicain.org/it/biblewebapp/?bid=1&bk=%s&cp=%s", 
								b.getCodigo(), c);
							System.out.println(url);
							String txt = WebUtil.readHTML(url);
							txt = txt.substring(txt.indexOf("<div id=\"bwa-text\">") + 19);
							txt = txt.substring(0, txt.indexOf("<div class=\"prev-next\">"));
							writer.println(">>>>>>");
							writer.println(b.getValor() + " " + c);
							writer.println(txt.replaceAll("<span class=\"versenumber\">", "\n<span class=\"versenumber\">"));
						}
					}
				}
			}
		}

		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\versPrev.txt", "UTF-8")) {
			ListUtils.split(Files.readAllLines(new File(fileHtml).toPath(), 
				StandardCharsets.UTF_8), ">>>>>>").stream().forEach(res -> {
				writer.println(">>>>>>" + res.get(0));
				boolean start = false;
				for(int i = 0; i < res.size(); i++) {
					String g = res.get(i);
					if(!start && !g.contains("<sup>1</sup>")) continue;
					else start = true;

					if(g.contains("<sup>0</sup>")) continue;

					if(g.contains("versenumber") || g.contains("versetext"))
						writer.println(WebUtil.formatHtml(g));
				}
			});
		}

		extractImportVerses(base);
	}

	public static void importBookBibleGateway(String base) throws SQLException, IOException {
		String fileHtml = String.format("D:\\Desarrollo\\html-%s.txt", base);
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\versPrev.txt", "UTF-8")) {
			ListUtils.split(Files.readAllLines(new File(fileHtml).toPath(), 
				StandardCharsets.UTF_8), ">>>>>>").stream().forEach(res -> {
				writer.println(">>>>>>" + res.get(0));
				for(int i = 0; i < res.size(); i++) {
					String g = res.get(i);
					if(g.contains("</sup>") || g.contains("chapternum"))
						writer.println(WebUtil.formatHtml(g));
					if(g.contains("verse line")) {
						writer.println(" " + WebUtil.formatHtml(res.get(i + 1)));
						i++;
					}
				}
			});
		}

		extractImportVerses(base);
	}

	private static void extractImportVerses(String base) throws IOException, SQLException {
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			String regex = "(\\d+)\\s*(.*)";  // captura el número de verso y el texto
			Pattern pattern = Pattern.compile(regex);
			List<RecordVo> versiculos = new ArrayList<>();
			StringBuilder textoVerso = new StringBuilder();
			int numeroVerso = -1;
			int bookId = 0, chapter = 0;
			int verse = 1;
			List<String> auxbk = null;
			List<List<String>> bks = new ArrayList<>();
			for (String linea : Files.readAllLines(new File("D:\\Desarrollo\\versPrev.txt").toPath(), 
				StandardCharsets.UTF_8)) {
				if(linea.startsWith(">>>>>>")) {
					auxbk = new ArrayList<>();
					bks.add(auxbk);
				}

				auxbk.add(linea);
			}

			for(List<String> h : bks) {
				for (String linea : h) {
					if(linea.startsWith(">>>>>>")) {
						bookId = BibleUtil.bookId(linea.substring(6, linea.lastIndexOf(" ")), base);
						chapter = Integer.parseInt(linea.substring(linea.lastIndexOf(" ") + 1));
						verse = 1;
						continue;
					}
					Matcher matcher = pattern.matcher(linea);
					if (matcher.matches()) {
						if (numeroVerso != -1) {
							versiculos.add(new RecordVo(bookId, 0, chapter, null, verse++, 
								textoVerso.toString().replace(" ", "").trim()));
						}
						
						numeroVerso = Integer.parseInt(matcher.group(1));
						textoVerso = new StringBuilder(matcher.group(2));
					} else {
						if (numeroVerso != -1) {
							textoVerso.append("\n").append(linea.replace(" ", "").trim());
						}
					}
				}

				if (numeroVerso != -1) {
					versiculos.add(new RecordVo(bookId, 0, chapter, null, verse, 
						textoVerso.toString().replace(" ", "").trim()));
				}
				numeroVerso = -1;
			}

			createVerses(versiculos, base);
			for (RecordVo versiculo : versiculos) {
				writer.println(versiculo);
			}
			System.out.println("Versos creados exitosamente");
		}
	}

	public static void importBookYouversion(String base, boolean existHTML) throws SQLException, IOException {
		String fileHtml = String.format("D:\\Desarrollo\\html_%s.txt", base);
		if(!existHTML) {
            try(PrintWriter writer = new PrintWriter(fileHtml, "UTF-8")) {
                int idVersion = 103; //823 vulgata
                for(int testId : new int[]{1, 2}) {
                    for(ItemVo b : BibleUtil.booksList(testId, base)) {
                        for(Integer c : BibleUtil.chaptersIds(b.getCodigo(), base)) {
                            String url = String.format("https://www.bible.com/es/bible/%s/%s.%s.%s", //823  - VULG 
                            idVersion, b.getDescripcion(), c, base);
                            System.out.println(url);
                            String txt = WebUtil.readWeb(url, "div.ChapterContent_reader__Dt27r", false);
                            txt = txt.replaceAll("<span class=\"ChapterContent_label", "\r\n<span class=\"ChapterContent_label");
                            txt = txt.replaceAll("<span class=\"ChapterContent_content__RrUqA", "\r\n<span class=\"ChapterContent_content__RrUqA");

                            if(txt.contains("Ese capítulo no está disponible en esta versión"))
                                throw new RuntimeException("Pagina no encontrada");
                            writer.println(">>>>>>");
							writer.println(b.getValor() + " " + c);
                            writer.println(txt);
                            break;
                        }
                        break;
                    }
                }
            }
		}

		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\versPrev.txt", "UTF-8")) {
			ListUtils.split(Files.readAllLines(new File(fileHtml).toPath(), 
				StandardCharsets.UTF_8), ">>>>>>").stream().forEach(res -> {
				writer.println(">>>>>>" + res.get(0));
				for(int i = 0; i < res.size(); i++) {
					String g = res.get(i);
					if(g.contains("ChapterContent_label__R2PLt") || g.contains("ChapterContent_content__RrUqA")) {
						String txt = WebUtil.formatHtml(g);
						if(!TextUtils.isEmpty(txt))
							writer.println(txt);
					}
				}
			});
		}

		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			List<RecordVo> versiculos = new ArrayList<>();
			StringBuilder textoVerso = new StringBuilder();
			int numeroVerso = -1;
			int bookId = 0, chapter = 0;
			List<String> auxbk = null;
			List<List<String>> bks = new ArrayList<>();
			for (String linea : Files.readAllLines(new File("D:\\Desarrollo\\versPrev.txt").toPath(), 
				StandardCharsets.UTF_8)) {
				if(linea.startsWith(">>>>>>")) {
					auxbk = new ArrayList<>();
					bks.add(auxbk);
				}

				auxbk.add(linea);
			}

			for(List<String> h : bks) {
				for (String linea : h) {
					if(linea.startsWith(">>>>>>")) {
						bookId = BibleUtil.bookId(linea.substring(6, linea.lastIndexOf(" ")), base);
						chapter = Integer.parseInt(linea.substring(linea.lastIndexOf(" ") + 1));
						continue;
					}

					try {
						int numero = Integer.parseInt(linea.trim());
						if (numeroVerso != -1) {
							versiculos.add(new RecordVo(bookId, 0, chapter, null, numeroVerso, 
								textoVerso.toString().trim()));
						}
						numeroVerso = numero;
						textoVerso = new StringBuilder();
					} catch (NumberFormatException e) {
						textoVerso.append(linea).append(" ");
					}
				}

				if (numeroVerso != -1) {
					versiculos.add(new RecordVo(bookId, 0, chapter, null, numeroVerso, 
								textoVerso.toString().trim()));
				}
			}

			createVerses(versiculos, base);
			for (RecordVo versiculo : versiculos) {
				writer.println(versiculo);
			}
			System.out.println("Versos creados exitosamente");
		}
	}

    public static void read(String folder) throws SQLException, IOException {
		String base = "DHH";
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

	public static void createVerses(List<RecordVo> verses, String base) throws SQLException {
		try (Connection conn = db.connection(base)) {
			conn.setAutoCommit(false);
			int z = max("verse", base) + 1;
			String sql = "insert into verse (id, book_id, chapter, verse, text) values (?, ?, ?, ?, ?)";
			int i = 0;
			try (PreparedStatement st = conn.prepareStatement(sql)) {
				for (RecordVo o : verses) {
					st.setInt(1, z++);
					st.setInt(2, o.getBookId());
					st.setInt(3, o.getChapterId());
					st.setInt(4, o.getVerse());
					st.setString(5, o.getText());
					
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
			//read("D:\\Desarrollo\\books");
			//loadBible("NBLA");
			loadBibleCatolic("Latinoamericana", true);
            //importBookYouversion("NBLA", false);
			//importBookBibleGateway("NBLA");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
