package com.shuai.dlog.config;


abstract public class DLogBaseConfigProvider {

    /**
     * 是否debug模式
     *
     * @return
     */
    public abstract boolean isDebug();

    /**
     * 上报延时配置
     *
     * @return
     */
    public long reportDelay() {
        return 5 * 60 * 1000;
    }


}
