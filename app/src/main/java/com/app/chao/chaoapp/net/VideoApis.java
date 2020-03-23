// (c)2016 Flipboard Inc, All Rights Reserved.

package com.app.chao.chaoapp.net;


import com.app.chao.chaoapp.bean.PageInfo;
import com.app.chao.chaoapp.bean.VideoRes;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Description: VideoApis
 */

public interface VideoApis {
    String HOST = "http://web.peakchao.top:250/video/";

    /**
     * 首页
     *
     * @return
     */
    @GET("getVideoBanner")
    Observable<VideoHttpResponse<List<VideoRes>>> getVideoBanner();

    /**
     * 首页-获取Banner
     *
     * @return
     */
    @GET("getVideoList")
    Observable<VideoHttpResponse<PageInfo<List<VideoRes>>>> getVideoList(@Query("page") int page);


}
