package com.app.chao.chaoapp.bean;

import java.io.Serializable;

/**
 * Description: 影片详情
 */
public class VideoInfo implements Serializable, Cloneable {
    public String title;
    public String pic;
    public String dataId;
    public String score;
    public String airTime;
    public String moreURL;
    public String loadType;
    //    public String description;

    @Override
    public VideoInfo clone() {
        VideoInfo stu = null;
        try {
            stu = (VideoInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return stu;
    }

}
