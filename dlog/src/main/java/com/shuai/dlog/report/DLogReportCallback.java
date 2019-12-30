package com.shuai.dlog.report;

/**
 * 日志上报结果回调
 */
public interface DLogReportCallback {

    void onSuccess();

    void onFail(String msg);

}
