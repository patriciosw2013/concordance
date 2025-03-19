package com.concordance.services.vo.interlineal;

import lombok.Data;

@Data
public class StrongVo {

    private int strongId;
    private String definition;
    private String defGlobal;
    private String language;
    private String word;
    private String def;
    private String defAlterna;
    private String type;
    private String deriva;
    private String defRV;
}
