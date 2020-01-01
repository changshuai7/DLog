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

### 2.打点策略
DLog有三种打点策略:

1、策略一：轮循上报，通过reportAlarm()配置定时轮循间隔时间。配置若<=0.则取消此策略

2、策略二、延时上报，通过reportDelay()配置延时上报间隔时间。配置若<=0.则取消此策略。即在写入日志的时候记录时间，下次再写入日志时，如果两次写入日志间隔的时间大于约定的reportDelay()时间，那么立即上报，否则不上报。

3、策略三、强制上报。对于一些非常紧急的日志，可以执行DLog.sendAll(); 强制立即上报所有日志。

注意：上报的日志均为全量日志。


### 3.初始化。
在Application中初始化

```
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
         * 延时上报策略配置。默认为5分钟
         * 若<=0.则取消延时上报策略
         * @return
         */
        @Override
        public long reportDelay() {
                return 5 * 1000;//为实现效果，这里定义为5秒
        }

        /**
         * 定时上报策略配置，默认为10分钟
         * 若<=0.则取消定时上报策略
         * @return
         */
        @Override
        public long reportAlarm() {
                return 15*1000;//为实现效果，这里定义为15秒
        }
});
```

在常驻项目的Activity或者Service中做如下配置
```
DLogConfig.getConfig().reportConfig(new DLogReportConfigProvider() {
    @Override
    public DLogSyncReportResult reportSync(List<DLogModel> models) {

        Logger.d("-----上层准备请求网络上报------");
        /**
         * 这里的List<DLogModel> models为数据库中所有的日志内容。
         * 建议开发者在此处上层通过自己的网络请求将日志上传到服务器，同时给DLog一个结果回调
         */
        try {
            Thread.sleep(2000);//模拟网络请求，耗时2秒上报
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return DLogSyncReportResult.SUCCESS;//成功回调
       //  return DLogSyncReportResult.FAIL;//失败回调
    }

    /**
     * 设置reportSync超时时间，超过此时间，默认返回DLogSyncReportResult.FAIL
     * @return
     */
    @Override
    public long reportSyncTimeOut() {
        return 5*1000;
    }
});
```
注意

1、reportSync方法为非主线程，请勿直接做和UI相关的内容。

2、reportSync中务必要执行阻塞方法，即同步耗时的方法，如同步的网络请求等。

3、reportSync方法可以根据你的业务扩展，比如将日志定期存入文件，通过网络上报等。只要保证同步执行耗时任务即可。

4、reportSync默认有超时时间，可通过reportSyncTimeOut配置，默认为60秒。超过此时间，默认返回DLogSyncReportResult.FAIL。

## 三、使用

1、DB中存储的数据Model：

```
private long id;//数据库自增id，无实际意义
private String uuid;//uuid全球唯一识别码
private String type;//类型
private long timestamp;//时间戳
private String content;//内容
```
开发者可以根据自己情况取使用type和content

2、存数据：
```
DLog.write(String type, T content); //T为javabean数据 务必保证通过Gson.toJson转换不会出错。
```
3、立即上传数据（全量数据）
```
DLog.sendAll();
```

4、删数据(全量数据)
```
DLog.deleteAll();
```
5、获取数据(全量数据)
```
DLog.getAll();
返回List<DLogModel>
```

## 四、注意


1、上报日志成功（得到上层返回的成功回调）以后，会直接删除日志。

2、上报出错以后（得到上层返回的失败回调）会有三次重试机会。三次失败以后，不再重试。直到以后上报成功，则重新计次。

3、开发者无需关心日志的丢失、重复等问题，DLog内部已经统统处理好。