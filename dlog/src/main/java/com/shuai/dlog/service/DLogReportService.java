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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
            reportSync();
        }else{
            Logger.e("网络连接不可用");
        }
    }


    /**
     * 同步上报网络请求
     * 超时时间为：LogConfig.getConfig().getReportConfig().reportSyncTimeOut()
     */
    private void reportSync(){
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
            result = futureTask.get(DLogConfig.getConfig().getReportConfig().reportSyncTimeOut(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException |TimeoutException e) {
            e.printStackTrace();
            result = new ResultObj(new ArrayList<String>(),DLogSyncReportResult.FAIL);
        }finally {
            executorService.shutdown();
            futureTask.cancel(true);
        }

        if (result != null) {
            if (result.getResult() == DLogSyncReportResult.SUCCESS){
                Logger.d("【日志数据上报成功】"+"uuid为"+result.getUuids().toString());

                PrefHelper.remove(REPORT_ERROR_COUNT_SP_KEY);//失败次数重置为0
                String[] uuidArr = new String[result.uuids.size()];
                for (int i = 0;i<result.getUuids().size();i++){
                    uuidArr[i] = result.getUuids().get(i);
                }
                DLogDBDao.getInstance(DLogConfig.getApp()).deleteLogDatasByUuid(uuidArr);//TODO 删除可能会失败。。。
                //注意：这里不可以清空所有日志，万一在上报的过程中，有新的日志写入呢？必须根据日志的uuid删除才可靠。
                //DLogDBDao.getInstance(DLogConfig.getApp()).deleteAllLogDatas();//日志记录全部清空

            }else if(result.getResult() == DLogSyncReportResult.FAIL) {

                int anInt = PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY, 0);
                PrefHelper.setInt(REPORT_ERROR_COUNT_SP_KEY,++anInt);//失败次数增加1次

                Logger.e("【日志数据上报失败】"+"第"+PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY,0)+"次");

                if (PrefHelper.getInt(REPORT_ERROR_COUNT_SP_KEY, 0)<3){
                    DLog.sendAll();//上报失败以后，会强制再次重试。超过3次以后，不再上报。
                }
            }else{
                //..一定不会执行的
                Logger.e("见鬼了，执行了SUCCESS和FAIL以外的内容");
            }
        }else{
            Logger.e("ResultObj结果为null");
        }
    }


    private ResultObj getSyncReportResult() {
        PrefHelper.setLong(REPORT_TIME_SP_KEY, System.currentTimeMillis());

        List<DLogModel> dLogModels = DLogDBDao.getInstance(DLogConfig.getApp()).loadAllLogDatas();

        List<String> uuids= new ArrayList<>();
        if (dLogModels != null) {
            for (DLogModel model : dLogModels) {
                uuids.add(model.getUuid());
                Logger.d("即将要上报的所有数据：" + "length=" + dLogModels.size() + ",id=" + model.getId() + ",uuid=" + model.getUuid() +",content="+ model.toString());
            }
        }

        if (DLogConfig.getConfig().getReportConfig() != null) {
            DLogSyncReportResult dLogSyncReportResult = DLogConfig.getConfig().getReportConfig().reportSync(dLogModels);
            if (dLogSyncReportResult==null){
                Logger.e("上层的reportSync不能返回为空");
                return null;
            }
            return new ResultObj(uuids,dLogSyncReportResult);
        }else{
            Logger.e("DLogBaseConfigProvider没有配置！");
            return null;
        }
    }


    private class ResultObj {
        private List<String> uuids;
        private DLogSyncReportResult result;

        public ResultObj(List<String> uuids, DLogSyncReportResult result) {
            this.uuids = uuids;
            this.result = result;
        }

        public List<String> getUuids() {
            return uuids;
        }

        public void setUuids(List<String> uuids) {
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
     * 检查上报时间
     *
     * @return true：可以上报；false：不可以上报
     */
    public static boolean reportCheck() {

        long lastReportTime = PrefHelper.getLong(REPORT_TIME_SP_KEY, 0);
        long curTime = System.currentTimeMillis();
        if (DLogConfig.getConfig().getBaseConfig() != null && DLogConfig.getConfig().getBaseConfig().reportDelay() > 0) { //延时>0的情况下，开启延时上报策略。
            if (curTime - lastReportTime > DLogConfig.getConfig().getBaseConfig().reportDelay()) {
                Logger.d("DLogReportService","延时上报策略通过，进入上报队列 "+"lastTime = "+lastReportTime+",curTime="+curTime +"，线程名:"+Thread.currentThread().getName());
                return true; //时间合理通过
            }
        }
        return false;
    }
}
