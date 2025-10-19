package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class AutoresUtil extends AutoresService {

    public static void main(String[] args) {
        try {
			String base = "Autores";
			for(String name : Arrays.asList("John MacArthur - Biblia de Estudio", "David Guzik - Comentarios"))
            	read("D:\\Libros\\" + name + ".docx", base, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
