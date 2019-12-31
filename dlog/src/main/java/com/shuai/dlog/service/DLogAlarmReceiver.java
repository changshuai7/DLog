package com.shuai.dlog.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DLogAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, DLogAlarmReportService.class);
        context.startService(i);
    }
}