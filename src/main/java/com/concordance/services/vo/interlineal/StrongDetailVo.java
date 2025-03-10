package com.concordance.services.vo.interlineal;

import java.util.List;

import lombok.Data;

@Data
public class StrongDetailVo {

    private int strongId;
    private String language;
    private String defGlobal;
    private List<String[]> details;
}
