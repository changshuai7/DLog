package com.shuai.example.dlog;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.shuai.dlog.DLog;
import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.config.DLogReportConfigProvider;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogSyncReportResult;
import com.shuai.dlog.utils.Logger;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initReportDLog();

        /**
         * 写日志，支持多线程
         */
        findViewById(R.id.insertData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TestLogBean bean = new TestLogBean();
                bean.setName("小黑0");
                bean.setAge(40);
                DLog.write(Thread.currentThread().getName() + "", bean);


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TestLogBean bean = new TestLogBean();
                        bean.setName("小红");
                        bean.setAge(120);
                        DLog.write(Thread.currentThread().getName() + "", bean);
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TestLogBean bean = new TestLogBean();
                        bean.setName("小蓝");
                        bean.setAge(200);
                        DLog.write(Thread.currentThread().getName() + "", bean);
                    }
                }).start();


            }
        });

        /**
         * 上传日志
         */
        findViewById(R.id.updateData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DLog.sendAll();
            }
        });

        /**
         * 获取所有数据
         */
        findViewById(R.id.getAllData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<DLogModel> all = DLog.getAll();
                for (DLogModel model:all){
                    Logger.d("还剩数据：" + "length=" + all.size() + ",id=" + model.getId() + ",uuid=" + model.getUuid() +",content=", model.toString());
                }
                Logger.d("还剩数据条数："+all.size());

            }
        });
    }


    /**
     * 初始化上传日志配置
     */
    private void initReportDLog() {
        DLogConfig.getConfig().reportConfig(new DLogReportConfigProvider() {
            @Override
            public DLogSyncReportResult reportSync(List<DLogModel> models) {

                Logger.d("-----上层准备请求网络上报------");
                /**
                 * 这里的List<DLogModel> models为数据库中所有的日志内容。
                 * 建议开发者在此处上层通过自己的网络请求将日志上传到服务器，同时给DLog一个结果回调
                 *
                 */
                try {
                    Thread.sleep(2000);//模拟网络请求，耗时2秒上报
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return DLogSyncReportResult.SUCCESS;
            }

            /**
             * 设置reportSync超时时间，超过此时间，默认返回DLogSyncReportResult.FAIL
             * @return
             */
            @Override
            public long reportSyncTimeOut() {
                return 3*1000;
            }
        });


    }


}
