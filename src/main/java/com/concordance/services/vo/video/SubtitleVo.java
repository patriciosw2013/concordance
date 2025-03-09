package com.concordance.services.vo.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@AllArgsConstructor
@ToString(exclude = "textos")
public class SubtitleVo implements Comparable<SubtitleVo> {

    private int book_id;
    private String url;
    private String titulo;
    private String description;
    private int chapter;
    private int part;
    private String textos;

    @Override
    public int compareTo(SubtitleVo x) {
        int c = new Integer(this.book_id).compareTo(new Integer(x.book_id));
        if(c == 0)
            c = this.getTitulo().compareTo(x.titulo);
        if(c == 0)
            c = new Integer(this.chapter).compareTo(new Integer(x.chapter));
        if(c == 0)
            c = new Integer(this.part).compareTo(new Integer(x.part));
        if(c == 0)
            c = this.getDescription().compareTo(x.getDescription());
        
        return c;
    }
}
