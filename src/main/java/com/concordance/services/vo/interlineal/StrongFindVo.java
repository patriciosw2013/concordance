package com.concordance.services.vo.interlineal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StrongFindVo {

    private int codigo;
    private String word;
    private String meaning;
    private int testamentId;
}
