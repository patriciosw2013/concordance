package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class AutoresUtil extends AutoresService {

    public static void main(String[] args) {
        try {
			String base = "Concilios";
			for(String name : Arrays.asList("Denzinger"))
            	read("D:\\Libros\\" + name + ".docx", base, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
