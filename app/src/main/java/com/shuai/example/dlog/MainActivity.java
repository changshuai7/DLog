package com.shuai.example.dlog;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.shuai.dlog.DLog;
import com.shuai.dlog.config.DLogConfig;
import com.shuai.dlog.config.DLogReportConfigProvider;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.report.DLogReportCallback;

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
                bean.setName("小黑");
                bean.setAge(40);
                DLog.write("thread-main", bean);


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TestLogBean bean = new TestLogBean();
                        bean.setName("小红");
                        bean.setAge(12);
                        DLog.write("thread-1", bean);
                    }
                }).start();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        TestLogBean bean = new TestLogBean();
                        bean.setName("小蓝");
                        bean.setAge(20);
                        DLog.write("thread-2", bean);
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
    }


    /**
     * 初始化上传日志配置
     */
    private void initReportDLog() {
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
    }


}
