package com.app.chao.chaoapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_history")
public class SearchHistoryEntity {
    @PrimaryKey
    @NonNull
    public String query = "";
    public long searchedAt;
}
