package com.shuai.example.dlog.config;

import com.shuai.dlog.config.DLogBaseConfigProvider;
import com.shuai.example.dlog.BuildConfig;

public class AppDLogBaseConfig extends DLogBaseConfigProvider {
    /**
     * 是否是debug模式
     *
     * @return
     */
    @Override
    public boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    /**
     * 延时上报策略配置。默认为5分钟
     * 若<=0.则取消延时上报策略
     *
     * @return
     */
    @Override
    public long reportDelay() {
        return 5 * 1000;//为实现效果，这里定义为5秒
    }

    /**
     * 定时上报策略配置。默认为10分钟
     * 若<=0.则取消定时上报策略
     *
     * @return
     */
    @Override
    public long reportAlarm() {
        return 15 * 1000;//为实现效果，这里定义为15秒
    }
}
