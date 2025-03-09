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
    private int strongId;
    private String word;
    private String type;
    private String meaning;
    private String morfologic;

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
        return strongId + "\n" + word + "\n" + type + "\n" + meaning;
    }
}
