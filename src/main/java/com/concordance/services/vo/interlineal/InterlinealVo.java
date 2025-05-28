package com.concordance.services.vo.interlineal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterlinealVo {

    private int bookId;
    private int chapter;
    private int verse;
    private int wordId;
    private int strongId;
    private String word;
    private String language;
    private String type;
    private String meaning;
    private String morfologic;
    private NotationVo notation;

    public InterlinealVo(int bookId, int chapter, int verse, int strongId, String word, String type, String meaning) {
        this.bookId = bookId;
        this.chapter = chapter;
        this.verse = verse;
        this.strongId = strongId;
        this.word = word;
        this.type = type;
        this.meaning = meaning;
    }

    public String txt() {
        return "<span style=\"color: #007ad9;\">" + strongId + "</span>\n" + word + 
            "\n<span style=\"font-weight: bold; color: #007ad9;\">" + type + 
            "</span>\n<span style=\"color: #d9534f;\">" + meaning;
    }
}
