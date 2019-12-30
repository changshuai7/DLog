package com.shuai.dlog.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.model.DLogModel;

import java.util.List;

public class DLogDBDao extends SQLiteOpenHelper {

    //获取去除掉"."以后的packageName作为表名字的唯一标识。防止不同的app集成本库造成数据库的冲突。
    private static final String getHandPackageName() {
        String[] pn = DLogConfig.getApp().getPackageName().split(".");
        StringBuffer buffer = new StringBuffer();
        for (String s : pn) {
            buffer.append(s);
        }
        return buffer.append("_db").toString();
    }

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = getHandPackageName() + ".db";
    private static String TABLE_LOG = getHandPackageName();

    private static DLogDBDao instance;

    private DLogDBDao(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        DLogDBManager.initializeInstance(this);
    }

    public static DLogDBDao getInstance(Context context) {
        if (instance == null) {
            synchronized (DLogDBDao.class) {
                if (instance == null) {
                    instance = new DLogDBDao(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private String createTableLog = "CREATE TABLE IF NOT EXISTS " + TABLE_LOG
            + "(" + DLogModel.DLOG_DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DLogModel.DLOG_DB_TYPE + " TEXT DEFAULT NULL,"
            + DLogModel.DLOG_DB_TIMESTAMP + " TEXT DEFAULT NULL,"
            + DLogModel.DLOG_DB_CONTENT + " TEXT DEFAULT NULL);";


    /**
     * 插入一条数据
     *
     * @param model
     * @return
     */
    public long insertLog(DLogModel model) {
        synchronized (this) {
            SQLiteDatabase database = DLogDBManager.getInstance().openDatabase();
            try {
                ContentValues contentValues = model.asContentValues();
                return database.insert(TABLE_LOG, null, contentValues);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeDatabaseQuietly(database);
            }
            return 0L;
        }
    }

    /**
     * 加载所有数据
     *
     * @return
     */
    public List<DLogModel> loadAllLogDatas() {
        synchronized (this) {
            SQLiteDatabase database = DLogDBManager.getInstance().openDatabase();
            List<DLogModel> logDatas = null;
            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_LOG, null, null, null, null, null, null);
                logDatas = DLogModel.toLogDatas(cursor);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeCursorQuietly(cursor);
                closeDatabaseQuietly(database);
            }
            return logDatas;
        }
    }

    /**
     * 删除所有日志
     */
    public void deleteAllLogDatas() {
        synchronized (this) {
            SQLiteDatabase database = DLogDBManager.getInstance().openDatabase();
            List<DLogModel> logDatas = null;
            Cursor cursor = null;
            try {
                database.execSQL("DELETE FROM " + TABLE_LOG);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeCursorQuietly(cursor);
                closeDatabaseQuietly(database);
            }

        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();
            db.execSQL(createTableLog);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOG);
            db.execSQL(createTableLog);
        }
    }

    private void closeDatabaseQuietly(SQLiteDatabase database) {
        if (database == null) {
            return;
        }
        try {
            if (database.inTransaction()) {
                database.endTransaction();
            }
            DLogDBManager.getInstance().closeDatabase();
        } catch (Exception ignored) {
        }
    }

    private void closeCursorQuietly(Cursor cursor) {
        if (cursor == null) {
            return;
        }
        try {
            cursor.close();
        } catch (Exception ignored) {
        }
    }
}
