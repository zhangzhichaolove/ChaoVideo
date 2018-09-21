package com.app.chao.chaoapp.net;

import com.app.chao.chaoapp.bean.HomeVideoData;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by Chao  2018/9/21 on 9:31
 * description
 */

public interface EyepetizerApis {
    String HOST = "http://baobab.kaiyanapp.com/api/";
    String END = "?vc=230&deviceModel=MI";

    //获取首页
    @GET("v4/tabs/selected")
    Observable<HomeVideoData> getAllVideo();

    //获取更多视频 ?date=1512349200000&num=2&page=2
    @GET("v4/tabs/selected")
    Observable<HomeVideoData> getAllVideo(@Query("date") String date, @Query("num") String num,
                                          @Query("page") String page);

    //发现
    @GET("v5/index/tab/discovery" + END)
    Observable<HomeVideoData> getFindData();

    //更多发现 http://baobab.kaiyanapp.com/api/v5/index/tab/discovery?start=20&num=10
    @GET("v5/index/tab/discovery")
    Observable<HomeVideoData> getFindMoreData(@Query("start") String start, @Query("num") String num);

    //推荐 http://baobab.kaiyanapp.com/api/v5/index/tab/allRec?page=0&deviceModel=MI%205
    @GET("v5/index/tab/allRec" + END)
    Observable<HomeVideoData> getRecommendData(@Query("page") String page);

    //详细页面获取推荐 http://baobab.kaiyanapp.com/api/v4/video/related?id=67546&vc=230&deviceModel=MI
    @GET("v4/video/related" + END)
    Observable<HomeVideoData> getDetailRecommendData(@Query("id") String id);

    //日报 http://baobab.kaiyanapp.com/api/v5/index/tab/feed
    @GET("v5/index/tab/feed" + END)
    Observable<HomeVideoData> getDailyData();

    //更多日报 http://baobab.kaiyanapp.com/api/v5/index/tab/feed?date=1514336400000&num=2
    @GET("v5/index/tab/feed" + END)
    Observable<HomeVideoData> getDailyMoreData(@Query("date") String date, @Query("num") String num);

    //创意 http://baobab.kaiyanapp.com/api/v5/index/tab/category/2
    @GET("v5/index/tab/category/2" + END)
    Observable<HomeVideoData> getCreativeData();

    //更多创意 http://baobab.kaiyanapp.com/api/v5/index/tab/category/2?start=10&num=10
    @GET("v5/index/tab/category/2" + END)
    Observable<HomeVideoData> getCreativeMoreData(@Query("start") String start, @Query("num") String num);

    //音乐 http://baobab.kaiyanapp.com/api/v5/index/tab/category/20
    @GET("v5/index/tab/category/20" + END)
    Observable<HomeVideoData> getMusicData();

    //更多音乐 http://baobab.kaiyanapp.com/api/v5/index/tab/category/20?start=10&num=10
    @GET("v5/index/tab/category/20" + END)
    Observable<HomeVideoData> getMusicMoreData(@Query("start") String start, @Query("num") String num);

    //旅行 http://baobab.kaiyanapp.com/api/v5/index/tab/category/6
    @GET("v5/index/tab/category/6" + END)
    Observable<HomeVideoData> getTravelData();

    //更多旅行 http://baobab.kaiyanapp.com/api/v5/index/tab/category/6?start=10&num=10
    @GET("v5/index/tab/category/6" + END)
    Observable<HomeVideoData> getTravelMoreData(@Query("start") String start, @Query("num") String num);

    //科普 http://baobab.kaiyanapp.com/api/v5/index/tab/category/32
    @GET("v5/index/tab/category/32" + END)
    Observable<HomeVideoData> getScienceData();

    //更多科普 http://baobab.kaiyanapp.com/api/v5/index/tab/category/32?start=10&num=10
    @GET("v5/index/tab/category/32" + END)
    Observable<HomeVideoData> getScienceMoreData(@Query("start") String start, @Query("num") String num);

    //搞笑 http://baobab.kaiyanapp.com/api/v5/index/tab/category/28
    @GET("v5/index/tab/category/28" + END)
    Observable<HomeVideoData> getFunnyData();

    //更多搞笑http://baobab.kaiyanapp.com/api/v5/index/tab/category/28?start=10&num=10
    @GET("v5/index/tab/category/28" + END)
    Observable<HomeVideoData> getFunnyMoreData(@Query("start") String start, @Query("num") String num);

