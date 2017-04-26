package com.nicue.onetwo.db;


import android.provider.BaseColumns;

public class TaskContract {
    public static final String DB_NAME = "com.nicue.onetwo.db";
    public static final int DB_VERSION = 2;

    public class TaskEntry implements BaseColumns {
        public static final String TABLE = "Objects";

        public static final String COL_TASK_TITLE = "title";

        public static final String COL_NUM_TITLE = "numbers";

    }
}
