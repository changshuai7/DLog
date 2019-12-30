package com.shuai.dlog.model;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DLogModel {

    public static final String DLOG_DB_ID = "dlog_db_id";
    public static final String DLOG_DB_TYPE = "dlog_db_type";
    public static final String DLOG_DB_TIMESTAMP = "dlog_db_timestamp";
    public static final String DLOG_DB_CONTENT = "dlog_db_content";

    private long id;//id
    private String type;//类型
    private long timestamp;//时间戳
    private String content; //内容

    public DLogModel() {
    }

    public DLogModel(String type, long timestamp, String content) {
        this(0, type, timestamp, content);
    }

    public DLogModel(int id, String type, long timestamp, String content) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.content = content;
    }

    public ContentValues asContentValues() {
        ContentValues values = new ContentValues();
        if (id != 0) {
            values.put(DLOG_DB_ID, id);
        }
        values.put(DLOG_DB_TYPE, type);
        values.put(DLOG_DB_TIMESTAMP, timestamp);
        values.put(DLOG_DB_CONTENT, content);
        return values;
    }

    public static List<DLogModel> toLogDatas(Cursor cursor) {
        List<DLogModel> datas = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                do {
                    DLogModel itemData = new DLogModel();
                    itemData.setContent(cursor.getString(cursor.getColumnIndex(DLOG_DB_CONTENT)));
                    itemData.setId(cursor.getLong(cursor.getColumnIndex(DLOG_DB_ID)));
                    itemData.setTimestamp(cursor.getLong(cursor.getColumnIndex(DLOG_DB_TIMESTAMP)));
                    itemData.setType(cursor.getString(cursor.getColumnIndex(DLOG_DB_TYPE)));
                    datas.add(itemData);
                } while (cursor.moveToNext());
            }
        }
        return datas;
    }

    @Override
    public String toString() {
        return "DLogModel{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", content='" + content + '\'' +
                '}';
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
