package com.concordance.services.vo;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AutorVo {
    
    private String autor;
    private Map<String, List<ItemVo>> books;
}
