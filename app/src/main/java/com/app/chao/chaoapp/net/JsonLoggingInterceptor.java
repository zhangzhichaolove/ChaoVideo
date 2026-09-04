package com.app.chao.chaoapp.net;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class JsonLoggingInterceptor implements Interceptor {
    private static final String TAG = "API_RESPONSE_JSON";
    private static final int MAX_CHARS_PER_LOG = 1000;
    private static final Gson PRETTY_GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        ResponseBody body = response.peekBody(Long.MAX_VALUE);
        log(response.request().url().toString(), body.string());
        return response;
    }

    private static void log(String url, String body) {
        String formattedBody = body;
        try {
            formattedBody = PRETTY_GSON.toJson(new JsonParser().parse(body));
        } catch (RuntimeException ignored) {
            // 非 JSON 响应仍按原文完整打印，便于定位服务端错误。
        }

        Log.d(TAG, "Response: " + url);
        for (String line : formattedBody.split("\\n", -1)) {
            logCompleteLine(line);
        }
        Log.d(TAG, "End response: " + url);
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
}
