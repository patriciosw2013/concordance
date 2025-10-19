package com.concordance.services.util;

import java.util.Arrays;

import com.concordance.services.AutoresService;

public class PatristicaUtil extends AutoresService {

    public static void main(String[] args) {
        try {
			String base = "Patristica";
            boolean sim = false;
			for(String name : Arrays.asList("Gregorio de Nisa - La gran catequesis",
                "Tertuliano de Cartago - Sobre la oración",
                "Tertuliano de Cartago - Apologético", 
                "Basilio de Cesárea - A los jóvenes", 
                "Basilio de Cesárea - Exhortación a un hijo espiritual",
                "Juan Crisóstomo - Homilías II"))
            	read("D:\\Libros\\Patristica\\" + name + ".docx", base, sim);
            
            //NotesUtil.loadNotesFile("Patristica", sim);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
