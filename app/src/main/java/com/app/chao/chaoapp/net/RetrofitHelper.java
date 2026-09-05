package com.app.chao.chaoapp.net;


import android.util.Log;

import com.app.chao.chaoapp.BuildConfig;
import com.app.chao.chaoapp.Constants;
import com.app.chao.chaoapp.utils.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit帮助类
 */
public class RetrofitHelper {
    private static final int MAX_RETRIES = 2;

    private static OkHttpClient okHttpClient = null;
    private static VideoApis videoApi;

    public static synchronized VideoApis getVideoApi() {
        initOkHttp();
        if (videoApi == null) {
            String baseUrl = ApiAddressManager.getBaseUrl();
            Retrofit retrofit = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                    .build();
            videoApi = new SourcedVideoApi(retrofit.create(VideoApis.class), baseUrl);
        }
        return videoApi;
    }

    static synchronized void resetVideoApi() {
        videoApi = null;
    }

    private static void initOkHttp() {
        if (okHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if (BuildConfig.DEBUG) {
                builder.addInterceptor(new JsonLoggingInterceptor());
            }
            File cacheFile = new File(Constants.PATH_CACHE);
            Cache cache = new Cache(cacheFile, 1024 * 1024 * 50);
            Interceptor cacheInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    if (!SystemUtils.isNetworkConnected()) {
                        request = request.newBuilder()
                                .cacheControl(CacheControl.FORCE_CACHE)
                                .build();
                    }
                    Response response = chain.proceed(request);
                    int tryCount = 0;
                    while (canRetry(request, response, tryCount)) {
                        response.close();
                        tryCount++;
                        Log.w(RetrofitHelper.class.getSimpleName(),
                                "Retrying request (" + tryCount + "/" + MAX_RETRIES + ")");
                        response = chain.proceed(request);
                    }

                    if (SystemUtils.isNetworkConnected()) {
                        int maxAge = 0;
                        // 有网络时, 不缓存, 最大保存时长为0
                        response = response.newBuilder()
                                .header("Cache-Control", "public, max-age=" + maxAge)
                                .removeHeader("Pragma")
                                .build();
                    } else {
                        // 无网络时，设置超时为4周
                        int maxStale = 60 * 60 * 24 * 28;
                        response = response.newBuilder()
                                .header("Cache-Control", "public, only-if-cached, max-stale=" + maxStale)
                                .removeHeader("Pragma")
                                .build();
                    }
                    return response;
                }
            };
            //设置缓存
            builder.addInterceptor(cacheInterceptor);
            builder.cache(cache);
            //设置超时
            builder.connectTimeout(10, TimeUnit.SECONDS);
            builder.readTimeout(20, TimeUnit.SECONDS);
            builder.writeTimeout(20, TimeUnit.SECONDS);
            //错误重连
            builder.retryOnConnectionFailure(true);
            okHttpClient = builder.build();
        }
    }

    private static boolean canRetry(Request request, Response response, int tryCount) {
        if (tryCount >= MAX_RETRIES
                || !("GET".equals(request.method()) || "HEAD".equals(request.method()))) {
            return false;
        }
        int code = response.code();
        return code == 408 || code == 500 || code == 502 || code == 503 || code == 504;
    }
}
