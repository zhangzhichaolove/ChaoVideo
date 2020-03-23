package com.app.chao.chaoapp.bean;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * {
 * "id": 34,
 * "title": "我脑中的橡皮擦",
 * "star": "4.5",
 * "publicTime": "2004-11-05",
 * "type": "剧情/爱情",
 * "toStar": "李宰汉",
 * "performer": "郑雨盛/孙艺珍/白钟学/金富善/金重基/善智贤",
 * "country": "韩国",
 * "alias": "我脑海中的橡皮擦/拥抱这分钟(港)/Eraser in my head/A moment to remember/Nae meorisokui jiwoogae/내 머리 속의 지우개",
 * "videoDescribe": "　　秀真（孙艺珍饰）是一个富家女，跟男友出走却被抛弃，在便利店内与在建筑公司当工人的哲洙（郑宇成饰）相遇。未来的日子里，他们一次又一次相遇，最终彼此相爱了。\n　　哲洙知道自己与秀真的身份悬殊，所以不敢轻易表白情谊，秀真向哲洙求婚。\n　　就算秀真父母阻拦，他们爱情的力量还是说服了父母。\n　　婚后生活甜蜜，秀真还帮忙修复了哲洙与母亲的关系。可是好景不长，秀真的记忆力开始一天比一天衰退，得了阿滋海默氏症，大脑在逐渐死亡。\n　　病魔深深折磨这相爱的两人，秀真有完全忘记一切的一天，可是哲洙却无法从此丢下秀真。\n　　他们的爱，能帮他们跨越记忆的考验吗",
 * "img": "https://file.peakchao.com:188/我脑中的橡皮擦.webp",
 * "video": "https://file.peakchao.com:188/我脑中的橡皮擦.mp4",
 * "videoTime": "144"
 * },
 */
public class VideoRes implements Serializable {
    @SerializedName("id")
    public String id;
    public String title;
    public String star;
    public String publicTime;
    public String type;
    public String toStar;
    public String performer;
    public String actors;
    public String country;
    public String alias;
    public String videoDescribe;
    public String img;
    public String video;
    public String videoTime;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStar() {
        return star;
    }

    public void setStar(String star) {
        this.star = star;
    }

    public String getPublicTime() {
        return publicTime;
    }

    public void setPublicTime(String publicTime) {
        this.publicTime = publicTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getToStar() {
        return toStar;
    }

    public void setToStar(String toStar) {
        this.toStar = toStar;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
    }

    public String getActors() {
        return actors;
    }

    public void setActors(String actors) {
        this.actors = actors;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getVideoDescribe() {
        return videoDescribe;
    }

    public void setVideoDescribe(String videoDescribe) {
        this.videoDescribe = videoDescribe;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getVideo() {
        return video;
    }

    public void setVideo(String video) {
        this.video = video;
    }

    public String getVideoTime() {
        return videoTime;
    }

    public void setVideoTime(String videoTime) {
        this.videoTime = videoTime;
    }
}
