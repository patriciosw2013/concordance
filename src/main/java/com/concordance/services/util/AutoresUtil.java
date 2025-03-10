package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class AutoresUtil extends AutoresService {
    
    /*public static void read(String path, boolean sim) throws SQLException, IOException {
        String bookName = FilenameUtils.removeExtension(new File(path).getName());
		System.out.println("Creando libro: " + bookName);
		BookMetadata meta = complex.get(bookName);
		List<Book> books = FileUtils.splitDocx(bookName, path, meta);
		if(meta != null) {
			if(meta.isChapter())
				books = FileUtils.chapterText(books, meta);
			else if(meta.isVerses())
				books = FileUtils.verseText(books, meta);
		}

		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			for(Book b : books) {
				System.out.println(b);

				System.out.println("==========");

				writer.println(b);
				writer.println("========");
				for(Paragraph p : b.getParagraphs())
					writer.println(p);
			}
		}

		if(sim) return;

        String base = "Autores";
		int total = books.size();
		int cont = 1;
        for(Book b : books) {
			boolean exist = existBook(b.getName(), b.getParent(), base);
			boolean chps = b.getParagraphs().stream().filter(i -> !TextUtils.isEmpty(i.getChapter())).count() > 0;
            try (Connection conn = db.connection(base)) {
				if(exist) {
					System.out.println("Actualizando: " + b.getParent() + ": " + b.getName() + " " + (cont++) + " de " + total);
					b.setId(bookId(b.getName(), b.getParent(), base));
					deleteVerses(b.getId(), base);
					deleteChapters(b.getId(), base);
				} else {
					System.out.println("Insertando: " + b.getName() + " " + (cont++) + " de " + total);
					b.setId(max("book", base) + 1);
					String sql = "insert into book (id, name, parent, title, autor) values (?, ?, ?, ?, ?)";
					try (PreparedStatement st = conn.prepareStatement(sql)) {
						st.setInt(1, b.getId());
						st.setString(2, b.getName());
						st.setString(3, b.getParent());
						st.setString(4, b.getTitle());
						st.setString(5, b.getAutor());
						
						st.executeUpdate();
					}
				}

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
						}
						st.executeBatch();
					}
				}
				
				String sql = "insert into verse (id, book_id, text, verse, chapter, chapter_id) values (?, ?, ?, ?, ?, ?)";
				int z = max("verse", base) + 1;
				try (PreparedStatement st = conn.prepareStatement(sql)) {
					for (Paragraph o : b.getParagraphs()) {
						st.setInt(1, z++);
						st.setInt(2, b.getId());
						st.setString(3, o.getText());
						st.setInt(4, o.getVerse());
						st.setString(5, o.getChapter());
						st.setInt(6, o.getChapterId());
						
						st.addBatch();
					}
					st.executeBatch();
				}
			}
		}
		
		System.out.println("Insercion exitosa");
	}*/

    public static void main(String[] args) {
        try {
			String base = "Autores";
			for(String name : Arrays.asList("Fabian Liendo - Predicas", "Baruc Korman - Estudios", "Charles Stanley - Mensajes"))
            	read("D:\\Libros\\" + name + ".docx", base, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
