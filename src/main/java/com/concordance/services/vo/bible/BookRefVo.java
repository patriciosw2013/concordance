package com.concordance.services.vo.bible;

import java.util.List;

import com.concordance.services.vo.Verse;

import lombok.Data;

@Data
public class BookRefVo {

    private String ref;
    private String texts;
    private List<Verse> verses;
    private String footNotes;
    private String crossRefs;
}
