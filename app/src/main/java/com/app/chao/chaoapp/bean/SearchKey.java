package com.app.chao.chaoapp.bean;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class SearchKey extends RealmObject {
    @PrimaryKey
    public String searchKey;
    public long insertTime;//插入时间

    public SearchKey() {
    }

    public SearchKey(String suggestion, long insertTime) {
        this.searchKey = suggestion;
        this.insertTime = insertTime;
    }

    public String getSearchKey() {
        return searchKey;
    }
}