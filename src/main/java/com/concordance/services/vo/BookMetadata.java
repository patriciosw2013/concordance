package com.concordance.services.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class BookMetadata {
    
    private String keySplit;
    private int indexTitle;
    private int indexDestination;
    private int indexDate;
    private boolean chapter;
    private boolean verses;
    private boolean chpTogether;
    private String regexChapter;
    private String regexVerses;
    private String chapterKey;
    private String verseKey;
    private String joiningKey;
    private String extraRegex;

    public BookMetadata() {
        joiningKey = " ";
        this.chapterKey = null;
        this.verseKey = ". ";
        this.regexVerses = "^\\d+(\\. ).*?";
        this.regexChapter = "^(CAPÍTULO).*$";
        this.joiningKey = " ";
    }
}
