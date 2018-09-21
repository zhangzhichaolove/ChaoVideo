package com.app.chao.chaoapp.bean;

import java.io.Serializable;

/**
 * Created by Chao  2018/9/20 on 16:16
 * description
 */

public class HomeVideoData implements Serializable {
    private String title;
    private String url;
    private String image;
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
