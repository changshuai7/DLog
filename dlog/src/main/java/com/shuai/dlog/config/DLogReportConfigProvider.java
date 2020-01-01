package com.shuai.dlog.config;


import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogReportCallback;
import com.shuai.dlog.report.DLogSyncReportResult;

import java.util.List;

abstract public class DLogReportConfigProvider {

    /**
     * 上报。请在上层网络请求中完成，并通过callback回调给DLog库。建议在方法中同步网络请求，执行耗时操作。
     *
     * @param models
     * @param callback
     */

    /**
     * 上报。请在上层网络请求中完成，通过DLogSyncReportResult返回结果。必须在方法中同步网络请求，执行耗时操作。
     * @param models
     * @return
     */
    abstract public DLogSyncReportResult reportSync(List<DLogModel> models);

    /**
     * 设置同步请求的耗时时间
     * @return
     */
    public long reportSyncTimeOut(){return 3*1000;}

}
