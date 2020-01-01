package com.shuai.dlog.config;


import com.shuai.dlog.constant.DLogConstant;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogSyncReportResult;

import java.util.List;

/**
 * 上报配置
 * @author changshuai
 */
abstract public class DLogReportConfigProvider {

    /**
     * 上报。请在上层网络请求中完成，通过DLogSyncReportResult返回结果。必须在方法中同步网络请求，执行耗时操作。
     * @param models
     * @return
     */
    abstract public DLogSyncReportResult reportSync(List<DLogModel> models);

    /**
     * 设置同步请求的超时时间
     * 默认为 1分钟
     * @return
     */
    public long reportSyncTimeOut(){return DLogConstant.REPORT_SYNC_TIMEOUT;}

}
