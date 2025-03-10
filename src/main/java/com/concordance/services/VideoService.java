package com.concordance.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.concordance.services.util.BibleUtil;
import com.concordance.services.util.ListUtils;
import com.concordance.services.vo.ItemIntVo;
import com.concordance.services.vo.video.SubtitleVo;
import com.concordance.services.vo.video.VideoMetadataVo;

public class VideoService {

    private static int cont;
	private static Map<String, VideoMetadataVo> metas;

	static {
		metas = new HashMap<String, VideoMetadataVo>();
		metas.put("subtitles-baruc", VideoMetadataVo.builder()
			.title("EXPOSICION")
			.groupingBy(true)
			.expositive(true)
			.regexReplace(Arrays.asList("^(.*)aquí está baruk y la lección de hoy ", 
				"^(.*)está baruk y la lección de \\[Música\\] ",
				"^(.*)y la lección de \\[Música\\] hoy ",
				"^(.*)y la lección de hoy \\[Música\\]",
				"^(.*)aquí está barud y la lección de hoy",
				"^(.*)estaba luz y la lección de hoy",
				"^(.*)Ruth y la lección de hoy",
				" te invitamos a seguir nuestros estudios cada semana por este canal y por el portal.*$",
				" Esperamos que te hayas edificado con el mensaje de hoy.*$"))
			.removeIni(Arrays.asList("aquí está barón y la lección de hoy [Música]", "aquí está baruc y la lección de hoy [Música]",
				"what you believe that create ", "nery times week ", "yuyu jefe del raid profedet ", "we had ", "johnny wright ",
				"very important a ", "when you here ", "many people when the ear ", "uy bienestar y netbook ians ",
				"aquí está barón y la lección de hoy", "aquí está baruc y la lección de hoy", "Aquí está Baruc y la lección de hoy"))
			.removeFin(Arrays.asList("esperamos que se haya beneficiado del mensaje", "esperamos que te hayas beneficiado del mensaje",
				"esperamos que te hayas edificado con el mensaje")).build());
		metas.put("subtitles-liendo", VideoMetadataVo.builder()
			.title("MENSAJE")
			.titleKey(" - ")
			.reeplaceTitle(true)
			.regexReplace(Arrays.asList("\\[Música\\] ", "\\[Música\\]", "\\[Aplausos\\] ")).build());
		metas.put("subtitles-stanley", VideoMetadataVo.builder()
			.title("MENSAJE")
			.titleKey("-")
			.regexReplace(Arrays.asList("^(.*)Dr. Stanley:", "^(.*)Dr. Charles Stanley:", "^(.*)hoy en el programa en contacto",
				"si este programa ha sido de bendición para usted.*$",
				"locutor: Si este programa ha sido de bendición para usted.*$",
				"\\[música\\] ", "\\[Música\\] ", "\\[música\\]", "\\[Música\\]",
				"^(.*)comienza una serie (.*) el mensaje de hoy ",
				"^comience su día con el devocional gratuito en contacto suscríbase hoy mismo",
				"^(.*)hoy en el programa 'en contacto'"))
			.reeplaceTitle(true)
			.exceptions(Arrays.asList("3-DVoKSzUqs", "6vaGzQoa5Yo", "6dindXWhaxE", "64_X1OthKPk", "dLX0TeNKwS8", "HOQYXVhI9QQ",
				"x9Hv9HyJiKs", "nMEVNWTMp7g", "UvpbJienzeg", "jp-YnMcXCsU", "Q-14xcPOgak", "tgd1dnIKc2U", "fMo3x-Jz4PY", "3_3XyowJdKE", 
				"sf305tRwuIg", "LKqGcUvkN3o", "xC3Mwyy2g3k", "4efocWotygw", "FuhOgQwNlr4", "U7glXPyAKAQ", "gAtvsvs1pHs", "OQHz07Ncv-I", 
				"9gqm4-Wg1z4", "DHwTtX7gA9M", "4haYcNEqoSk", "XlZn1Uto11I", "RMqJZphg7SE", "NudxZzfu0Mw", "mdQ8gqdO4T0", "MyAHGeoPhhY",
				"e0Fi5Pegwks", "Ofs8j4oZosk", "z6UIRKBGzek", "Xc8PnoTGhTQ", "x7pi6UyUSQo", "aaBIa908Xws", "0SN6RvoyXvM", "MmAekzVWudc", 
				"kiREM1y1RjE", "NVyogwWA0nk", "vFsWvfd5_ek", "aZkDp51VAMc", "lDogYxWR42A", "ns3aejSXZ_g", "auV0ZQzks1Q", "K2myZaEbBDU",
				"s4NxDoyLQ48", "4SNrikZvfKE", "iKO60nah57I", "7vprSta-TAU", "x5h_e7zT8gQ", "YbQzs4-EfQ4", "slzQpWytAhY", "LpAsM6sejzc", 
				"YN1S33-ps9E", "OcfoEv_8Uv0", "GuKlhOmFeyQ", "hiSlE_LQnEE", "HyOErr1zXF0", "4APAY-KEydE", "GwyAjle2LgI", "hRLa68vpoms",
				"s9zA5d3hI70", "kS1iRFFIGyA", "9JAhbYUE6H8", "dIZzTAiHf1s", "_bFvdiPIl8Q", "pHne0puTDhM", "nlGpkREyHe8", "6xsJy38-coU", 
				"KZxe-aSzxZ4", "rHnvdkiRExw", "Kh1IalkA3Tw", "kMvt5AkwBMI", "PfdvVp3POmc", "jU23O_GE3-Y", "i4ZN5NnUhe8", "N29qaj_-ii4", 
				"eh8lJRPuctY", "Jydvm7VuAzk", "YXoRArJLSks", "vtXj5co_lWM", "0yXxIv96gBo", "_LjJQgw9biE", "kjgQuk1QGyg", "cyuyp4JQtb8", 
				"TxtsnQkP1OI", "9_a5bO6wABA", "XVx9yMwNKGI", "HWGIGmQiF-E", "stdqFohYlvw", "TMLes31l6lI", "JfHzU_69Z4U", "kg3W7Mg48MM", 
				"B9pwVv0FlhY", "0hjPbnBfrjQ", "JyCzY7QqMSI", "TKLn2DCTD_s", "A0A_a3mkaeY", "gPZhw7I17sc", "zIHBcdYC3BE", "6N3KqbvvFf0",
				"lNOjfODqOzA", "Ha9fX9B0g7Q", "cbVrtr_ypgo", "TlExBsNYwDE", "LTWPbMSUd8w", "ZWzYBwXkkiE", "WjZG-dNlCvY", "n4JhdnoKHfw", 
				"Jal0PQaa-js", "nfVEV9sRm7E", "jf4nThIYyY0", "xwqyT5ZQjaM", "odzFdAw_ejE", "6mY7rsm0TCY", "xzhpp-ZAg4M", "i2aczbb8fh0", 
				"18f5BsCnW_8", "_G2C1V6ejN8")).build());
	}