    //时尚 http://baobab.kaiyanapp.com/api/v5/index/tab/category/24
    @GET("v5/index/tab/category/24" + END)
    Observable<HomeVideoData> getFashionData();

    //更多时尚http://baobab.kaiyanapp.com/api/v5/index/tab/category/24?start=10&num=10
    @GET("v5/index/tab/category/24" + END)
    Observable<HomeVideoData> getFashionMoreData(@Query("start") String start, @Query("num") String num);

    //运动 http://baobab.kaiyanapp.com/api/v5/index/tab/category/18
    @GET("v5/index/tab/category/18" + END)
    Observable<HomeVideoData> getSportsData();

    //更多运动http://baobab.kaiyanapp.com/api/v5/index/tab/category/18?start=10&num=10
    @GET("v5/index/tab/category/18" + END)
    Observable<HomeVideoData> getSportsMoreData(@Query("start") String start, @Query("num") String num);

    //动画 http://baobab.kaiyanapp.com/api/v5/index/tab/category/10
    @GET("v5/index/tab/category/10" + END)
    Observable<HomeVideoData> getAnimData();

    //更多动画http://baobab.kaiyanapp.com/api/v5/index/tab/category/10?start=10&num=10
    @GET("v5/index/tab/category/10" + END)
    Observable<HomeVideoData> getAnimMoreData(@Query("start") String start, @Query("num") String num);

    //广告 http://baobab.kaiyanapp.com/api/v5/index/tab/category/14
    @GET("v5/index/tab/category/14" + END)
    Observable<HomeVideoData> getAdvertData();

    //更多广告http://baobab.kaiyanapp.com/api/v5/index/tab/category/14?start=10&num=10
    @GET("v5/index/tab/category/14" + END)
    Observable<HomeVideoData> getAdvertMoreData(@Query("start") String start, @Query("num") String num);

    //开胃 http://baobab.kaiyanapp.com/api/v5/index/tab/category/4
    @GET("v5/index/tab/category/4" + END)
    Observable<HomeVideoData> getAppetizerData();

    //更多开胃http://baobab.kaiyanapp.com/api/v5/index/tab/category/4?start=10&num=10
    @GET("v5/index/tab/category/4" + END)
    Observable<HomeVideoData> getAppetizerMoreData(@Query("start") String start, @Query("num") String num);

    //生活 http://baobab.kaiyanapp.com/api/v5/index/tab/category/36
    @GET("v5/index/tab/category/36" + END)
    Observable<HomeVideoData> getLifeData();

    //更多生活http://baobab.kaiyanapp.com/api/v5/index/tab/category/36?start=10&num=10
    @GET("v5/index/tab/category/36" + END)
    Observable<HomeVideoData> getLifeMoreData(@Query("start") String start, @Query("num") String num);

    //剧情 http://baobab.kaiyanapp.com/api/v5/index/tab/category/12
    @GET("v5/index/tab/category/12" + END)
    Observable<HomeVideoData> getPlotData();

    //更多剧情http://baobab.kaiyanapp.com/api/v5/index/tab/category/12?start=10&num=10
    @GET("v5/index/tab/category/12" + END)
    Observable<HomeVideoData> getPlotMoreData(@Query("start") String start, @Query("num") String num);

    //预告 http://baobab.kaiyanapp.com/api/v5/index/tab/category/8
    @GET("v5/index/tab/category/8" + END)
    Observable<HomeVideoData> getTrailerData();

    //更多预告http://baobab.kaiyanapp.com/api/v5/index/tab/category/8?start=10&num=10
    @GET("v5/index/tab/category/8" + END)
    Observable<HomeVideoData> getTrailerMoreData(@Query("start") String start, @Query("num") String num);

    //集锦 http://baobab.kaiyanapp.com/api/v5/index/tab/category/34
    @GET("v5/index/tab/category/34" + END)
    Observable<HomeVideoData> getHighlightsData();

    //更多集锦http://baobab.kaiyanapp.com/api/v5/index/tab/category/34?start=10&num=10
    @GET("v5/index/tab/category/34" + END)
    Observable<HomeVideoData> getHighlightsMoreData(@Query("start") String start, @Query("num") String num);

    //记录 http://baobab.kaiyanapp.com/api/v5/index/tab/category/22
    @GET("v5/index/tab/category/22" + END)
    Observable<HomeVideoData> getRecordData();

