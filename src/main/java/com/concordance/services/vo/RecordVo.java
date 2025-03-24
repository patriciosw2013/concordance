package com.concordance.services.vo;

import java.util.List;

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
    private List<ItemVo> notes;

    public RecordVo(int bookId, int chapterId, int verse) {
        this.bookId = bookId;
        this.chapterId = chapterId;
        this.verse = verse;
    }

    public RecordVo(int bookId, int recordId, int chapterId, String chapter, int verse, String text) {
        this.bookId = bookId;
        this.recordId = recordId;
        this.chapterId = chapterId;
        this.chapter = chapter;
        this.verse = verse;
        this.text = text;
    }
}
