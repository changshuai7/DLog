package com.shuai.dlog.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Base64;

import com.shuai.dlog.config.DLogConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 简易SP 工具类
 *
 * @author changshuai
 */

public class PrefHelper {

    private static Context context = DLogConfig.getApp();

    public static boolean getBoolean(String key, boolean defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(key, defValue);
    }

    public static void setBoolean(String key, boolean value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static int getInt(String key, int defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getInt(key, defValue);
    }

    public static void setInt(String key, int value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static long getLong(String key, long defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getLong(key, defValue);
    }

    public static void setLong(String key, long value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putLong(key, value);
        editor.commit();
    }

    public static String getString(String key, String defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(key, defValue);
    }

    public static void setString(String key, String value) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void remove(String key) {
        Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(key);
        editor.commit();
    }

    public static void setObject(String key, Object object) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            String oAuth_Base64 = new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));
            Editor editor = preferences.edit();
            editor.putString(key, oAuth_Base64);
            editor.commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static <T> T getObject(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Object object = null;
        String string = preferences.getString(key, "");
        if (string == "") {
            return null;
        }
        byte[] base64 = Base64.decode(string.getBytes(), Base64.DEFAULT);
        ByteArrayInputStream bais = new ByteArrayInputStream(base64);
        try {
            ObjectInputStream bis = new ObjectInputStream(bais);
            object = bis.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (T) object;
    }

    public static void registerOnPrefChangeListener(OnSharedPreferenceChangeListener listener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(listener);
        } catch (Exception e) {
        }
    }

    public static void unregisterOnPrefChangeListener(OnSharedPreferenceChangeListener listener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(listener);
        } catch (Exception e) {
        }
    }

}
