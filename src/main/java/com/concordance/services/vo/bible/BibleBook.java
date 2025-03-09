package com.concordance.services.vo.bible;

import java.util.List;

import com.concordance.services.vo.Verse;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"verses"})
public class BibleBook {

	private int key;
	private int testament;
	private String abbr;
	private String title;
	private List<Verse> verses;
}
