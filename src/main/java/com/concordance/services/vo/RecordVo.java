package com.concordance.services.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecordVo {
    
    private int bookId;
    private int recordId;
    private int chapterId;
    private String chapter;
    private int verse;
    private String text;
    private String description;

    public RecordVo(int bookId, int recordId, int chapterId, String chapter, int verse, String text) {
        this.bookId = bookId;
        this.recordId = recordId;
        this.chapterId = chapterId;
        this.chapter = chapter;
        this.verse = verse;
        this.text = text;
    }
}
