package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class PatristicaUtil extends AutoresService {

    public static void main(String[] args) {
        try {
			String base = "Patristica";
            boolean sim = false;
			for(String name : Arrays.asList("Agustin de Hipona - Confesiones", "Agustin de Hipona - Homilias sobre 1 Juan", "Agustin de Hipona - Sermon de la montaña"))
            	read("D:\\Libros\\Patristica\\" + name + ".docx", base, sim);
            
            NotesUtil.loadNotesFile("Patristica", sim);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
