package com.shuai.dlog.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;

import com.shuai.dlog.DLog;
import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.db.DLogDBDao;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogSyncReportResult;
import com.shuai.dlog.utils.Logger;
import com.shuai.dlog.utils.PrefHelper;
import com.shuai.dlog.utils.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 写在前面：
 * 一、>>>>> 必须要了解的基础知识 <<<<<
 * JobIntentService是使用静态方法enqueueWork调用执行异步耗时任务的。
 * 每次enqueueWork都会创建一个JobIntentService的对象实例，执行onHandleWork中的任务。执行完毕则销毁实例对象，关闭服务。开发者无需关注JobIntentService的生命周期
 * 且在JobID相同的情况下，如果多次调用enqueueWork，会多次创建不同的JobIntentService的对象实例，且这些任务都会进入队列，one by one依次执行，直至任务执行完毕。
 * <p>
 * 二、>>>>> DLog的埋点策略 <<<<<
 * DLog有三种埋点策略:
 * 1、策略一：定时轮循上报，即启动DLogAlarmReportService服务即可定期上报
 * 2、策略二、延时上报，即在写入日志的时候记录时间，下次再写入日志时，如果两次写入日志间隔的时间大于约定的deploy时间，那么立即上报。否则不上报。
 * 3、策略三、强制上报。对于一些非常紧急的日志，可以执行DLog.sendAll();强制上报所有日志。
 * <p>
 * <p>
 * 但是有几个棘手的问题，
 * 问题1：
 * 如果在某个时间定时上报时间到了，此时又正好有一个延时上报满足时间条件也准备上报，或者还有一个日志在准备做强制上报。那么多者同时满足条件的上报任务，同时上报，必然会导致上报内容重复。
 * 针对此问题的解决方案就是：上报的任务必须one by one。在同一时间内只可以允许有一个上报任务。如果有多个，那么请排队等待。
 * 具体的实现方式就是每次上报时候，检查是否有JobIntentService存活。没有的话，立即上报，有的话，等待上个上报任务完成，再执行上报。
 * 所以规定，在外部实现的上报日志的方法reportSync中，要求必须是同步耗时的。否则任务无法one by one
 * <p>
 * 需要注意的是：
 * 延时上报只有在符合deploy时间的情况下才会上报。小于deploy时间的会自动忽略不予上报
 * 定时轮训上报、强制上报、延时上报如果同时满足上报条件了，即同时到了上报的时间点，都会进入上报队列等待上报，永不丢失
 * <p>
 * 问题2：
 * 如果在上报日志的时候，有新的日志写入了，那么怎么处理？
 * 这个问题的解决方案是：比如现在有1-10 总共10条数据发送给了应用层调用准备上报。那么DLog库中会记录此次上报数据的的所有uuid。
 * DLog库收到应用层上报成功的回调以后，自动删除对应的日志内容。在此期间，写入任何新的日志完全没有影响
 * <p>
 * 三、>>>>> DLog埋点重试策略 <<<<<
 * DLog将上报的处理交给上层处理，上层返回成功或者失败的回调。
 * 针对成功回调，删除对应的日志内容。
 * 针对失败回调，DLog会重试三次，如果超过三次已然没有上报成功，那么重试策略终止（很好理解，无限重试，不怕死循环？），直到将来上报成功以后，此策略再次开启。
 * <p>
 * 四、>>>>> DLog埋点超时策略 <<<<<
 * DLog将上报的处理交给上层处理，上层可以在reportSync中方法中执行同步耗时的操作处理数据，但是，并不意味着底层会无限期阻塞于此。
 * DLog中规定了超时时间reportSyncTimeOut，如果在超时时间内上层未能给到回调，DLog会默认认为上报失败。
 */

public class DLogReportService extends JobIntentService {

