package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class PatristicaUtil extends AutoresService {

    public static void main(String[] args) {
        try {
			String base = "Patristica";
			for(String name : Arrays.asList("Rufino de Aquileya - Comentario al símbolo apostólico"))
            	read("D:\\Libros\\Patristica\\" + name + ".docx", base, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
