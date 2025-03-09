package com.concordance.services.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResultsVo {
    
    private Book book;
    private ContentVo results;
    private String notes;
    private List<ItemVo> chapters;
}
