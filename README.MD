# DLog:高效轻量打点库
## 写在前边


本库实现了app中的频繁打点需求，实现了多线程数据保护，数据存储于db中，更高效


## 一、框架结构

略


## 二、集成方式

### 1.引入。

```
dependencies {
        implementation 'com.shuai:dlog:version'
}

版本号 一般采用Tag中最新的版本。
```


### 2.初始化。
在Application中初始化

```
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
```

在常驻项目的Activity或者Service中做如下配置
```
DLogConfig.getConfig().reportConfig(new DLogReportConfigProvider() {
    @Override
    public void report(List<DLogModel> models, DLogReportCallback callback) {

        /**
         * 这里的List<DLogModel> models为数据库中所有的日志内容。
         * 建议开发者在此处上层通过自己的网络请求将日志上传到服务器，同时给DLog一个结果回调
         *
         */

        callback.onSuccess();
        //callback.onFail("发生错误");

    }
});
```
需要注意的是report方法非同步线程，请勿直接做和UI相关的内容。
## 三、使用

1、DB中存储的数据Model：

```
private long id;//数据库自增id，无实际意义
private String type;//类型
private long timestamp;//时间戳
private String content; //内容
```
开发者可以根据自己情况取使用type和content

2、存数据：
```
DLog.write(String type, T content);
```
3、立即上传数据（全量数据）
```
DLog.sendAll();
```

4、删数据(全量数据)
```
DLog.deleteAll();
```
5、获取数据
```
DLog.getAll();
返回List<DLogModel>
```

## 四、注意
1、为了防止打点过频引起的接口请求过于频繁，库中默认采取了延时策略。两次上报log的时间隔不会小于reportDelay所限定的时间。即：在DLog.write()时，如果距离上次上报时间小于reportDelay则不上报。反之，则立即上报


2、每次上报日志成功（得到上层返回的成功回调）以后，会直接删除日志。


3、上报出错以后（得到上层返回的失败回调）会有三次重试机会。三次失败以后，不再重试。直到以后上报成功，则重新计次。