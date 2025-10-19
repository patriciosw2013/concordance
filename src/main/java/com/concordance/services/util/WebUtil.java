package com.concordance.services.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.primefaces.shaded.json.JSONArray;
import org.primefaces.shaded.json.JSONObject;

import com.concordance.services.vo.Book;
import com.concordance.services.vo.ItemStringVo;
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

    public static void readAgustinus() throws MalformedURLException, IOException {
        Document doc = Jsoup.connect("https://www.augustinus.it/spagnolo/index.htm").get();
        Elements enlaces = doc.select("a.LinkNero2G");
        List<ItemStringVo> urls = new ArrayList<>();
        Set<String> rejUrls = new HashSet<>();
        rejUrls.add("benvenuto.htm");
        rejUrls.add("links.htm");
        rejUrls.add("../iconografia/index.htm");
        rejUrls.add("copyright.htm");
        rejUrls.add("http://augustinus.it.master.com/texis/master/search/+/form/NuovaGraficaSpagnolo.html");

        Set<String> excep = new HashSet<>();
        excep.add("lettere");
        excep.add("discorsi");
        excep.add("esposizioni_salmi");
        excep.add("commento_vsg");
        excep.add("commento_lsg");
        excep.add("cdd");
        excep.add("confessioni");
        excep.add("ritrattazioni");
        excep.add("montagna");
        excep.add("predestinazione_santi");
        excep.add("trinita");
        excep.add("esposizione_romani");
        excep.add("esposizione_galati");
        excep.add("questioni_ettateuco");
        excep.add("grazia_libero_arbitrio");
        excep.add("dono_perseveranza");
        excep.add("perfezione_giustizia");
        excep.add("castigo_perdono");
        excep.add("questioni_simpliciano");
        excep.add("immortalita_anima");
        excep.add("felicita");
        excep.add("attribuiti");
        for (Element enlace : enlaces) {
            String url = enlace.attr("href");
            String titulo = enlace.text();
            if(rejUrls.contains(url)) continue;

            if(excep.stream().filter(i -> url.contains(i)).count() > 0) continue;

            //if(url.contains("contr_acc"))
            urls.add(new ItemStringVo(titulo, "https://www.augustinus.it/spagnolo/" + url));
        }

        List<Book> urlsFinal = new ArrayList<>();
        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
            for (ItemStringVo item : urls) {
                String baseUrl = item.getValor();
                System.out.println("Procesando obra: " + baseUrl);
                try {
                    doc = Jsoup.connect(baseUrl).get();
                    //writer.println(baseUrl);
                    //writer.println(doc.html());
                    //writer.println();

                    Element frame = doc.selectFirst("frame[name=principale]");
                    String src = frame.attr("src");
                    String frameUrl = resolveUrl(baseUrl, src);
                    System.out.println("OBRA: " + item.getCodigo());
                    
                    doc = Jsoup.connect(frameUrl).get();
                    //writer.println(frameUrl);
                    //writer.println(doc.html());
                    //writer.println();
                    Elements links = doc.select("a");
                    frameUrl = resolveUrl(frameUrl, links.get(1).attr("href"));

                    doc = Jsoup.connect(frameUrl).get();
                    //writer.println(frameUrl);
                    //writer.println(doc.html());
                    //writer.println();
                    frame = doc.selectFirst("frame[name=principale]");
                    frameUrl = resolveUrl(frameUrl, frame.attr("src"));
                    frame = doc.selectFirst("frame[name=sommario]");
                    doc = Jsoup.connect(frameUrl).get();
                    
                    /* Pagina definitiva */
                    //writer.println(frameUrl);
                    //writer.println(doc.html());
                    if(frame != null) {
                        /* Tiene mas de 1 libro, el frame sommario tiene la lista de links */
                        System.out.println("Tiene mas de 1 libro, lista de links");
                        frameUrl = resolveUrl(frameUrl, frame.attr("src"));
                        /* Lista de libros de la obra */
                        doc = Jsoup.connect(frameUrl).get();
                        writer.println(frameUrl);
                        writer.println(doc.html());
                        links = doc.select("a");
                        for (Element lk : links) {
                            String baseUrlBook = lk.attr("href");
                            frameUrl = resolveUrl(frameUrl, baseUrlBook);
                            doc = Jsoup.connect(frameUrl).get();
                            writer.println(lk.text() + ": " + frameUrl);

                            /*String urlNotes = null;
                            Element enlace = doc.selectFirst("a[href*=note]");
                            if (enlace != null) {
                                String href = enlace.attr("href");
                                urlNotes = resolveUrl(frameUrl, href.split("#")[0]);
                            }

                            urlsFinal.add(new Book(lk.text(), item.getCodigo(), "Agustin de Hipona", frameUrl, urlNotes));*/
                            
                            frame = doc.selectFirst("frame[name=principale]");
                            if(frame != null) {
                                /* Tiene estructura de texto y notas */
                                frameUrl = resolveUrl(frameUrl, frame.attr("src"));
                                frame = doc.selectFirst("frame[name=note_pie_pagina]");
                                String urlNotes = frame != null ? resolveUrl(frameUrl, frame.attr("src")) : null;
                                urlsFinal.add(new Book(lk.text(), item.getCodigo(), "Agustin de Hipona", frameUrl, urlNotes));
                            } else {
                                /* No tiene notas */
                                urlsFinal.add(new Book(lk.text(), item.getCodigo(), "Agustin de Hipona", frameUrl, null));
                            }
                        }
                    } else {
                        frame = doc.selectFirst("frame[name=principale]");
                        frameUrl = resolveUrl(frameUrl, frame.attr("src"));
                        frame = doc.selectFirst("frame[name=note_pie_pagina]");
                        String urlNotes = frame != null ? resolveUrl(frameUrl, frame.attr("src")) : null;
                        urlsFinal.add(new Book(null, item.getCodigo(), "Agustin de Hipona", frameUrl, urlNotes));
                    }
                } catch (IOException e) {
                    System.err.println("Error procesando: " + baseUrl);
                    e.printStackTrace();
                }
            }

            for(Book x : urlsFinal)
                writer.println(x);
        }

        Map<String, List<Book>> bks = new LinkedHashMap<>();
        for(Book x : urlsFinal) {
            x.setDestination(readURL(x.getDestination(), StandardCharsets.ISO_8859_1.name()));
            if(x.getBookDate() != null)
                x.setBookDate(x.getBookDate() != null ? readURL(x.getBookDate(), StandardCharsets.ISO_8859_1.name()) : null);
            if(x.getBookDate() != null && x.getBookDate().length() > 0)
                x.setBookDate(x.getBookDate().substring(x.getBookDate().indexOf("1 ")));

            if(bks.containsKey(x.getParent()))
                bks.get(x.getParent()).add(x);
            else {
                List<Book> bs = new ArrayList<>();
                bs.add(x);
                bks.put(x.getParent(), bs);
            }
        }

        try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
            for(Entry<String, List<Book>> x : bks.entrySet()) {
                writer.println("<obra>");
                writer.println(String.format("<parent>%s</parent>", x.getKey()));
                for(Book b : x.getValue()) {
                    writer.println("<book>");
                    writer.println(String.format("<nombre>%s</nombre>", b.getName() != null ? b.getName() : ""));
                    writer.println(String.format("<autor>%s</autor>", b.getAutor()));
                    writer.println(String.format("<contenido>%s</contenido>", b.getDestination()));
                    if(b.getBookDate() != null)
                        writer.println(String.format("<notas>%s</notas>", b.getBookDate()));
                    else writer.println("<notas></notas>");
                    writer.println("</book>");
                }
                writer.println("</obra>");
            }
        }
    }

    private static String resolveUrl(String baseUrl, String relativePath) {
        try {
            return new java.net.URL(new java.net.URL(baseUrl), relativePath).toString();
        } catch (Exception e) {
            System.err.println("Error resolviendo URL: " + baseUrl + " + " + relativePath);
            return relativePath;
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

    public static String readTag(String in, String tag, boolean pretty) throws IOException {
        Document document = Jsoup.parse(in);
        Element div = document.select(tag).first();
        if(div == null) return null;
        return pretty ? div.outerHtml() : div.html();
    }

    public static List<String> readTags(String in, String tag) throws IOException {
        Document document = Jsoup.parse(in);
        Elements divs = document.select(tag);
        List<String> res = new ArrayList<>();
        for (Element span : divs) {
            res.add(span.text());
        }
        return res;
    }
    
    public static List<String> readHtmlTags(String in, String tag) throws IOException {
        Document document = Jsoup.parse(in);
        Elements divs = document.select(tag);
        List<String> res = new ArrayList<>();
        for (Element span : divs) {
            res.add(span.html());
        }
        return res;
    }

    public static List<String> readPrettyHtmlTags(String in, String tag) throws IOException {
        Document document = Jsoup.parse(in);
        Elements divs = document.select(tag);
        List<String> res = new ArrayList<>();
        for (Element span : divs) {
            res.add(span.outerHtml());
        }
        return res;
    }

    public static Elements readNodeTags(String in, String tag) throws IOException {
        Document document = Jsoup.parse(in);
        Elements divs = document.select(tag);
        return divs;
    }

    public static List<List<Node>> readHtmlChilds(String in, String tag) throws IOException {
        Document document = Jsoup.parse(in);
        Elements divs = document.select(tag);
        List<List<Node>> res = new ArrayList<>();
        for (Element span : divs) {
            res.add(span.childNodes());
        }
        return res;
    }

    public static List<String> readTags(String in, String tag, String attr) throws IOException {
        Document document = Jsoup.parse(in);
        Elements divs = document.select(tag);
        List<String> res = new ArrayList<>();
        for (Element span : divs) {
            res.add(span.attr(attr));
        }
        return res;
    }

    public static String extractText(String in) {
        Document document = Jsoup.parse(in);
        for (Node node : document.body().childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                return textNode.text().trim();
            }
        }

        return null;
    }

    public static String replaceLinks(String in) {
        String regex = "(https?://(?:www\\.)?youtube\\.com/watch\\?v=[\\w-]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(in);

        return matcher.replaceAll("<a href=\"$0\" target=\"_blank\">$0</a>");
    }

    public static String formatHtml(String in) {
        return Jsoup.parse(in).wholeText();
    }

    public static String formatHtml(String in, String charset) {
        return Jsoup.parse(in, charset).wholeText();
    }

    public static void main(String[] args) {
        try {
            //readAgustinus();

            /* Crea los word con las obras */
            List<String> res = Files.readAllLines(new File("D:\\Desarrollo\\agustinus.txt").toPath(), StandardCharsets.UTF_8);
            List<String> tags = readHtmlTags(res.stream().collect(Collectors.joining("%%")), "obra");
            List<String> pars = null;
            String dir = "D:\\Libros\\res\\";
            for (String o : tags) {
                pars = new ArrayList<>();
                String parent = readTag(o, "parent", false);
                List<String> obras = readHtmlTags(o, "book");
                String autor = null;
                for (String x : obras) {
                    String nombre = readTag(x, "nombre", false);
                    autor = readTag(x, "autor", false);
                    String contenido = readTag(x, "contenido", false);
                    contenido = contenido.replaceAll("&nbsp;", "");
                    
                    System.out.println(parent+ " " + nombre + " " + autor);
                    pars.addAll(Arrays.asList(contenido.split("%%")));
                }

                FileUtils.crearDocx(pars, dir + autor + " - " + parent + ".docx");
            }

            /* Obtiene las notas */
            try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
                for (String o : tags) {
                    String parent = readTag(o, "parent", false);
                    List<String> obras = readHtmlTags(o, "book");
                    for (String x : obras) {
                        String nombre = readTag(x, "nombre", false);
                        String autor = readTag(x, "autor", false);
                        String notas = readTag(x, "notas", false);
                        if(notas.isBlank()) continue;
                        
                        if(nombre.isBlank())
                            writer.println(">>" + autor + " - " + parent + " - " + parent);
                        else writer.println(">>" + autor + " - " + parent + " - " + nombre);
                        for(String z : notas.split("%%"))
                            writer.println(z);
                        writer.println();
                    }
                }

                writer.println();
                for (String o : tags) {
                    String parent = readTag(o, "parent", false);
                    writer.println("\"" + parent + "\",");
                }
                writer.println();

                for (String o : tags) {
                    String parent = readTag(o, "parent", false);
                    List<String> obras = readHtmlTags(o, "book");
                    for(String x : obras) {
                        String autor = readTag(x, "autor", false);
                        String contenido = readTag(x, "contenido", false);
                        String libro = contenido.contains("LIBRO") ? "\"^(LIBRO).*\"" : "null";
                        String cap = contenido.contains("CAPÍTULO") ? "\"^(CAPÍTULO.*)\"" : "null";
                        String verses = contenido.contains("1. ") ? "\"^(\\\\d+)\\\\..*\"" : "null";
                        String tog = contenido.contains("I. 1.") ? "true" : "false";
                        
                        writer.println(String.format("complex.put(\"%s\",BookMetadata.builder().keySplit(%s).indexTitle(-1).regexChapter(%s).regexVerses(%s).chpTogether(%s).joiningKey(\"\\n\").build());",
                            autor + " - " + parent, libro, cap, verses, tog));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
