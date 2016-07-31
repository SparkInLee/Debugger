package com.lee.app.debug;

import android.app.Application;
import android.content.Intent;

import com.lee.sdk.service.DebuggerService;

/**
 * Created by jianglee on 7/30/16.
 */
public class DebugApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(DebugApp.this, DebuggerService.class);
        startService(intent);
    }
}
