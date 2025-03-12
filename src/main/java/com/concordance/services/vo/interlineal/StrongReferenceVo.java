package com.concordance.services.vo.interlineal;

import lombok.Data;

@Data
public class StrongReferenceVo {

    private int verseId;
    private int strongId;
    private String word;
    private String type;
    private String meaning;
    private String reference;
}
