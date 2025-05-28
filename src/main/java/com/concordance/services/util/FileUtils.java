package com.concordance.services.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import com.concordance.services.vo.Book;
import com.concordance.services.vo.BookAuxVo;
import com.concordance.services.vo.BookMetadata;
import com.concordance.services.vo.MapVo;
import com.concordance.services.vo.Paragraph;
import com.concordance.services.vo.ParagraphAuxVo;

public class FileUtils {

	public static List<String> readResource(String resource) {
		try {
			Path path = new File(FileUtils.class.getResource(resource).toURI()).toPath();
			return Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static List<String> readFile(String file) {
		try {
			Path path = new File(file).toPath();
			return Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void createFile(String in, String name) throws FileNotFoundException {
		File f = new File(name);
		
		try(PrintWriter s = new PrintWriter(f)) {
			s.write(in);
		}
	}
	
	public static File createTmpFile(InputStream in) throws IOException {
		final File tempFile = File.createTempFile("stream2file", ".tmp");
		tempFile.deleteOnExit();
		try (FileOutputStream out = new FileOutputStream(tempFile)) {
			IOUtils.copy(in, out);
		}
		return tempFile;
	}

	public static List<String> readDocxFile(String path) throws FileNotFoundException, IOException {
		List<String> res = new ArrayList<>();
		try(FileInputStream fis = new FileInputStream(path)) {
			try(XWPFDocument document = new XWPFDocument(fis)) {
				List<XWPFParagraph> paragraphs = document.getParagraphs();
				for (XWPFParagraph par : paragraphs) {
					res.add(par.getText());
				}
			}
		}

		return res;
	}

	public static String readPDFFile(String path) throws IOException {
		PDDocument document = PDDocument.load(new File(path));
		if (document.isEncrypted())
			return "";

		PDFTextStripper stripper = new PDFTextStripper();
		return stripper.getText(document);
	}

	public static List<Book> processDoc(String bookName, String path, BookMetadata meta) throws FileNotFoundException, IOException {
		if(meta == null)
			throw new RuntimeException("Debe ingresar la metadata");

		return ListUtils.splitRegex(readDocxFile(path), meta.getKeySplit(), true).stream().map(i -> {
			Book b = new Book();
			b.setName(meta.getKeySplit() != null ? i.get(0).trim() : bookName);
			if(bookName.indexOf(" - ") == -1) {
				b.setAutor("");
				b.setParent(bookName);
			} else {
				String[] header = bookName.split(" - ");
				b.setAutor(header[0].trim());
				b.setParent(header[1]);

				if(b.getName().equals(bookName))
					b.setName(b.getParent());
			}

			int j = meta.getIndexTitle() + (meta.getKeySplit() == null ? 1 : 2);
			b.setTitle(meta.getIndexTitle() >= 0 ? i.get(meta.getIndexTitle() + 1).trim() : null);

			if(meta.getIndexDate() > 0) {
				b.setBookDate(i.get(meta.getIndexDate() + 1).trim());
				j = Math.max(j, meta.getIndexDate() + 2);
			}

			if(meta.getIndexDestination() > 0) {
				b.setDestination(i.get(meta.getIndexDestination() + 1).trim());
				j = Math.max(j, meta.getIndexDestination() + 1);
			}

			if(meta.getRegexChapter() != null) {
				Map<String, List<String>> chapters = ListUtils.groupByRegex(i.subList(j, i.size()), meta.getRegexChapter());
				if(meta.getRegexVerses() != null) {
					b.setParagraphs(chapters.entrySet().stream().flatMap(z -> {
						List<Paragraph> pars = new ArrayList<>();
						if(!meta.isChpTogether())
							pars.add(new Paragraph(z.getKey(), 0, z.getValue().get(0)));

						pars.addAll(ListUtils.groupBy(z.getValue().subList(meta.isChpTogether() ? 0 : 1, z.getValue().size()), 
							meta.getRegexVerses()).stream().map(c -> new Paragraph(z.getKey(), "".equals(c.getKey()) ? TextUtils.extractNumber(c.getValue().get(0)) : Integer.parseInt(c.getKey()), 
								c.getValue().stream().collect(Collectors.joining(meta.getJoiningKey())))).collect(Collectors.toList()));
						return pars.stream();
					}).collect(Collectors.toList()));
				} else {
					b.setParagraphs(chapters.entrySet().stream().map(z -> new Paragraph(z.getKey(), 0, 
						z.getValue().stream().collect(Collectors.joining(meta.getJoiningKey())))).collect(Collectors.toList()));
				}
			} else if(meta.getRegexVerses() != null) {
				Map<String, List<String>> verses = ListUtils.groupByRegex(i.subList(j, i.size()), meta.getRegexVerses());
				b.setParagraphs(verses.entrySet().stream().map(z -> new Paragraph("", "".equals(z.getKey()) ? 0 : Integer.parseInt(z.getKey()), 
						z.getValue().stream().collect(Collectors.joining(meta.getJoiningKey())))).collect(Collectors.toList()));
			} else {
				b.setParagraphs(i.subList(j + 1, i.size()).stream().map(z -> new Paragraph(0, z)).collect(Collectors.toList()));
			}
			return b;
		}).collect(Collectors.toList());
	}

	/** Devuelve una lista de parrafos divididos en libros que pueden tener una palabra clave para su division */
	public static List<Book> splitDocx(String bookName, String path, BookMetadata meta) throws FileNotFoundException, IOException {
		List<MapVo> res = new ArrayList<>();
		List<String> aux = null;
		if(meta == null)
			throw new RuntimeException("Debe ingresar la metadata");

		if(meta.getKeySplit() == null) {
			aux = new ArrayList<>();
			res.add(new MapVo(bookName, aux));
		} else System.out.println(meta);

		for (String o : readDocxFile(path)) {
			if(o.isEmpty()) continue;
			if(meta.getKeySplit() != null && o.matches(meta.getKeySplit())) {
				aux = new ArrayList<>();
				res.add(new MapVo(o, aux));
			} else {
				aux.add(o);
			}
		}

		return res.stream().map(i -> {
			Book b = new Book();
			b.setName(i.getKey().trim());
			b.setParagraphs(new ArrayList<>());
			if(bookName.indexOf(" - ") == -1) {
				b.setAutor("");
				b.setParent(bookName);
			} else { 
				b.setAutor(bookName.substring(0, bookName.indexOf(" -")).trim());

				if(b.getName().equals(bookName)) {
					b.setParent(bookName.substring(bookName.indexOf(" -") + 3));
					b.setName(b.getParent());
				} else b.setParent(bookName.substring(bookName.indexOf(" -") + 3));
			}

			if(meta != null && meta.getIndexTitle() >= 0)
				b.setTitle(i.getValue().get(meta.getIndexTitle()).trim());

			int j = meta != null ? meta.getIndexTitle() : 0;
			if(meta != null && meta.getIndexDate() > 0) {
				b.setBookDate(i.getValue().get(meta.getIndexDate()).trim());
				j = Math.max(j, meta.getIndexDate());
			}

			if(meta != null && meta.getIndexDestination() > 0) {
				b.setDestination(i.getValue().get(meta.getIndexDestination()).trim());
				j = Math.max(j, meta.getIndexDestination());
			}

			for(int o = j + 1; o < i.getValue().size(); o++) {
				String txt = i.getValue().get(o);
				if(TextUtils.isEmpty(txt)) continue;
				b.getParagraphs().add(new Paragraph(0, txt));
			}

			return b;
		}).collect(Collectors.toList());
	}

	public static List<Book> verseText(List<Book> res, BookMetadata meta) {
		for(Book x : res) {
			BookAuxVo aux = processParagraphs(x.getParagraphs(), meta.getRegexVerses(), null, meta.getVerseKey(), meta.getJoiningKey(), false);

			x.setTitle((TextUtils.isEmpty(x.getTitle()) ? "" : x.getTitle().concat(". "))
					.concat(aux.getTitle().stream().collect(Collectors.joining(" "))));
			x.setParagraphs(aux.getTexts());
			/* Elimina el campo chapter */
			x.getParagraphs().forEach(i -> i.setChapter(null));
		}

		return res;
	}

	private static List<Paragraph> splitParagraphs(List<List<Paragraph>> in, BookMetadata meta) {
		List<Paragraph> aux = null;
		List<List<Paragraph>> pars = null;
		List<Paragraph> res = new ArrayList<>();
		/* Cada lista de parrafos corresponde a 1 capitulo entero */
		for(List<Paragraph> x : in) {
			Paragraph title = x.get(0);
			aux = new ArrayList<>();
			pars = new ArrayList<>();
			pars.add(aux);
			for(Paragraph o : x) {
				if(o.getText().matches(meta.getRegexVerses())) {
					aux = new ArrayList<>();
					aux.add(o);
					pars.add(aux);
				} else {
					aux.add(o);
				}
			}

			for(List<Paragraph> z : pars) {
				if(z.isEmpty()) continue;
				String text = z.get(0).getText();
				res.add(new Paragraph(title.getChapter().trim(), 
					text.matches(meta.getRegexVerses()) ? TextUtils.extractNumber(text) : z.get(0).getVerse(),
					z.stream().map(Paragraph::getText).collect(Collectors.joining(meta.getJoiningKey())).trim()));
			}
		}

		return res;
	}

	public static List<Book> chapterText(List<Book> res, BookMetadata meta) {
		for(Book x : res) {
			BookAuxVo aux = processParagraphs(x.getParagraphs(), meta.getRegexChapter(), meta.getChapterKey(), 
				meta.isChpTogether() ? meta.getVerseKey() : null, meta.getJoiningKey(), meta.isChpTogether());

			x.setTitle((TextUtils.isEmpty(x.getTitle()) ? "" : x.getTitle().concat(". "))
					.concat(aux.getTitle().stream().collect(Collectors.joining(" "))));

			if(meta.isVerses()) {
				x.setParagraphs(splitParagraphs(aux.getChapters(), meta));
			} else {
				x.setParagraphs(aux.getTexts());
			}

			if(meta.getExtraRegex() != null) {
				List<Paragraph> pars = new ArrayList<>();
				Pattern p = Pattern.compile(meta.getExtraRegex());
				for (Paragraph o : x.getParagraphs()) {
					Matcher m = p.matcher(o.getText());
					if(m.matches()) {
						String i = o.getText().substring(0, m.start());
						String j = o.getText().substring(m.start());
						pars.add(new Paragraph(o.getChapter(), o.getVerse(), i));
						pars.add(new Paragraph(o.getChapter(), o.getVerse(), j));
					} else pars.add(o);
				}

				x.setParagraphs(pars);
			}
		}

		return res;
	}

	private static BookAuxVo processParagraphs(List<Paragraph> paragraphs, String regex, String chapterKey, String verseKey, 
		String joiningKey, boolean chpTogether) {
		List<ParagraphAuxVo> process = new ArrayList<>();
		List<String> title = new ArrayList<>();
		List<String> aux = null;
		if(chapterKey == null && verseKey != null) {
			aux = new ArrayList<>();
			process.add(new ParagraphAuxVo(null, 0, 
				aux));
		}

		for(Paragraph z : paragraphs) {
			if(z.getText().matches(regex)) {
				if(aux != null && aux.isEmpty())
					process.clear();

				aux = new ArrayList<>();
				aux.add(z.getText());
				int indexChp = chapterKey != null ? z.getText().indexOf(chapterKey) : 0;
				int indexVr = chapterKey != null && chpTogether ? indexChp + 1 : 0;
				process.add(new ParagraphAuxVo(TextUtils.isEmpty(z.getChapter()) ? indexChp > 0 ? z.getText().substring(0, indexChp) : z.getText() : z.getChapter(), 
					verseKey != null ? TextUtils.extractNumber(z.getText().substring(indexVr)) : 0, 
					aux));
			} else if(aux != null) {
				aux.add(z.getText());
			} else {
				title.add(z.getText());
			} 
		}

		return new BookAuxVo(title, process.stream().map(i -> {
				return new Paragraph(i.getParagraph().getChapter(), 
					i.getParagraph().getVerse(), 
					i.getTexts().stream().collect(Collectors.joining(joiningKey)));
			}).collect(Collectors.toList()), process.stream().map(i -> {
				return i.getTexts().stream().map(j -> new Paragraph(i.getParagraph().getChapter(), 
					i.getParagraph().getVerse(), j)).collect(Collectors.toList());
			}).collect(Collectors.toList()));
	}

	@SuppressWarnings("incomplete-switch")
	public static List<List<String>> readXLS(String path) throws IOException {
		List<List<String>> res = new ArrayList<>();
		try(XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(new File(path)))) {
			XSSFSheet hs = wb.getSheetAt(0);
			Iterator<Row> rowIterator = hs.iterator();
			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				Iterator<Cell> cellIterator = row.cellIterator();
   
				List<String> rws = new ArrayList<>();
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();
					switch (cell.getCellType()) {
					case NUMERIC:
						rws.add((int)cell.getNumericCellValue() + "");
						break;
					case STRING:
						rws.add(cell.getStringCellValue());
						break;
					}
				}
				res.add(rws);
			}
		}

		return res;
	}

	public static void main(String[] args) {
		try {
			/*BookMetadata meta = BookMetadata.builder().keySplit("^(LIBRO).*")
				.indexTitle(0)
				.chapter(true).verses(true)
				.regexChapter("^(CAP \\d+)\\..*").regexVerses("^(?:CAP \\d+\\.\\s)?(\\d+)\\..*")
				.chapterKey(null).verseKey("").chpTogether(false).joiningKey(">>").build();
			try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
				String bookName = "Test";
				for(Book x : splitDocx(bookName, "D:\\Libros\\" + bookName + ".docx", meta)) {
					writer.println(x);
				}
			}*/

			BookMetadata meta = BookMetadata.builder().keySplit("^(LIBRO).*")
				.indexTitle(-1)
				.regexChapter("^(SALMO \\d+.*)").regexVerses("^(\\d+)\\..*")
				.joiningKey("\n").build();
			try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
				String bookName = "Test";
				for(Book x : processDoc(bookName, "D:\\Libros\\Patristica\\" + bookName + ".docx", meta)) {
					for(Paragraph p : x.getParagraphs())
						writer.println(p.getText());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
