package com.shuai.example.dlog;

import android.app.Application;

import com.shuai.dlog.config.DLogConfig;
import com.shuai.example.dlog.config.AppDLogBaseConfig;
import com.shuai.example.dlog.config.AppDLogReportConfig;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * 初始化DLog
         */
        DLogConfig.init(this)
                .baseConfig(new AppDLogBaseConfig())
                .reportConfig(new AppDLogReportConfig());

    }
}
