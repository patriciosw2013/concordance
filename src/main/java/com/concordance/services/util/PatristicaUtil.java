package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class PatristicaUtil extends AutoresService {

    public static void main(String[] args) {
        try {
			String base = "Patristica";
			for(String name : Arrays.asList("Agustin de Hipona - El don de la perseverancia"))
            	read("D:\\Libros\\Patristica\\" + name + ".docx", base, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
