package com.app.chao.chaoapp.net;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class RetrofitLoggingTest {
    @Test public void retryLoggingNeverIncludesRequestUrlOrCredentials() throws Exception {
        RetrofitHelper.getVideoApi();
        Field field = RetrofitHelper.class.getDeclaredField("okHttpClient"); field.setAccessible(true);
        OkHttpClient client = (OkHttpClient) field.get(null);
        Interceptor retry = client.interceptors().get(client.interceptors().size() - 1);
        Request request = new Request.Builder().url("https://user:RETRY_PASSWORD@example.test/RETRY_PATH?token=RETRY_QUERY").build();
        AtomicInteger calls = new AtomicInteger();
        Interceptor.Chain chain = (Interceptor.Chain) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Interceptor.Chain.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("request")) return request;
                    if (method.getName().equals("proceed")) return new Response.Builder().request((Request) arguments[0])
                            .protocol(Protocol.HTTP_1_1).code(calls.incrementAndGet() < 3 ? 503 : 200).message("fixture")
                            .body(ResponseBody.create("{}", okhttp3.MediaType.get("application/json"))).build();
                    throw new AssertionError("Unexpected chain method " + method.getName());
                });
        ShadowLog.clear();
        try (Response response = retry.intercept(chain)) { assertEquals(200, response.code()); }
        assertEquals(3, calls.get());
        assertEquals(2, ShadowLog.getLogsForTag("RetrofitHelper").size());
        for (ShadowLog.LogItem log : ShadowLog.getLogsForTag("RetrofitHelper")) {
            assertTrue(log.msg.contains("Retrying request"));
            for (String secret : new String[]{"RETRY_PASSWORD", "RETRY_PATH", "RETRY_QUERY", "example.test"}) {
                assertFalse(log.msg, log.msg.contains(secret));
            }
        }
    }
}
