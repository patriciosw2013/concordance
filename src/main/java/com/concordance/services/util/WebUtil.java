package com.concordance.services.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.concordance.services.vo.ItemVo;

public class WebUtil {
    
    public static String readURL(String url, String charset) throws MalformedURLException, IOException {
        try(Scanner sc = new Scanner(new URL(url).openStream(), charset).useDelimiter("\\A")) {
            return Jsoup.parse(sc.next()).wholeText();
        }
    }

    public static void readSumaTeologica() throws MalformedURLException, IOException {
        /*https://hjg.com.ar/sumat/a/c119.html 1-119  Suma teológica - Parte Ia - Cuestión 
            * https://hjg.com.ar/sumat/b/c114.html 1-114   II-I    Suma teológica - Parte I-IIae - Cuestión 114
            * https://hjg.com.ar/sumat/c/c189.html 1-189   II-II   Suma teológica - Parte II-IIae - Cuestión 1
            * https://hjg.com.ar/sumat/d/c90.html 1-90 III Suma teológica - Parte IIIa - Cuestión 1
        */
        List<ItemVo> urls = new ArrayList<>();
        urls.add(new ItemVo(119, "https://hjg.com.ar/sumat/a/c%s.html,Suma teológica - Parte Ia - Cuestión ,PARTE I"));
        urls.add(new ItemVo(114, "https://hjg.com.ar/sumat/b/c%s.html,Suma teológica - Parte I-IIae - Cuestión ,PARTE II-I"));
        urls.add(new ItemVo(189, "https://hjg.com.ar/sumat/c/c%s.html,Suma teológica - Parte II-IIae - Cuestión ,PARTE II-II"));
        urls.add(new ItemVo(90, "https://hjg.com.ar/sumat/d/c%s.html,Suma teológica - Parte IIIa - Cuestión ,PARTE III"));
        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
            for(ItemVo o : urls) {
                String[] aux = o.getValor().split(",");
                writer.println(aux[2]);

                for(int i = 1; i <= o.getCodigo(); i++) {
                    String txt = readURL(String.format(aux[0], i), StandardCharsets.ISO_8859_1.name());
                    int index = txt.indexOf(aux[1] + i);
                    txt = txt.substring(index);
                    int indexEnd = txt.indexOf("c. " + i);
                    txt = txt.substring(0, indexEnd);
                    txt = TextUtils.quitarSaltosDeLinea(txt);

                    writer.println(txt);
                    writer.println("\n");
                }
            }
        }
    }

    public static String readHTML(String urlString) throws IOException {
        Document doc = Jsoup.connect(urlString).get();
        return doc.html();
    }

    public static String readWeb(String url, String tag, boolean pretty) throws IOException {
        Document document = Jsoup.parse(readHTML(url));
        Element div = document.select(tag).first();
        return pretty ? div.outerHtml() : div.html();
    }

    public static String formatHtml(String in) {
        return Jsoup.parse(in).wholeText();
    }

    public static void main(String[] args) {
        try {
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
