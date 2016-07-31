package com.lee.app.master;

import android.app.Application;
import android.content.Context;

import com.lee.sdk.framework.Client;
import com.lee.sdk.framework.DebuggerUtil;

/**
 * Created by jianglee on 7/30/16.
 */
public class MasterApp extends Application {

    public static Application sInstance = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        sInstance = MasterApp.this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new DBHelper(sInstance).getWritableDatabase();
        SPManager.getInstance().setTime();
        if (FileManager.getInstance().isDebug()) {
            DebuggerUtil.getInstance().bind(sInstance, new Client(sInstance), "com.lee.app.debug");
        }
    }
}
