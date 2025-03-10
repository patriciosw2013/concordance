package com.concordance.services.vo.bible;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NoteBibleVo {

    private int bookId;
    private int chapterId;
    private int verse;
    private int type;
    private String text;
}
