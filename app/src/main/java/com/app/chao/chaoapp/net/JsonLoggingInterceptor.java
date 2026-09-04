package com.app.chao.chaoapp.net;

import android.util.Log;

import com.app.chao.chaoapp.App;
import com.app.chao.chaoapp.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;
import java.util.concurrent.atomic.AtomicLong;

final class JsonLoggingInterceptor implements Interceptor {
    static final String TAG = "API_RESPONSE_JSON";
    private static final int MAX_CHARS_PER_LOG = 1000;
    private static final AtomicLong REQUEST_IDS = new AtomicLong();
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!BuildConfig.DEBUG) {
            return chain.proceed(chain.request());
        }
        long requestId = REQUEST_IDS.incrementAndGet();
        long startNanos = System.nanoTime();
        String method = chain.request().method();
        String url = redact(chain.request().url().toString());
        Log.d(TAG, "#" + requestId + " " + method + " " + url);
        Response response = chain.proceed(chain.request());
        long elapsedNanos = System.nanoTime() - startNanos;
        long size = response.body() == null ? 0 : response.body().contentLength();
        Log.d(TAG, String.format(java.util.Locale.US, "#%d HTTP %d %.1fms size=%d",
                requestId, response.code(), elapsedNanos / 1e6d, size));
        return ApiResponseLogStore.logResponse(App.getInstance(), response, requestId);
    }

    static void log(long requestId, String url, String body) {
        String formattedBody = body;
        try {
            formattedBody = PRETTY_GSON.toJson(JsonParser.parseString(body));
        } catch (RuntimeException ignored) {
            // 非 JSON 响应仍按原文完整打印，便于定位服务端错误。
        }

        Log.d(TAG, "#" + requestId + " response: " + redact(url));
        for (String line : formattedBody.split("\\n", -1)) {
            logCompleteLine(line);
        }
        Log.d(TAG, "#" + requestId + " end response");
    }

    private static void logCompleteLine(String line) {
        if (line.isEmpty()) {
            Log.d(TAG, "");
            return;
        }
        for (int start = 0; start < line.length(); start += MAX_CHARS_PER_LOG) {
            int end = Math.min(start + MAX_CHARS_PER_LOG, line.length());
            Log.d(TAG, line.substring(start, end));
        }
    }

    private static String redact(String value) {
        return value.replaceAll("(?i)(token|authorization|cookie|password|secret|api_key)=([^&]+)",
                "$1=██");
    }
}
