package com.concordance.services.vo.bible;

import java.util.List;

import com.concordance.services.vo.ItemVo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "txt")
public class CitaVo {

	private int bookId;
	private String book;
	private int chapter;
	private int verseIni;
	private int verseFin;
	private String version;
	
	private String txt;
	private String title;
	private String notes;
	private List<ItemVo> chapters;

	public CitaVo(String version) {
		this.version = version;
	}

	public CitaVo(int bookId, String book, int chapter, int verseIni, int verseFin, String version) {
		this.bookId = bookId;
		this.book = book;
		this.chapter = chapter;
		this.verseIni = verseIni;
		this.verseFin = verseFin;
		this.version = version;
	}

	public String cita() {
		return book + " " + chapter + ":" + verseIni + (version != null ? " ".concat(version) : " RVR1960");
	}

	public String citaSimple() {
		return book + " " + chapter + (verseIni > 0 ? ":" + verseIni : "");
	}
}
