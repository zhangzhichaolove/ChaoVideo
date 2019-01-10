// (c)2016 Flipboard Inc, All Rights Reserved.

package com.app.chao.chaoapp.net;


import com.app.chao.chaoapp.bean.HomeVideoData;
import com.app.chao.chaoapp.bean.SpecialVideoData;
import com.app.chao.chaoapp.bean.VideoRes;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Description: VideoApis
 */

public interface VideoApis {
    String HOST = "https://api.apiopen.top/";

    /**
     * 首页
     *
     * @return
     */
    @GET("homePageApi/homePage.do")
    Observable<VideoHttpResponse<VideoRes>> getHomePage();

    /**
     * 首页-获取Banner
     *
     * @return
     */
    @GET("getVideoHomeData")
    Observable<VideoHttpResponse<List<HomeVideoData>>> getVideoHomeData();

    /**
     * 首页-获取专题
     *
     * @return
     */
    @GET("getVideoSpecialData")
    Observable<VideoHttpResponse<List<SpecialVideoData>>> getVideoSpecialData();

    /**
     * 影片详情
     *
     * @param mediaId 影片id
     * @return
     */
    @GET("videoDetailApi/videoDetail.do")
    Observable<VideoHttpResponse<VideoRes>> getVideoInfo(@Query("mediaId") String mediaId);

    /**
     * 影片分类列表
     *
     * @param catalogId
     * @param pnum
     * @return
     */
    @GET("columns/getVideoList.do")
    Observable<VideoHttpResponse<VideoRes>> getVideoList(@Query("catalogId") String catalogId, @Query("pnum") String pnum);

    /**
     * 影片搜索
     *
     * @param pnum
     * @return
     */
    @GET("searchKeyWordApi/getVideoListByKeyWord.do")
    Observable<VideoHttpResponse<VideoRes>> getVideoListByKeyWord(@Query("keyword") String keyword, @Query("pnum") String pnum);

    /**
     * 获取评论列表
     *
     * @param mediaId
     * @param pnum
     * @return
     */
    @GET("Commentary/getCommentList.do")
    Observable<VideoHttpResponse<VideoRes>> getCommentList(@Query("mediaId") String mediaId, @Query("pnum") String pnum);
}
