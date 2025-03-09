package com.concordance.services.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ChapterVo {
    
    private int chapterId;
    private List<Integer> verses;

    public ChapterVo(int chapterId) {
        this.chapterId = chapterId;
        this.verses = new ArrayList<>();
    }

    public void addVerso(int verso) {
        this.verses.add(verso);
    }
}
