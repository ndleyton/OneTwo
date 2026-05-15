package com.nicue.onetwo.data.counter;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {CounterEntity.class}, version = 2, exportSchema = false)
public abstract class CounterDatabase extends RoomDatabase {
    public abstract CounterDao counterDao();
}
