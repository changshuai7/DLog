package com.shuai.dlog.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.shuai.dlog.DLog;
import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.utils.Logger;

import java.util.Date;

/**
 * 定时上报任务
 * 采用AlarmManger+Service+BroadcastReceiver实现长期精确的定时任务
 * @author changshuai
 */
public class DLogAlarmReportService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.d("定时上报打印时间: " + new Date().toString());
        DLog.sendAll();

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);

        long time = DLogConfig.getConfig().getBaseConfig().reportAlarm(); //设置时间
        long triggerAtTime = SystemClock.elapsedRealtime() + time;

        Intent i = new Intent(this, DLogAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }
}