package com.concordance.services.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        for (Element enlace : enlaces) {
            String url = enlace.attr("href");
            String titulo = enlace.text();
            if(rejUrls.contains(url)) continue;

            if(url.contains("contro_fausto") || url.contains("contro_fortunato"))
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
                        frameUrl = resolveUrl(frameUrl, frame.attr("src"));
                        /* Lista de libros de la obra */
                        doc = Jsoup.connect(frameUrl).get();
                        //writer.println(frameUrl);
                        //writer.println(doc.html());
                        links = doc.select("a");
                        for (Element lk : links) {
                            String baseUrlBook = lk.attr("href");
                            frameUrl = resolveUrl(frameUrl, baseUrlBook);
                            doc = Jsoup.connect(frameUrl).get();
                            //writer.println(lk.text());
                            //writer.println(baseUrlBook);
                            //writer.println(frameUrl);
                            //writer.println(doc.html());

                            String urlNotes = null;
                            Element enlace = doc.selectFirst("a[href*=note]");
                            if (enlace != null) {
                                String href = enlace.attr("href");
                                urlNotes = resolveUrl(frameUrl, href.split("#")[0]);
                            }

                            urlsFinal.add(new Book(lk.text(), item.getCodigo(), "Agustin de Hipona", frameUrl, urlNotes));
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
        }

        Map<String, List<Book>> bks = new LinkedHashMap<>();
        for(Book x : urlsFinal) {
            x.setDestination(readURL(x.getDestination(), StandardCharsets.ISO_8859_1.name()));
            x.setBookDate(x.getBookDate() != null ? readURL(x.getBookDate(), StandardCharsets.ISO_8859_1.name()) : null);

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
                    writer.println(String.format("<nombre>%s</nombre>", b.getName()));
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

        /*try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\versPrev.txt", "UTF-8")) {
            List<String> res = Files.readAllLines(new File("D:\\Desarrollo\\preview.txt").toPath(), StandardCharsets.UTF_8);
            List<List<String>> gp = ListUtils.split(res, ">>OBRA");
            for (List<String> o : gp) {
                String obra = o.get(0);
                String subobra = o.get(1);
                String autor = o.get(2);
                writer.println(obra);
                writer.println(subobra);
                writer.println(autor);
                List<List<String>> gps = ListUtils.split(o.subList(3, o.size()), ">>CONTENT");
                writer.println(gps.get(0).subList(10, gps.get(0).size()));
                writer.println(gps.size() > 1 ? gps.get(1) : "SIN NOTAS");
            }
        }*/
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
            readAgustinus();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
