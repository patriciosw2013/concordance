package com.concordance.services.vo.video;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoMetadataVo {

    private String title;
    private boolean groupingBy;
    private boolean expositive;
    private boolean reeplaceTitle;
    private String titleKey;
    private List<String> regexReplace;
    private List<String> removeIni;
    private List<String> removeFin;
    private List<String> exceptions;

    public String title(String ini) {
        if(reeplaceTitle) return title;

        if(title != null) {
            return String.format("%s %s", title, ini.toUpperCase());
        } return ini;
    }
}
