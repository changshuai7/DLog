package com.shuai.example.dlog;

import android.app.Application;

import com.shuai.dlog.config.DLogBaseConfigProvider;
import com.shuai.dlog.config.DLogConfig;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * 初始化DLog
         */
        DLogConfig.init(this).baseConfig(new DLogBaseConfigProvider() {
            /**
             * 是否是debug模式
             * @return
             */
            @Override
            public boolean isDebug() {
                return BuildConfig.DEBUG;
            }

            /**
             * 上报延时。默认为5分钟。可复写实现自己的延时策略。
             * @return
             */
            @Override
            public long reportDelay() {
                return 5 * 1000;
            }
        });
    }
}
