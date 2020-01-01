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
             * 延时上报策略配置
             * 若<=0.则取消延时上报策略
             * @return
             */
            @Override
            public long reportDelay() {
                return 5 * 1000;//为实现效果，这里定义为5秒
            }

            /**
             * 定时上报策略配置
             * 若<=0.则取消定时上报策略
             * @return
             */
            @Override
            public long reportAlarm() {
                return 15*1000;//为实现效果，这里定义为15秒
            }
        });
    }
}
