package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class AutoresUtil extends AutoresService {

    public static void main(String[] args) {
        try {
			String base = "Concilios";
			for(String name : Arrays.asList("Concilios de la Iglesia Católica Romana", "Concilios Ecuménicos"))
            	read("D:\\Libros\\" + name + ".docx", base, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
