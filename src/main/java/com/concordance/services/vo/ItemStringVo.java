package com.concordance.services.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemStringVo {
    
    private String codigo;
    private String valor;
    private String descripcion;

    public ItemStringVo(String codigo, String valor) {
        this.codigo = codigo;
        this.valor = valor;
    }
}
