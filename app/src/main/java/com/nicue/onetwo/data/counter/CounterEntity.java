package com.nicue.onetwo.data.counter;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Objects")
public class CounterEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private long id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "numbers")
    private int value;

    public CounterEntity(String title, int value) {
        this.title = title;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getValue() {
        return value;
    }
}
