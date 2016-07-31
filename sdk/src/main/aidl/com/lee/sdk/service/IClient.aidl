// IClient.aidl
package com.lee.sdk.service;

import android.os.ParcelFileDescriptor;

// Declare any non-default types here with import statements

interface IClient {
    String executeCommand(int code, String param);
    ParcelFileDescriptor getFileDescriptor();
}
