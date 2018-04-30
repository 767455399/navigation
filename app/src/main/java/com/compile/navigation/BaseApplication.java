package com.compile.navigation;

import android.app.Application;
import android.content.Context;

import com.iflytek.cloud.SpeechUtility;

/**
 * Created by wangqing on 2018/4/28.
 */

public class BaseApplication extends Application {
    private static final String TAG = "BaseApplication";
    private static Context applicationContext;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = getApplicationContext();
        //科大讯飞初始化
        //  Setting.setShowLog(true); //设置日志开关（默认为true），设置成false时关闭语音云SDK日志打印
        SpeechUtility.createUtility(BaseApplication.this, "appid=5ae41a92");
//        new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
//            @Override
//            public void run() {
//                SpeechUtility.createUtility(applicationContext, "appid=5ae41a92");//=号后面写自己应用的APPID
//                Log.d(TAG, "run: 初始化结束");
//            }
//        });

    }



    public static Context getContext() {
        return applicationContext;
    }

    public static void setContext(Context context) {
        if (applicationContext == null) {
            applicationContext = context;
        }
    }
}
