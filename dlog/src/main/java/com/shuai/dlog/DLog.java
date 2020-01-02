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
import com.shuai.dlog.utils.PrefHelper;
import com.shuai.dlog.utils.Util;

import java.util.List;

import static com.shuai.dlog.service.DLogReportService.REPORT_TIME_SP_KEY;

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
                    // 每次写入数据库以后，都要去检查是否需要上报。如果 ①开启了delay策略、delay时间检查通过 ②并且当前没有正在执行的上报任务。那么立即上报。
                    // 之所以要在 当前没有正在执行的上报任务时 才上报，就是因为write是一个频繁操作。有可能在某个时间，满足delay时间检查通过的数据有多条。而我们要求：delay策略满足时间条件后只允许上报一次。否则发生了多次上报会使得延时策略失去意义。
                    if (!Util.isServiceRunning(DLogConfig.getApp(),DLogReportService.class.getName()) && reportDelayCheck()){
                        report();
                    }
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
        report();
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
     */
    private static void report() {

        DLogReportService.launchService(DLogConfig.getApp());

    }

    /**
     * 检查延时上报策略和时间检查是否通过
     *
     * @return true：通过；false：不通过
     */
    private static boolean reportDelayCheck() {

        long lastReportTime = PrefHelper.getLong(REPORT_TIME_SP_KEY, 0);
        long curTime = System.currentTimeMillis();
        if (DLogConfig.getConfig().getBaseConfig() != null && DLogConfig.getConfig().getBaseConfig().reportDelay() > 0) { //延时>0的情况下，说明开启了延时上报策略。
            if (curTime - lastReportTime > DLogConfig.getConfig().getBaseConfig().reportDelay()) {//时间合理通过
                Logger.d("DLogReportService", "延时上报策略通过，进入上报队列 " + "lastTime = " + lastReportTime + ",curTime=" + curTime + "，线程名:" + Thread.currentThread().getName());
                return true; //时间合理通过
            }
        }
        return false;
    }

}
