package com.concordance.services.vo;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString(exclude = {"paragraphs"})
public class Book {

	private int id;
	private String name;
	private String parent;
	private String title;
	private String autor;
	private String destination;
	private String bookDate;
	private List<Paragraph> paragraphs;

	public Book(String parent, String autor) {
		this.parent = parent;
		this.autor = autor;
	}

	public Book(String name, String parent, String autor, String destination, String bookDate) {
		this.name = name;
		this.parent = parent;
		this.autor = autor;
		this.destination = destination;
		this.bookDate = bookDate;
	}
}
