package com.nicue.onetwo.data.counter;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {CounterEntity.class}, version = 3, exportSchema = false)
public abstract class CounterDatabase extends RoomDatabase {
    public static final Migration MIGRATION_2_3 =
            new Migration(2, 3) {
                @Override
                public void migrate(SupportSQLiteDatabase database) {
                    database.execSQL(
                            "ALTER TABLE Objects ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");
                    database.execSQL("UPDATE Objects SET sort_order = _id - 1");
                }
            };

    public abstract CounterDao counterDao();
}
