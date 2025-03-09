package com.concordance.services.vo.bible;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CitaVo {

	private int bookId;
	private String book;
	private int chapter;
	private int verseIni;
	private int verseFin;
	private String version;

	public String cita() {
		return book + " " + chapter + ":" + verseIni + " RVR1960";
	}
}
