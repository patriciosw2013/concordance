package com.concordance.services.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChapterVo {
    
    private int chapterId;
    private String name;
    private String url;
    private List<Integer> verses;

    public ChapterVo(int chapterId) {
        this.chapterId = chapterId;
        this.verses = new ArrayList<>();
    }

    public ChapterVo(int chapterId, String name, String url) {
        this.chapterId = chapterId;
        this.verses = new ArrayList<>();
        this.name = name;
        this.url = url;
    }

    public void addVerso(int verso) {
        this.verses.add(verso);
    }
}
