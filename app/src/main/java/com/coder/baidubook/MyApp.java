package com.coder.baidubook;

import android.app.Application;

//import com.tencent.smtt.sdk.QbSdk;

/**
 * Created by YandZD on 2017/11/28.
 */

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
//
//            @Override
//            public void onViewInitFinished(boolean arg0) {
//                // TODO Auto-generated method stub
//            }
//
//            @Override
//            public void onCoreInitFinished() {
//            }
//        };
        //x5内核初始化接口
//        QbSdk.initX5Environment(getApplicationContext(), cb);
    }
}
