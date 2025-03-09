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
}
