package com.concordance.services.vo;

import java.util.List;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "texts")
public class ParagraphAuxVo {
    
    private Paragraph paragraph;
    private List<String> texts;

    public ParagraphAuxVo(String chapter, int verse, List<String> texts) {
        this.paragraph = new Paragraph(chapter, verse, null);
        this.texts = texts;
    }
}
