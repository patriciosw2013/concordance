package com.concordance.services.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookAuxVo {
    
    private List<String> title;
    private List<Paragraph> texts;
    private List<List<Paragraph>> chapters;
}
