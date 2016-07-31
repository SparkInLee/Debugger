// IDebugger.aidl
package com.lee.sdk.service;

import android.os.IBinder;
// Declare any non-default types here with import statements

interface IDebugger {
    int register(String name, IBinder client);
    void unregister(int id);
}
