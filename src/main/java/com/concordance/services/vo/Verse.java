package com.concordance.services.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Verse {

	private int chapter;
	private int verse;
	private String text;
}
