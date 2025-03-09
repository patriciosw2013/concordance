package com.concordance.services.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Paragraph implements Comparable<Paragraph> {

	private String chapter;
	private int chapterId;
	private int verse;
	private String text;

	public Paragraph(int verse, String text) {
		this.verse = verse;
		this.text = text;
	}

	public Paragraph(String chapter, int verse, String text) {
		this.chapter = chapter;
		this.verse = verse;
		this.text = text;
	}

	public String toString() {
		return "(" + chapter + " - "+ verse + ": " + (text.length() < 0 ? text.substring(0, 20) : text) + ")";
	}

	@Override
	public int compareTo(Paragraph x) {
		if(x.getChapter() != null) {
			return this.getChapter().compareTo(x.getChapter());
		}

		return 0;
	}
}
