package com.concordance.services.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TextExtractedVo {

	private String text;
	private int start;
	private int end;
}
