package com.app.chao.chaoapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.app.chao.chaoapp.bean.VideoRes;

@Entity(tableName = "video_records")
public class VideoRecordEntity {
    @PrimaryKey
    @NonNull
    public String videoKey = "";
    public String videoId;
    public String title;
    public String star;
    public String publicTime;
    public String type;
    public String director;
    public String performer;
    public String actors;
    public String country;
    public String alias;
    public String description;
    public String imageUrl;
    public String videoUrl;
    public String videoTime;
    public int episodes;
    public boolean favorite;
    public long favoriteAt;
    public long lastWatchedAt;
    public int lastEpisode;
    public long positionMs;
    public long durationMs;
    public int watchCount;

    public static VideoRecordEntity from(VideoRes video) {
        VideoRecordEntity result = new VideoRecordEntity();
        result.videoKey = keyOf(video);
        result.videoId = video == null ? null : video.getId();
        result.title = video == null ? null : video.getTitle();
        result.star = video == null ? null : video.getStar();
        result.publicTime = video == null ? null : video.getPublicTime();
        result.type = video == null ? null : video.getType();
        result.director = video == null ? null : video.getToStar();
        result.performer = video == null ? null : video.getPerformer();
        result.actors = video == null ? null : video.getActors();
        result.country = video == null ? null : video.getCountry();
        result.alias = video == null ? null : video.getAlias();
        result.description = video == null ? null : video.getVideoDescribe();
        result.imageUrl = video == null ? null : video.getImg();
        result.videoUrl = video == null ? null : video.getVideo();
        result.videoTime = video == null ? null : video.getVideoTime();
        result.episodes = video == null ? 0 : video.getEpisodes();
        return result;
    }

    public VideoRes toVideo() {
        VideoRes result = new VideoRes();
        result.setId(videoId);
        result.setTitle(title);
        result.setStar(star);
        result.setPublicTime(publicTime);
        result.setType(type);
        result.setToStar(director);
        result.setPerformer(performer);
        result.setActors(actors);
        result.setCountry(country);
        result.setAlias(alias);
        result.setVideoDescribe(description);
        result.setImg(imageUrl);
        result.setVideo(videoUrl);
        result.setVideoTime(videoTime);
        result.setEpisodes(episodes);
        result.setLocalProgress(lastEpisode, positionMs, durationMs);
        return result;
    }

    public void mergeMetadata(VideoRecordEntity source) {
        videoId = source.videoId;
        title = source.title;
        star = source.star;
        publicTime = source.publicTime;
        type = source.type;
        director = source.director;
        performer = source.performer;
        actors = source.actors;
        country = source.country;
        alias = source.alias;
        description = source.description;
        imageUrl = source.imageUrl;
        videoUrl = source.videoUrl;
        videoTime = source.videoTime;
        episodes = source.episodes;
    }

    @NonNull
    public static String keyOf(VideoRes video) {
        if (video != null && video.getId() != null && !video.getId().trim().isEmpty()) {
            return "id:" + video.getId().trim();
        }
        String url = video == null ? null : video.getVideo();
        return "url:" + (url == null ? "" : url);
    }
}
