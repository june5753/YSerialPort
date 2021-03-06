package com.yujing.chuankou;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.yujing.ycrash.YCrash;


public class App extends Application {

    public static App INSTANCE;

    public static App getInstance() {
        if (INSTANCE == null) {
            synchronized (App.class) {
                if (INSTANCE == null) {
                    INSTANCE = new App();
                }
            }
        }
        return INSTANCE;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE=this;
        YCrash.getInstance().init(this);
        YCrash.getInstance().setAppName(getString(R.string.app_name));
        YCrash.getInstance().setCrashInfoListener(appInfo -> Log.e("崩溃拦截",appInfo.崩溃信息));
    }
}
