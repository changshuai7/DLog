package com.shuai.dlog.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.shuai.dlog.DLog;
import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.db.DLogDBDao;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogReportCallback;
import com.shuai.dlog.utils.Logger;
import com.shuai.dlog.utils.PrefHelper;
import com.shuai.dlog.utils.Util;

import java.util.List;

public class DLogReportService extends JobIntentService {

    //用于存储时间的SP的key
    public static final String REPORT_TIME_SP_KEY = "D_LOG_REPORT_TIME_SP_KEY";
    public static final String REPORT_ERROR_COUNT_SP_KEY = "D_LOG_REPORT_ERROR_COUNT_SP_KEY";
    private static final int JOB_ID = 1000;

    //TODO 其实延迟上报是有bug的，因为如果有线程同时访问launchService,enqueueWork内部又是异步的。这样会导致最终依然有多个线程进入了方法中，让enqueueWork执行多次。
    //TODO 这样的结果导致了onHandleWork有可能会被执行多次。
    //TODO 好在onHandleWork是one by one执行的，即使多次执行，没有严格按照固定时间延迟上报，依然不会引起数据的重复上报。只不过后上报的数据就为空了。
    public static void launchService(Context ctx, boolean focusLaunch) {
        if (reportCheck() || focusLaunch) {
            try {
                enqueueWork(ctx, DLogReportService.class, JOB_ID, new Intent(ctx, DLogReportService.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        /**
         * JobIntentService执行任务的顺序为one by one。所以最好保证onHandleWork中的任务为同步任务
         */
        Logger.d("DLogReportService","onHandleWork执行");
        if (Util.isNetworkConnected(DLogConfig.getApp())){
            report();
        }else{
            Logger.e("网络连接不可用");
        }
    }


    private void report() {
        PrefHelper.setLong(REPORT_TIME_SP_KEY, System.currentTimeMillis());

        List<DLogModel> dLogModels = DLogDBDao.getInstance(DLogConfig.getApp()).loadAllLogDatas();

        if (DLogConfig.getConfig().getReportConfig() != null) {
            DLogConfig.getConfig().getReportConfig().report(dLogModels, new DLogReportCallback() {
                @Override
                public void onSuccess() {
                    Logger.d("【日志数据上报成功】");
                    PrefHelper.remove(REPORT_ERROR_COUNT_SP_KEY);//次数重置为0
                    DLogDBDao.getInstance(DLogConfig.getApp()).deleteAllLogDatas();//日志记录全部清空
                }

                @Override
                public void onFail(String msg) {
                    Logger.e("【日志数据上报失败】错误信息为"+msg);

                    int anInt = PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY, 0);
                    PrefHelper.setInt(REPORT_ERROR_COUNT_SP_KEY,++anInt);//错误增加1次

                    if (PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY, 0)<3){
                        DLog.sendAll();//上报失败以后，会再次重试。超过3次以后，不再上报。
                    }

                }
            });
        }else{
            Logger.e("DLogBaseConfigProvider没有配置！");
        }

    }

    /**
     * 检查上报时间
     *
     * @return true：可以上报；false：不可以上报
     */
    public static boolean reportCheck() {

        long lastReportTime = PrefHelper.getLong(REPORT_TIME_SP_KEY, 0);
        long curTime = System.currentTimeMillis();
        if (DLogConfig.getConfig().getBaseConfig() != null && DLogConfig.getConfig().getBaseConfig().reportDelay() > 0) { //延时>0的情况下，开启延时上报策略。
            if (curTime - lastReportTime > DLogConfig.getConfig().getBaseConfig().reportDelay()) {
                Logger.d("DLogReportService","策略通过 "+"lastTime = "+lastReportTime+",curTime="+curTime +"，线程名字:"+Thread.currentThread().getName());
                return true; //时间合理通过
            }
        }

        return false;
    }
}
