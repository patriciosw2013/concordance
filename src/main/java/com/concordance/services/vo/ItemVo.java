package com.concordance.services.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemVo {
    
    private int codigo;
    private String valor;
    private String descripcion;
    
    public ItemVo(int codigo, String valor) {
        this.codigo = codigo;
        this.valor = valor;
    }
}
