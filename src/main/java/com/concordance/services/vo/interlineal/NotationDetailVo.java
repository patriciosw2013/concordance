package com.concordance.services.vo.interlineal;

import java.util.List;

import com.concordance.services.vo.ItemStringVo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotationDetailVo {

    private String notation;
    private List<ItemStringVo> detail;
}
