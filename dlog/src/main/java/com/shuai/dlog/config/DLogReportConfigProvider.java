package com.shuai.dlog.config;


import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogReportCallback;

import java.util.List;

abstract public class DLogReportConfigProvider {

    /**
     * 上报。请在上层网络请求中完成，并通过callback回调给DLog库
     *
     * @param models
     * @param callback
     */
    abstract public void report(List<DLogModel> models, DLogReportCallback callback);

}
