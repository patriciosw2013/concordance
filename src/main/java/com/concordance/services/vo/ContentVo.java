package com.concordance.services.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContentVo {

    private int chapterId;
    private String chapter;
    private List<String> contents;
}