    //用于存储时间的SP的key
    public static final String REPORT_TIME_SP_KEY = "D_LOG_REPORT_TIME_SP_KEY";
    public static final String REPORT_ERROR_COUNT_SP_KEY = "D_LOG_REPORT_ERROR_COUNT_SP_KEY";
    private static final int JOB_ID = 1000;
    private static final String TAG = DLogReportService.class.getSimpleName();

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
        Logger.d(TAG, "onHandleWork执行");
        if (Util.isNetworkConnected(DLogConfig.getApp())) {
            reportSync();
        } else {
            Logger.e(TAG,"网络连接不可用");
        }
    }


    /**
     * 同步上报网络请求
     * 超时时间为：LogConfig.getConfig().getReportConfig().reportSyncTimeOut()
     */
    private void reportSync() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        FutureTask<ResultObj> futureTask = new FutureTask<>(new Callable<ResultObj>() {

            @Override
            public ResultObj call() throws Exception {
                return getSyncReportResult();
            }
        });

        executorService.execute(futureTask);

        ResultObj result;
        try {
            long timeOut;//超时时间
            if (DLogConfig.getConfig().getReportConfig()!=null && DLogConfig.getConfig().getReportConfig().reportSyncTimeOut()>0){
                timeOut = DLogConfig.getConfig().getReportConfig().reportSyncTimeOut();
            }else{
                timeOut = 60*1000;
            }
            result = futureTask.get(timeOut, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {//超时等各种错误，则执行上报失败回调
            e.printStackTrace();
            result = new ResultObj(null, DLogSyncReportResult.FAIL);
        } finally {
            executorService.shutdown();
            futureTask.cancel(true);
        }

        if (result != null) {
            if (result.getResult() == DLogSyncReportResult.SUCCESS) {
                Logger.d(TAG,"【日志数据上报成功】" + "上报的长度为" + result.getUuids().length + ",uuids为" + Arrays.toString(result.getUuids()));

                PrefHelper.remove(REPORT_ERROR_COUNT_SP_KEY);//失败次数重置为0
                if (result.getUuids() != null) {
                    DLogDBDao.getInstance(DLogConfig.getApp()).deleteLogDatasByUuid(result.getUuids());//TODO 删除可能会失败。出现几率极小，暂时先不处理
                }
                //注意：这里不可以清空所有日志，万一在上报的过程中，有新的日志写入呢？必须根据日志的uuid删除才可靠。
                //DLogDBDao.getInstance(DLogConfig.getApp()).deleteAllLogDatas();//日志记录全部清空

            } else if (result.getResult() == DLogSyncReportResult.FAIL) {

                int anInt = PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY, 0);
                PrefHelper.setInt(REPORT_ERROR_COUNT_SP_KEY, ++anInt);//失败次数增加1次

                Logger.e(TAG,"【日志数据上报失败】" + "第" + PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY, 0) + "次");

                if (PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY, 0) < 3) {
                    DLog.sendAll();//上报失败以后，会强制再次重试。超过3次以后，不再上报。
                }
            } else {
                //..一定不会执行的
                Logger.e(TAG,"见鬼了，执行了SUCCESS和FAIL以外的内容");
            }
        } else {
            Logger.e(TAG,"ResultObj结果为null，可能出现了各种异常");
        }
    }


    private ResultObj getSyncReportResult() {
        PrefHelper.setLong(REPORT_TIME_SP_KEY, System.currentTimeMillis());

        List<DLogModel> dLogModels = DLogDBDao.getInstance(DLogConfig.getApp()).loadAllLogDatas();

        if (dLogModels == null) {
            Logger.e(TAG,"异常：从数据库中loadAllLogDatas数据为null，无法上报");
            return null;
        }

        if (DLogConfig.getConfig().getReportConfig() == null) {
            Logger.e(TAG,"异常：DLogBaseConfigProvider没有配置，无法上报");
            return null;
        }

        //记录即将要上报的日志的uuids
        Logger.d(TAG,"即将要上报数据长度为：" + dLogModels.size());
        String[] uuids = new String[dLogModels.size()];
        for (int i = 0; i < dLogModels.size(); i++) {
            DLogModel model = dLogModels.get(i);
            uuids[i] = model.getUuid();
            Logger.d(TAG,"即将要上报的所有数据为：" + "length=" + dLogModels.size() + ",id=" + model.getId() + ",uuid=" + model.getUuid() + ",content=" + model.toString());
        }

        DLogSyncReportResult dLogSyncReportResult = DLogConfig.getConfig().getReportConfig().reportSync(dLogModels);
        if (dLogSyncReportResult == null) {
            Logger.e(TAG,"异常：上层的reportSync不能返回为null");
            return null;
        }

        return new ResultObj(uuids, dLogSyncReportResult);

    }


    private class ResultObj {
        private String[] uuids;
        private DLogSyncReportResult result;

        public ResultObj(String[] uuids, DLogSyncReportResult result) {
            this.uuids = uuids;
            this.result = result;
        }

        public String[] getUuids() {
            return uuids;
        }

        public void setUuids(String[] uuids) {
            this.uuids = uuids;
        }

        public DLogSyncReportResult getResult() {
            return result;
        }

        public void setResult(DLogSyncReportResult result) {
            this.result = result;
        }
    }


    /**
     * 检查延时上报时间
     *
     * @return true：可以上报；false：不可以上报
     */
    public static boolean reportCheck() {

        long lastReportTime = PrefHelper.getLong(REPORT_TIME_SP_KEY, 0);
        long curTime = System.currentTimeMillis();
        if (DLogConfig.getConfig().getBaseConfig() != null && DLogConfig.getConfig().getBaseConfig().reportDelay() > 0) { //延时>0的情况下，开启延时上报策略。
            if (curTime - lastReportTime > DLogConfig.getConfig().getBaseConfig().reportDelay()) {
                Logger.d("DLogReportService", "延时上报策略通过，进入上报队列 " + "lastTime = " + lastReportTime + ",curTime=" + curTime + "，线程名:" + Thread.currentThread().getName());
                return true; //时间合理通过
            }
        }
        return false;
    }
}