    //更多记录http://baobab.kaiyanapp.com/api/v5/index/tab/category/22?start=10&num=10
    @GET("v5/index/tab/category/22" + END)
    Observable<HomeVideoData> getRecordMoreData(@Query("start") String start, @Query("num") String num);

    //游戏 http://baobab.kaiyanapp.com/api/v5/index/tab/category/30
    @GET("v5/index/tab/category/30" + END)
    Observable<HomeVideoData> getGameData();

    //更多游戏http://baobab.kaiyanapp.com/api/v5/index/tab/category/30?start=10&num=10
    @GET("v5/index/tab/category/30" + END)
    Observable<HomeVideoData> getGameMoreData(@Query("start") String start, @Query("num") String num);

    //萌宠 http://baobab.kaiyanapp.com/api/v5/index/tab/category/26
    @GET("v5/index/tab/category/26" + END)
    Observable<HomeVideoData> getCuteData();

    //更多萌宠http://baobab.kaiyanapp.com/api/v5/index/tab/category/26?start=10&num=10
    @GET("v5/index/tab/category/26" + END)
    Observable<HomeVideoData> getCuteMoreData(@Query("start") String start, @Query("num") String num);

    //综艺 http://baobab.kaiyanapp.com/api/v5/index/tab/category/38
    @GET("v5/index/tab/category/38" + END)
    Observable<HomeVideoData> getArtsData();

    //更多综艺http://baobab.kaiyanapp.com/api/v5/index/tab/category/38?start=10&num=10
    @GET("v5/index/tab/category/38" + END)
    Observable<HomeVideoData> getArtsMoreData(@Query("start") String start, @Query("num") String num);

    //所有分类 http://baobab.kaiyanapp.com/api/v4/categories/all
    @GET("v4/categories/all" + END)
    Observable<HomeVideoData> getAllCategoriesData();

    //分类详情 http://baobab.kaiyanapp.com/api/v4/categories/detail/tab?id=24
    @GET("v4/categories/detail/tab" + END)
    Observable<HomeVideoData> getCategoriesDetailData(@Query("id") String id);

    //分类详情首页 http://baobab.kaiyanapp.com/api/v4/categories/detail/index?id=24
    @GET("v4/categories/detail/index" + END)
    Observable<HomeVideoData> getCDetailHomeData(@Query("id") String id);

    //分类详情更多首页 http://baobab.kaiyanapp.com/api/v4/categories/detail/index?id=24&page=1&needFilter=true
    @GET("v4/categories/detail/index" + END)
    Observable<HomeVideoData> getCDetailMoreHomeData(@Query("id") String id, @Query("page") String page);

    //分类详情全部 http://baobab.kaiyanapp.com/api/v4/categories/videoList?id=24
    @GET("v4/categories/videoList" + END)
    Observable<HomeVideoData> getCDetailALLData(@Query("id") String id);

    //分类详情更多全部 http://baobab.kaiyanapp.com/api/v4/categories/videoList?start=10&num=10&id=24
    @GET("v4/categories/videoList" + END)
    Observable<HomeVideoData> getCDetailMoreALLData(@Query("id") String id, @Query("start") String start,
                                                    @Query("num") String num);

    //分类详情作者 http://baobab.kaiyanapp.com/api/v4/categories/detail/pgcs?id=24
    @GET("v4/categories/detail/pgcs" + END)
    Observable<HomeVideoData> getCDetailAuthorData(@Query("id") String id);

    //分类详情更多作者 http://baobab.kaiyanapp.com/api/v4/categories/detail/pgcs?id=24&start=5&num=5
    @GET("v4/categories/detail/pgcs" + END)
    Observable<HomeVideoData> getCDetailMoreAuthorData(@Query("id") String id, @Query("start") String start,
                                                       @Query("num") String num);

    //分类详情专辑 http://baobab.kaiyanapp.com/api/v4/categories/detail/playlist?id=24
    @GET("v4/categories/detail/playlist" + END)
    Observable<HomeVideoData> getCDetailPlayListData(@Query("id") String id);

    //分类详情更多专辑 http://baobab.kaiyanapp.com/api/v4/categories/detail/playlist?id=24&start=5&num=5
    @GET("v4/categories/detail/playlist" + END)
    Observable<HomeVideoData> getCDetailMorePlayListData(@Query("id") String id, @Query("start") String start,
                                                         @Query("num") String num);

