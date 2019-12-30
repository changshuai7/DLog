package com.shuai.dlog.db;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

public class DLogDBManager {


    private AtomicInteger mOpenCounter = new AtomicInteger();

    private static DLogDBManager instance;
    private SQLiteOpenHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    private DLogDBManager(SQLiteOpenHelper helper) {
        mDatabaseHelper = helper;
    }

    public static synchronized void initializeInstance(SQLiteOpenHelper helper) {
        if (instance == null) {
            instance = new DLogDBManager(helper);
        }
    }

    public static synchronized DLogDBManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(DLogDBManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }
        return instance;
    }

    public synchronized SQLiteDatabase openDatabase() {
        //如果计数为1，打开数据库连接，否则说明连接已经打开，不再重复操作。
        if (mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        //与openDatabase成对，最后需要关闭时，close掉。
        if (mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();
        }
    }
}