	@Deprecated
    public static List<String> readSRTFile(Path path) throws IOException {
		List<String> txts = Files.readAllLines(path);
		String res = txts.subList(2, txts.size() - 1).stream()
				.filter(o -> !o.matches("^\\d+$") && !o.contains("-->") && !o.isEmpty())
				.collect(Collectors.joining(" "));
 
		return Arrays.asList(path.getFileName().toString(), txts.get(0), txts.get(1), res);
	}

	@Deprecated
	public static List<List<String>> unifySRT(String directory) throws IOException {
		List<List<String>> result = new ArrayList<List<String>>();
		Files.walk(new File(directory).toPath())
			.filter(i -> i.toString().endsWith(".srt"))
			.forEach(z -> {
			try {
				result.add(readSRTFile(z));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		return result;
	}

	public static List<List<String>> readSRT(String srt) throws IOException {
		List<List<String>> result = new ArrayList<List<String>>();
		List<List<String>> init = ListUtils.split(Files.readAllLines(new File(srt).toPath()), "SUBTITLE_SEPARATOR");
		for (List<String> txts : init) {
			String res = txts.subList(3, txts.size() - 1).stream()
					.filter(o -> !o.matches("^\\d+$") && !o.contains("-->") && !o.isEmpty())
					.collect(Collectors.joining(" "));
	
			result.add(Arrays.asList(txts.get(0), txts.get(1), txts.get(2), res));
		}

		return result;
	}

    private static ItemIntVo extractNumbers(String in) {
        String regex = "(\\d+(?!\\sTim|\\sTes|\\sCor))(?:\\s\\w+\\s(\\d+))?";
        Pattern pattern = Pattern.compile(regex); //, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(in);
        int chp = 0, pt = 0;
        while (matcher.find()) {
            if(chp == 0) chp = Integer.parseInt(matcher.group(1));
            else pt = Integer.parseInt(matcher.group(1));
            if(matcher.group(2) != null)
                pt = Integer.parseInt(matcher.group(2));
        }
        return new ItemIntVo(chp, pt);
    }

    public static List<SubtitleVo> processSubtitles(String dir, VideoMetadataVo meta) throws IOException, SQLException {
        List<SubtitleVo> result = new ArrayList<>();
        List<List<String>> res = readSRT(dir);
        for (List<String> x : res) {
            String url = x.get(0);
            String title = x.get(1);
            String desc = x.get(2);
            String text = x.get(3);

            text = text.replaceAll(" ", " ");
            text = text.replaceAll("  ", " ");
            text = text.replaceAll("  ", " ");
            text = text.replaceAll("  ", " ");

			if(meta.getRegexReplace() != null)
				for(String rg : meta.getRegexReplace())
					text = text.replaceAll(rg, "");

			if(meta.getRemoveIni() != null) {
				for(String o : meta.getRemoveIni()) {
					int idx = text.indexOf(o);
					if(idx > 0)
						text = text.substring(idx + o.length());
				}
			}

			if(meta.getRemoveFin() != null) {
				for(String o : meta.getRemoveFin()) {
					int idx = text.indexOf(o);
					if(idx > 0)
						text = text.substring(0, idx);
				}
			}
            ItemIntVo nums = meta.isGroupingBy() ? extractNumbers(desc) : new ItemIntVo(0, 0);
            result.add(new SubtitleVo(meta.isExpositive() ? BibleUtil.bookId(title, "RVR1960") : 0, 
				url, title, desc, nums.getCodigo(), nums.getValor(), text));
        }

        return result;
    }

	public static void generateReadingText(String fileOrigin) throws IOException, NumberFormatException, SQLException {
		String path = "D:\\Desarrollo\\output-yt\\" + fileOrigin + ".srt";
		VideoMetadataVo meta = metas.get(fileOrigin);
		if(meta == null)
			throw new RuntimeException("No tiene metadata");

		Set<String> exceptions = meta.getExceptions() != null ? new HashSet<>(meta.getExceptions()) : new HashSet<>();
		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", StandardCharsets.UTF_8.name())) {
			if(meta.isGroupingBy()) {
				Map<String, List<SubtitleVo>> res = new LinkedHashMap<>();
				processSubtitles(path, meta).stream().filter(i -> !exceptions.contains(i.getUrl())).sorted().forEach(o -> {
					res.computeIfAbsent(meta.title(o.getTitulo()), k -> new ArrayList<>()).add(o);
				});

				for(Entry<String, List<SubtitleVo>> x : res.entrySet()) {
					writer.println(x.getKey());
					for(SubtitleVo o : x.getValue()) {
						String url = "https://www.youtube.com/watch?v=" + o.getUrl();
						
						writer.println(String.format("CAPÍTULO %s", o.getChapter()) + (o.getPart() > 0 ? " P." + o.getPart() : ""));
						writer.println(o.getDescription());
						writer.println(String.format("URL video: <a href=\"%s\" target=\"blank\">%s</a>", url, url));
						writer.println(o.getTextos().trim());
						writer.println();
					}
				}
			} else {
				cont = 1;
				processSubtitles(path, meta).stream().filter(i -> !exceptions.contains(i.getUrl())).sorted().forEach(o -> {
					String url = "https://www.youtube.com/watch?v=" + o.getUrl(); // fileName.substring(fileName.lastIndexOf("-") + 1, fileName.length() - 4);

					String title = meta.getTitleKey() != null ? meta.getTitle() + " " + (cont++) + ". " + 
						o.getDescription().substring(0, o.getDescription().indexOf(meta.getTitleKey())) : 
						meta.title(o.getTitulo())  + " " + cont++;
					
					writer.println(title.trim());
					writer.println(o.getDescription());
					writer.println(String.format("URL video: <a href=\"%s\" target=\"blank\">%s</a>", url, url));
					writer.println(o.getTextos().trim());
					writer.println();
				});
			}
		}
	}

	public static void main(String[] args) {
		try {
			generateReadingText("subtitles-baruc");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
