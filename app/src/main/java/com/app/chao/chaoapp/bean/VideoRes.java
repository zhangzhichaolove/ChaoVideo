package com.app.chao.chaoapp.bean;

import com.app.chao.chaoapp.data.VideoSource;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.JsonAdapter;
import android.os.Parcel;
import android.os.Parcelable;


import okhttp3.HttpUrl;

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
public class VideoRes implements Parcelable {
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
    public int episodes;
    // Optional ordered, 1-based episode contract. Null retains the legacy filename convention;
    // an explicitly supplied array is authoritative, including an empty array.
    @SerializedName("episodeUrls")
    @JsonAdapter(EpisodeUrlsAdapter.class)
    private String[] episodeUrls;
    private int localWatchedEpisode;
    private long localProgressMs;
    private long localDurationMs;
    // Client-owned provenance: an API response must not impersonate local/download records.
    private transient String sourceId;
    private transient String sourceBaseUrl;
    private transient String storedLibraryKey;

    public void bindApiSource(String baseUrl) {
        if (sourceId != null) return;
        sourceBaseUrl = VideoSource.normalize(baseUrl);
        sourceId = VideoSource.apiId(sourceBaseUrl);
    }

    public void bindOfflineSource(boolean downloaded) {
        if (sourceId != null) return;
        sourceId = downloaded ? VideoSource.DOWNLOAD : VideoSource.LOCAL;
    }

    public String getSourceId() { return sourceId == null ? VideoSource.LEGACY : sourceId; }
    public String getSourceBaseUrl() { return sourceBaseUrl; }
    public String getStoredLibraryKey() { return storedLibraryKey; }

    /** Room rows have an established key, including old raw-URL keys that must not be recomputed. */
    public void restoreLibrarySource(String sourceId, String baseUrl, String key) {
        this.sourceId = sourceId == null ? VideoSource.LEGACY : sourceId;
        sourceBaseUrl = baseUrl;
        storedLibraryKey = key;
    }


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
        return resolveUrl(img);
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getVideo() {
        if ((video == null || video.trim().isEmpty()) && episodeUrls != null && episodeUrls.length > 0) {
            return getEpisodeVideo(1);
        }
        return resolveUrl(video);
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

    public int getEpisodes() {
        return episodeUrls == null ? Math.max(0, episodes) : episodeUrls.length;
    }

    public String[] getEpisodeUrls() {
        return episodeUrls == null ? null : episodeUrls.clone();
    }

    public void setEpisodeUrls(String[] urls) {
        episodeUrls = urls == null ? null : urls.clone();
    }

    /** Fail the API response rather than silently guessing media for a malformed explicit list. */
    public void validateEpisodeUrls() {
        if (episodeUrls == null) return;
        for (int i = 0; i < episodeUrls.length; i++) {
            String url = episodeUrls[i];
            if (url == null || url.trim().isEmpty() || HttpUrl.parse(getEpisodeVideo(i + 1)) == null) {
                throw new IllegalArgumentException("episodeUrls 必须包含有效的 HTTP(S) 分集地址");
            }
        }
    }

    public void setEpisodes(int episodes) {
        this.episodes = episodes;
    }

    public int getLocalWatchedEpisode() {
        return localWatchedEpisode;
    }

    public long getLocalProgressMs() {
        return localProgressMs;
    }

    public long getLocalDurationMs() {
        return localDurationMs;
    }

    public void setLocalProgress(int episode, long positionMs, long durationMs) {
        localWatchedEpisode = episode;
        localProgressMs = positionMs;
        localDurationMs = durationMs;
    }

    public String getEpisodeVideo(int episode) {
        if (episodeUrls != null) {
            return episode < 1 || episode > episodeUrls.length ? null : resolveUrl(episodeUrls[episode - 1]);
        }
        return resolveUrl(episodePath(video, episode));
    }

    static String episodePath(String source, int episode) {
        if (source == null || episode < 1) {
            return source;
        }

        // Legacy compatibility only: never rewrite underscores/dots in a query or fragment.
        int end = source.length();
        int query = source.indexOf('?');
        int fragment = source.indexOf('#');
        if (query >= 0) end = Math.min(end, query);
        if (fragment >= 0) end = Math.min(end, fragment);
        String path = source.substring(0, end);
        String tail = source.substring(end);
        int suffixStart = path.lastIndexOf('_');
        int extensionStart = path.lastIndexOf('.');
        if (suffixStart > path.lastIndexOf('/') && extensionStart > suffixStart
                && isNumber(path.substring(suffixStart + 1, extensionStart))) {
            return path.substring(0, suffixStart + 1)
                    + episode + path.substring(extensionStart) + tail;
        }
        if (extensionStart > path.lastIndexOf('/')) {
            return path.substring(0, extensionStart)
                    + "_" + episode + path.substring(extensionStart) + tail;
        }
        return path + "_" + episode + tail;
    }

    private static boolean isNumber(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String resolveUrl(String value) {
        // Legacy records lack provenance. Keep their recorded URL; never silently redirect to
        // the currently selected server. All fresh API models are bound by SourcedVideoApi.
        HttpUrl baseUrl = sourceBaseUrl == null ? null : HttpUrl.parse(sourceBaseUrl);
        HttpUrl resolvedUrl = baseUrl == null || value == null ? null : baseUrl.resolve(value);
        return resolvedUrl == null ? value : resolvedUrl.toString();
    }

    public VideoRes() {
    }

    private VideoRes(Parcel in) {
        id = in.readString();
        title = in.readString();
        star = in.readString();
        publicTime = in.readString();
        type = in.readString();
        toStar = in.readString();
        performer = in.readString();
        actors = in.readString();
        country = in.readString();
        alias = in.readString();
        videoDescribe = in.readString();
        img = in.readString();
        video = in.readString();
        videoTime = in.readString();
        episodes = in.readInt();
        localWatchedEpisode = in.readInt();
        localProgressMs = in.readLong();
        localDurationMs = in.readLong();
        sourceId = in.readString();
        sourceBaseUrl = in.readString();
        storedLibraryKey = in.readString();
        episodeUrls = in.createStringArray();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(star);
        dest.writeString(publicTime);
        dest.writeString(type);
        dest.writeString(toStar);
        dest.writeString(performer);
        dest.writeString(actors);
        dest.writeString(country);
        dest.writeString(alias);
        dest.writeString(videoDescribe);
        dest.writeString(img);
        dest.writeString(video);
        dest.writeString(videoTime);
        dest.writeInt(episodes);
        dest.writeInt(localWatchedEpisode);
        dest.writeLong(localProgressMs);
        dest.writeLong(localDurationMs);
        dest.writeString(sourceId);
        dest.writeString(sourceBaseUrl);
        dest.writeString(storedLibraryKey);
        dest.writeStringArray(episodeUrls);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VideoRes> CREATOR = new Creator<VideoRes>() {
        @Override
        public VideoRes createFromParcel(Parcel in) {
            return new VideoRes(in);
        }

        @Override
        public VideoRes[] newArray(int size) {
            return new VideoRes[size];
        }
    };
}
