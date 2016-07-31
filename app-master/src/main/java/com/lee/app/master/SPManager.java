package com.lee.app.master;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by jianglee on 7/30/16.
 */
public class SPManager {

    private SharedPreferences spManager;

    private static SPManager sInstance = null;

    public static SPManager getInstance() {
        if (null == sInstance) {
            synchronized (SPManager.class) {
                if (null == sInstance) {
                    sInstance = new SPManager();
                }
            }
        }

        return sInstance;
    }

    private SPManager() {
        spManager = MasterApp.sInstance.getSharedPreferences("debugger", Context.MODE_PRIVATE);
    }

    public void setTime() {
        spManager.edit().putString("time", String.valueOf(System.currentTimeMillis())).commit();
    }
}
