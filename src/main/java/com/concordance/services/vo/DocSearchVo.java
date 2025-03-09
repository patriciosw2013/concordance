package com.concordance.services.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocSearchVo {
    
    private String fileName;
    private List<String> fragments;
}
