package com.app.chao.chaoapp.bean;

// Preserve the generic records type used by Gson in the minified Retrofit response.
@androidx.annotation.Keep
public class PageInfo<T> {

    public T records;

    public T getRecords() {
        return records;
    }

    public void setRecords(T records) {
        this.records = records;
    }
}
