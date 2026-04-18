package com.files.codes.model.anime;

import com.google.gson.annotations.SerializedName;

public class AnimeTitles {
    @SerializedName("en")
    private String en;
    @SerializedName("ja")
    private String ja;
    @SerializedName("vi")
    private String vi;

    public String getEn() { return en; }
    public String getJa() { return ja; }
    public String getVi() { return vi; }

    public void setEn(String en) { this.en = en; }
    public void setJa(String ja) { this.ja = ja; }
    public void setVi(String vi) { this.vi = vi; }

    /** Returns best available title: vi > en > ja */
    public String getBestTitle() {
        if (vi != null && !vi.isEmpty()) return vi;
        if (en != null && !en.isEmpty()) return en;
        if (ja != null && !ja.isEmpty()) return ja;
        return "";
    }
}
