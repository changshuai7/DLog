package com.shuai.example.dlog;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import com.shuai.dlog.DLog;
import com.shuai.dlog.model.DLogModel;
import com.shuai.dlog.utils.Logger;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * 写日志，支持多线程
         */
        findViewById(R.id.insertData).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                for (int i = 0;i<500;i++){
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            TestLogBean bean = new TestLogBean();
                            bean.setName("小红");
                            bean.setAge(120);
                            DLog.write(Thread.currentThread().getName() + "", bean);
                        }
                    }).start();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            TestLogBean bean = new TestLogBean();
                            bean.setName("小蓝");
                            bean.setAge(200);
                            DLog.write(Thread.currentThread().getName() + "", bean);
                        }
                    }).start();
                }


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


}