    //详情页面 http://baobab.kaiyanapp.com/api/v2/video/17631
    @GET("v2/video/{id}" + END)
    Observable<HomeVideoData> getVideoDetailData(@Path("id") String id);

    //热门搜索词 http://baobab.kaiyanapp.com/api/v3/queries/hot
    @GET("v3/queries/hot" + END)
    Observable<List<String>> getQueriesHotData();

    //搜索 http://baobab.kaiyanapp.com/api/v1/search?query=搜素词
    @GET("v1/search" + END)
    Observable<HomeVideoData> getQueryData(@Query("query") String query);

    //更多搜索 http://baobab.kaiyanapp.com/api/v1/search?start=10&num=10&query=搜素词
    @GET("v1/search" + END)
    Observable<HomeVideoData> getMoreQueryData(@Query("query") String query, @Query("start") String start,
                                               @Query("num") String num);

    //标签首页 http://baobab.kaiyanapp.com/api/v1/tag/index?id=460
    @GET("v1/tag/index" + END)
    Observable<HomeVideoData> getTagIndexData(@Query("id") String id);

    //标签视频 http://baobab.kaiyanapp.com/api/v1/tag/videos?id=460
    @GET("v1/tag/videos" + END)
    Observable<HomeVideoData> getTagVideoData(@Query("id") String id);

    //更多标签视频 http://baobab.kaiyanapp.com/api/v1/tag/videos?start=10&num=10&strategy=date&id=460
    @GET("v1/tag/videos" + END)
    Observable<HomeVideoData> getTagMoreVideoData(@Query("id") String id, @Query("start") String start,
                                                  @Query("num") String num, @Query("strategy") String strategy);

    //标签动态 http://baobab.kaiyanapp.com/api/v1/tag/dynamics?id=460
    @GET("v1/tag/dynamics" + END)
    Observable<HomeVideoData> getTagDynamicsData(@Query("id") String id);

    //更多标签动态 http://baobab.kaiyanapp.com/api/v1/tag/dynamics?start=10&num=10&strategy=date&id=460
    @GET("v1/tag/dynamics" + END)
    Observable<HomeVideoData> getTagMoreDynamicsData(@Query("id") String id, @Query("start") String start,
                                                     @Query("num") String num, @Query("strategy") String strategy);

    //作者详情 http://baobab.kaiyanapp.com/api/v5/userInfo/tab?id=2172&userType=PGC
    @GET("v5/userInfo/tab" + END)
    Observable<HomeVideoData> getAuthorDetailIndexData(@Query("id") String id,
                                                       @Query("userType") String userType);

    //作者详情首页 http://baobab.kaiyanapp.com/api/v5/userInfo/tab/index?id=2164&userType=PGC
    @GET("v5/userInfo/tab/index" + END)
    Observable<HomeVideoData> getAuthorIndexData(@Query("id") String id,
                                                 @Query("userType") String userType);

    //作者详情视频 http://baobab.kaiyanapp.com/api/v4/pgcs/videoList?id=2164
    @GET("v4/pgcs/videoList" + END)
    Observable<HomeVideoData> getAuthorVideoData(@Query("id") String id);

    //作者详情更多视频 http://baobab.kaiyanapp.com/api/v4/pgcs/videoList?start=10&num=10&id=2164&strategy=date
    @GET("v4/pgcs/videoList" + END)
    Observable<HomeVideoData> getAuthorMoreVideoData(@Query("id") String id, @Query("start") String start,
                                                     @Query("num") String num, @Query("strategy") String strategy);

    //作者详情动态 http://baobab.kaiyanapp.com/api/v5/userInfo/tab/dynamics?id=2164&userType=PGC
    @GET("v5/userInfo/tab/dynamics" + END)
    Observable<HomeVideoData> getAuthorDynamicsData(@Query("id") String id,
                                                    @Query("userType") String userType);

    //作者详情动态 http://baobab.kaiyanapp.com/api/v5/userInfo/tab/dynamics?start=10&num=10&id=2164&userType=PGC
    @GET("v5/userInfo/tab/dynamics" + END)
    Observable<HomeVideoData> getAuthorMoreDynamicsData(@Query("id") String id, @Query("start") String start,
                                                        @Query("num") String num, @Query("userType") String userType);


}
