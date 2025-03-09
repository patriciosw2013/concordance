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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
			.regexReplace(Arrays.asList("^(.*)aquí está baruk y la lección de hoy "))
			.removeIni(Arrays.asList("aquí está barón y la lección de hoy [Música]", "aquí está baruc y la lección de hoy [Música]",
				"aquí está barón y la lección de hoy", "aquí está baruc y la lección de hoy", "Aquí está Baruc y la lección de hoy"))
			.removeFin(Arrays.asList("esperamos que se haya beneficiado del mensaje", "esperamos que te hayas beneficiado del mensaje",
				"esperamos que te hayas edificado con el mensaje")).build());
		metas.put("subtitles-liendo", VideoMetadataVo.builder()
			.title("MENSAJE")
			.titleKey(" - ")
			.reeplaceTitle(true).build());
		metas.put("subtitles-stanley", VideoMetadataVo.builder()
			.title("MENSAJE")
			.titleKey("-")
			.regexReplace(Arrays.asList("^(.*)Dr. Stanley: ", "\\[música\\] ", "\\[Música\\] "))
			.reeplaceTitle(true).build());
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

		try(PrintWriter writer = new PrintWriter("D:\\Desarrollo\\preview.txt", StandardCharsets.UTF_8.name())) {
			if(meta.isGroupingBy()) {
				Map<String, List<SubtitleVo>> res = new LinkedHashMap<>();
				processSubtitles(path, meta).stream().sorted().forEach(o -> {
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
				processSubtitles(path, meta).stream().sorted().forEach(o -> {
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
