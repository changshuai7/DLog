package com.shuai.dlog;


import android.content.Intent;

import com.google.gson.Gson;
import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.db.DLogDBDao;
import com.shuai.dlog.excutor.DLogThreadPoolManager;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.service.DLogAlarmReportService;
import com.shuai.dlog.service.DLogReportService;
import com.shuai.dlog.utils.Logger;
import com.shuai.dlog.utils.Util;

import java.util.List;

/**
 * DLog入口
 * @author changshuai
 */
public class DLog {

    /**
     * 写入日志文件
     *
     * @param type
     * @param content
     * @param <T>
     */
    public static <T> void write(final String type, final T content) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Gson gson = new Gson();
                String contentJson = gson.toJson(content);
                long insertIndex = DLogDBDao.getInstance(DLogConfig.getApp()).insertLog(new DLogModel(type, System.currentTimeMillis(), contentJson));
                if (insertIndex != -1) {
                    Logger.d("插入数据库成功，insertIndex="+insertIndex);
                    //每条数据插入成功，都要去尝试上报，不过尝试上报会检查是否符合delay时间限制，只有符合delay时间限制的，才会上报。
                    report(false);
                } else {
                    Logger.e("插入数据库失败，insertIndex=-1");
                }

            }
        };
        //此处利用多线程去处理多个写入的任务。
        DLogThreadPoolManager.getInstance().executeThread(runnable);
    }

    /**
     * 强制上报所有日志（如果有正在上报的任务，会进入上报日志队列中等待）
     */
    public static void sendAll() {
        report(true);
    }

    /**
     * 获取所有的日志内容
     *
     * @return
     */
    public static List<DLogModel> getAll() {
        return DLogDBDao.getInstance(DLogConfig.getApp()).loadAllLogDatas();
    }

    /**
     * 清除所有日志内容
     */
    public static void deleteAll() {
        DLogDBDao.getInstance(DLogConfig.getApp()).deleteAllLogDatas();
    }

    /**
     * 启动定时上报服务（如果此服务被系统杀死，可通过此方法来启动）
     */
    public static void startAlarmReportService(){
        if (DLogConfig.getConfig().getBaseConfig()!=null && DLogConfig.getConfig().getBaseConfig().reportAlarm() > 0){
            DLogConfig.getApp().startService(new Intent(DLogConfig.getApp(), DLogAlarmReportService.class));
        }
    }

    /**
     * 上报日志
     *
     * @param focusLaunch 是否强制上报
     *                    需要注意的是：
     *                    如果是true，则为强制上报，即使此刻有上报任务，也依然会进入队列等待。
     *                    如果是false，则为非强制上报。默认遵循着延时上报策略：有正在上报的任务，那么舍弃；如果没有，进入内部，去检查延时时间，延时时间满足，则上报
     */
    private static void report(boolean focusLaunch) {
        if (focusLaunch){
            DLogReportService.launchService(DLogConfig.getApp(), true);
        }else{
            if (Util.isServiceRunning(DLogConfig.getApp(),DLogReportService.class.getName())){
                Logger.e("存在正在上报的任务..无法继续上报");
            }else{
                DLogReportService.launchService(DLogConfig.getApp(), false);
            }
        }
    }

}
