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
    public static final String REPORT_TIME_SP_KEY = DLogReportService.class.getSimpleName() + "_lastReportTime_key";
    public static final String REPORT_ERROR_COUNT_SP_KEY = "REPORT_ERROR_COUNT_SP_KEY";
    private static final int JOB_ID = 1000;

    //上报延时：默认5分钟一次
    private static final long DEFAULT_DELAY = 5 * 60 * 1000;
    private static final long REPORT_DELAY = DLogConfig.getConfig().getBaseConfig() == null ? DEFAULT_DELAY : DLogConfig.getConfig().getBaseConfig().reportDelay();

    //是否正在上报中.
    protected boolean isReporting = false;

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
        Logger.d("onHandleWork执行");
        if (isReporting) {
            if (DLogConfig.getConfig().getBaseConfig().isDebug()) {
                Logger.e("正在上报中...return");
            }
        } else {
            if (Util.isNetworkConnected(DLogConfig.getApp())){
                report();
            }else{
                Logger.e("网络连接不可用");
            }
        }
    }


    private void report() {
        PrefHelper.setLong(REPORT_TIME_SP_KEY, System.currentTimeMillis());

        List<DLogModel> dLogModels = DLogDBDao.getInstance(DLogConfig.getApp()).loadAllLogDatas();

        if (DLogConfig.getConfig().getReportConfig() != null) {
            isReporting = true;
            DLogConfig.getConfig().getReportConfig().report(dLogModels, new DLogReportCallback() {
                @Override
                public void onSuccess() {
                    isReporting = false;
                    Logger.d("【日志数据上报成功】");
                    PrefHelper.remove(REPORT_ERROR_COUNT_SP_KEY);//次数重置为0
                    DLogDBDao.getInstance(DLogConfig.getApp()).deleteAllLogDatas();//日志记录全部清空
                }

                @Override
                public void onFail(String msg) {
                    isReporting = false;
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
     * @return
     */
    public static boolean reportCheck() {
        long lastReportTime = PrefHelper.getLong(REPORT_TIME_SP_KEY, 0);
        long curTime = System.currentTimeMillis();
        if (lastReportTime > curTime || curTime - lastReportTime > REPORT_DELAY) {
            return true;
        }
        return false;
    }
}
