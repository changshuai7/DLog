package com.shuai.dlog.config;

import android.app.Application;
import android.support.annotation.NonNull;

import com.shuai.dlog.utils.Logger;

public class DLogConfig {

    private static DLogConfig.Config sConfig;
    private static Application sApp;

    public static Config getConfig() {
        return sConfig;
    }

    public static Application getApp() {
        return sApp; // TODO 可能会有多进程的问题。暂时先不处理，遇到问题再另行解决。
    }


    /**
     * 初始化
     *
     * @param app Application
     * @return 配置文件
     */
    public static DLogConfig.Config init(@NonNull final Application app) {
        sApp = app;
        if (sConfig == null) {
            sConfig = new DLogConfig.Config();
        }
        return sConfig;
    }


    public static class Config {

        private DLogBaseConfigProvider baseConfig = null;
        private DLogReportConfigProvider reportConfig = null;

        public Config baseConfig(DLogBaseConfigProvider baseConfig) {
            this.baseConfig = baseConfig;
            Logger.debug(baseConfig.isDebug());//设置log日志模式
            return this;
        }

        public Config reportConfig(DLogReportConfigProvider reportConfig) {
            this.reportConfig = reportConfig;
            return this;
        }

        public DLogBaseConfigProvider getBaseConfig() {
            return baseConfig;
        }

        public DLogReportConfigProvider getReportConfig() {
            return reportConfig;
        }
    }
}
