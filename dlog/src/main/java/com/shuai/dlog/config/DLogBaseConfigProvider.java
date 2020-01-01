package com.shuai.dlog.config;

/**
 * DLog基本配置
 * @author changshuai
 */
abstract public class DLogBaseConfigProvider {

    /**
     * 是否debug模式
     *
     * @return
     */
    public abstract boolean isDebug();

    /**
     * 延时上报配置。默认为5分钟
     * 若<=0.则取消延时上报策略
     *
     * @return
     */
    public long reportDelay() {
        return 5 * 60 * 1000;
    }

    /**
     * 定时上报配置。默认为10分钟
     * 若<=0.则取消定时上报策略
     *
     * @return
     */
    public long reportAlarm() {
        return 10 * 60 * 1000;
    }


}
