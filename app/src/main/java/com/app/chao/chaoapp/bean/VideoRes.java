package com.app.chao.chaoapp.bean;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Description:
 */
public class VideoRes {
    public
    @SerializedName("list")
    List<VideoType> list;
    public String title;
    public String score;
    public String videoType;
    public String region;
    public String airTime;
    public String director;
    public String actors;
    public String pic;
    public String description;
    public String smoothURL;
    public String SDURL;
    public String HDURL;

    public String getVideoUrl() {
        if (!TextUtils.isEmpty(HDURL))
            return HDURL;
        else if (!TextUtils.isEmpty(SDURL))
            return SDURL;
        else if (!TextUtils.isEmpty(smoothURL))
            return smoothURL;
        else
            return "";
    }
}
