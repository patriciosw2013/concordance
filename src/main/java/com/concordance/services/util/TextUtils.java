package com.concordance.services.util;

import java.io.PrintWriter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.concordance.services.vo.TextExtractedVo;

public class TextUtils {

	public static boolean isEmpty(String in) {
		if(in == null) return true;

		return in.trim().length() == 0;
	}

	public static String value(String in, String option) {
		return isEmpty(in) ? option : in;
	}

	public static int extractNumber(String in) {
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(in);
		if(m.find())
			return Integer.parseInt(m.group());
        
		return 0;
	}

	public static int indexOf(String in) {
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(in);
		if(m.find())
			return m.end();
        
		return 0;
	}

	public static List<Integer> extractNumbers(String in) {
		if(in == null)
			return new ArrayList<>();

        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(in);
		List<Integer> res = new ArrayList<>();
		if(m.find()) {
			for(int i = 1; i <= m.groupCount(); i++)
				res.add(Integer.parseInt(m.group(i)));
		}
		return res;
	}

	public static String quitarEspaciosYTildes(String texto) {
        String sinEspacios = texto.replace(" ", "");
        String sinTildes = Normalizer.normalize(sinEspacios, Normalizer.Form.NFD);
        sinTildes = sinTildes.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        return sinTildes;
    }

	public static String quitarAcentos(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD);
        sinTildes = sinTildes.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        return sinTildes;
    }

	public static List<String> segmentos(String in, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(in);
		List<String> segs = new ArrayList<>();
        if (matcher.matches()) {
			for(int i = 0; i < matcher.groupCount(); i++) {
				segs.add(matcher.group(i));
			}
        }

		return segs;
	}

	public static String grupo(String in, String regex, int grupo) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(in);
        if (matcher.matches())
			return matcher.group(grupo);

		return null;
	}

	public static String lpad(String input, String relleno) {
        if (input != null && input.length() < 2) {
            return relleno + input;
        }
        return input;
    }

	public static String eliminarLineasBlancas(String input) {
        return Arrays.stream(input.split("\n"))
                .filter(linea -> !linea.trim().isEmpty())
                .collect(Collectors.joining("\n"));
    }

	public static String textBetween(String str, String lim1, String lim2) {
		Pattern pattern = Pattern.compile(String.format("\\%s(.*?)\\%s", lim1, lim2));
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return matcher.group(1);
		}

		return null;
	}
	
	public static List<String> texts(String str, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		List<String> res = new ArrayList<>();
		while (matcher.find()) {
			res.add(matcher.group());
		}

		return res;
	}

	public static String textBetween(String str, String lim1, String lim2, int indexFrom) {
		Pattern pattern = Pattern.compile(String.format("\\%s(.*?)\\%s", lim1, lim2));
		Matcher matcher = pattern.matcher(str);
		if (matcher.find(indexFrom)) {
			return matcher.group(1);
		}

		return null;
	}

	public static int indexOf(String str, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return matcher.start();
		}

		return -1;
	}

	public static boolean phraseEqual(String in1, String in2) {
		Map<String, String> rep = new HashMap<>();
		rep.put(" ", "");
		rep.put("á", "a");
		rep.put("é", "e");
		rep.put("í", "i");
		rep.put("ó", "o");
		rep.put("ú", "u");
		rep.put(";", "");
		rep.put(",", "");
		rep.put("¡", "");
		rep.put("!", "");

		Map<String, String> suf = new HashMap<>();
		suf.put(".", "");
		suf.put(",", "");
		suf.put(";", "");
		suf.put(":", "");

		String aux1 = in1.toLowerCase().trim();
		String aux2 = in2.toLowerCase().trim();

		for (Entry<String, String> e : rep.entrySet()) {
			aux1 = aux1.replaceAll(e.getKey(), e.getValue());
			aux2 = aux2.replaceAll(e.getKey(), e.getValue());
		}

		for (Entry<String, String> e : suf.entrySet()) {
			if (aux1.endsWith(e.getKey()))
				aux1 = aux1.substring(0, aux1.length() - 1);

			if (aux2.endsWith(e.getKey()))
				aux2 = aux2.substring(0, aux2.length() - 1);
		}

		return aux1.equals(aux2);
	}

	public static boolean startWithNumber(String str) {
		return Pattern.compile("^[0-9].*").matcher(str).matches();
	}
	
	public static boolean endsWithNumber(String str) {
		return Pattern.compile(".*[0-9]$").matcher(str).find();
	}
	
	public static int extractNumberPrefix(String str) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return Integer.parseInt(matcher.group());
		}

		return 0;
	}
	
	public static int extractNumberSufix(String str) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(str);
		String res = null;
		while(matcher.find()) {
			res = matcher.group();
		}

		return Integer.parseInt(res);
	}
	
	public static boolean isNumber(String str) {
		return str.matches("\\d+");
	}
	
	public static TextExtractedVo extract(String str, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return new TextExtractedVo(matcher.group(), matcher.start(), matcher.end());
		}
		
		return new TextExtractedVo(null, -1, -1);
	}
	
	public static int[] indexes(String str, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return new int[] {matcher.start(), matcher.end()};
		}
		
		return new int[] {-1,-1};
	}
	
	public static int startWithIndex(String str, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return matcher.start();
		}
		
		return -1;
	}
	
	public static int endWithIndex(String str, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(str);
		if (matcher.find()) {
			return matcher.end();
		}
		
		return -1;
	}

	public static String eliminarEspacios(String texto) {
        String[] lineas = texto.split("\n");
        StringBuilder resultado = new StringBuilder();

        for (String linea : lineas) {
            linea = linea.trim();
            if (!linea.isEmpty()) {
                if (resultado.length() > 0) {
                    resultado.append("\n");
                }
                resultado.append(linea);
            }
        }

        return resultado.toString();
    }

    public static String quitarSaltosDeLinea(String texto) {
        String[] parrafos = texto.split("\n\\s*\n");
        StringBuilder resultado = new StringBuilder();

        for (String parrafo : parrafos) {
            parrafo = parrafo.trim();
            parrafo = parrafo.replaceAll("\r\n", " ").replaceAll("\n", " ").concat("\n");
            parrafo = parrafo.replaceAll(" fff", "").replaceAll("", "");
            if (resultado.length() > 0) {
                resultado.append("");
            }
            resultado.append(parrafo);
        }

        return resultado.toString();
    }
	
	public static void main(String[] args) {
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", "UTF-8")) {
			String txt = FileUtils.readFile("D:\\Desarrollo\\model to unify.txt").stream().collect(Collectors.joining("\n"));
			txt = TextUtils.quitarSaltosDeLinea(txt);

			writer.println(txt);
			writer.println("\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
