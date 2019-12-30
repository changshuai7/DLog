package com.shuai.dlog;


import com.google.gson.Gson;
import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.db.DLogDBDao;
import com.shuai.dlog.excutor.DLogThreadPoolManager;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.service.DLogReportService;
import com.shuai.dlog.utils.Logger;

import java.util.List;

/**
 * 策略：
 * 默认会采取每隔5分钟上报一次（全量数据），每次上报完成数据以后会删除数据库存有的数据。
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
                    report(false);
                } else {
                    Logger.e("插入数据库失败，insertIndex=-1");
                }

                printLog();

            }
        };
        DLogThreadPoolManager.getInstance().executeThread(runnable);
    }

    /**
     * 强制上报所有日志
     */
    public static void sendAll() {
        printLog();
        report(true);
    }

    /**
     * 获取所有的日志内容
     *
     * @return
     */
    public List<DLogModel> getAll() {
        return DLogDBDao.getInstance(DLogConfig.getApp()).loadAllLogDatas();
    }

    /**
     * 清除所有日志内容
     */
    public void deleteAll() {
        DLogDBDao.getInstance(DLogConfig.getApp()).deleteAllLogDatas();
    }

    /**
     * 上报日志
     *
     * @param focusLaunch 是否强制上报
     */
    private static void report(boolean focusLaunch) {
        DLogReportService.launchService(DLogConfig.getApp(), focusLaunch);
    }

    /**
     * 打印日志
     */
    private static void printLog() {
        //打印日志
        List<DLogModel> dLogModels = DLogDBDao.getInstance(DLogConfig.getApp()).loadAllLogDatas();
        if (dLogModels != null) {
            for (DLogModel model : dLogModels) {
                Logger.d("数据库所有数据：" + "总长度为：" + dLogModels.size() + "，id为：" + model.getId() + "，内容为：", model.toString());
            }
        }
    }
}
