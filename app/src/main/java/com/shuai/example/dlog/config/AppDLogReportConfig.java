package com.shuai.example.dlog.config;

import android.content.Context;

import com.shuai.dlog.config.DLogReportConfigProvider;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogSyncReportResult;
import com.shuai.dlog.utils.Logger;

import java.util.List;

public class AppDLogReportConfig extends DLogReportConfigProvider {
    @Override
    public DLogSyncReportResult reportSync(Context context, List<DLogModel> models) {

        Logger.d("-----上层准备请求网络上报------:"+context.toString());

        /**
         * 这里的List<DLogModel> models为数据库中所有的日志内容。
         * 建议开发者在此处上层通过自己的网络请求将日志上传到服务器，同时给DLog一个结果回调
         */

        try {
            Thread.sleep(2000);//模拟网络请求，耗时2秒上报
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return DLogSyncReportResult.SUCCESS;

    }

    /**
     * 设置reportSync超时时间，超过此时间，默认返回DLogSyncReportResult.FAIL
     * @return
     */
    @Override
    public long reportSyncTimeOut() {
        return 5*1000;
    }
}
