package com.app.chao.chaoapp.bean;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Gson normally coerces JSON numbers/booleans into strings; those are not episode URLs. */
public final class EpisodeUrlsAdapter extends TypeAdapter<String[]> {
    @Override public String[] read(JsonReader in) throws IOException {
        if (in.peek() != JsonToken.BEGIN_ARRAY) throw new JsonParseException("episodeUrls 必须是地址数组");
        List<String> urls = new ArrayList<>();
        in.beginArray();
        while (in.hasNext()) {
            if (in.peek() != JsonToken.STRING) throw new JsonParseException("episodeUrls 必须包含字符串地址");
            urls.add(in.nextString());
        }
        in.endArray();
        return urls.toArray(new String[0]);
    }

    @Override public void write(JsonWriter out, String[] urls) throws IOException {
        out.beginArray();
        for (String url : urls) out.value(url);
        out.endArray();
    }
}
