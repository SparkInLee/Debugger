package com.lee.sdk.framework;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.IBinder;
import android.os.RemoteException;

import com.lee.sdk.service.DebuggerService;
import com.lee.sdk.service.IDebugger;

import java.util.Arrays;

/**
 * Utils which is used to bind and unbind a {@link Client} or a CustomClient which extends from {@link Client} to {@link DebuggerService}
 * Code Example:
 * <code>
 *     public CustomApplication extends Application{
 *         @Override
 *         public void onCreate(){
 *             DebuggerUtil.getInstance().bind(getApplicationContext(), new Client(getApplicationContext()));
 *         }
 *     }
 * </code>
 * You'd better to unbind the client when you app exit.
 * Of course, you can bind or unbind the client to debuggerService anytime you want.
 *
 * @see DebuggerService
 * @see Client
 *
 * @author jiangli
 */
public class DebuggerUtil {
    private static final String TAG = "DebuggerUtil";

    private static DebuggerUtil sInstance = null;

    private ThreadLocal<Integer> bindId = new ThreadLocal<Integer>();
    private ThreadLocal<IDebugger> bindService = new ThreadLocal<IDebugger>();
    private ThreadLocal<Context> context = new ThreadLocal<Context>();
    private ThreadLocal<ServiceConnection> serviceConnection = new ThreadLocal<ServiceConnection>();

    public static DebuggerUtil getInstance() {
        if (null == sInstance) {
            synchronized (DebuggerUtil.class) {
                if (null == sInstance) {
                    sInstance = new DebuggerUtil();
                }
            }
        }

        return sInstance;
    }

    private DebuggerUtil() {
        bindId.set(-1);
    }

    public void bind(final Context context, final Client client, String debugPackage) {
        this.context.set(context);
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pInfo = pm.getPackageInfo(debugPackage, PackageManager.GET_SIGNATURES);
            if (null != pInfo) {
                Signature[] mine = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
                if (Arrays.equals(pInfo.signatures, mine)) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(debugPackage, "com.lee.sdk.service.DebuggerService"));
                    serviceConnection.set(new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            if (service.isBinderAlive()) {
                                IDebugger debugger = IDebugger.Stub.asInterface(service);
                                bindService.set(debugger);
                                try {
                                    bindId.set(debugger.register(context.getPackageName(), client.asBinder()));
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            bindId.set(-1);
                            bindService.set(null);
                            DebuggerUtil.this.context.set(null);
                            serviceConnection.set(null);
                        }
                    });
                    context.bindService(intent, serviceConnection.get(), Service.BIND_AUTO_CREATE);
                } else {
                    Logger.i(TAG, "Signature is not matched.");
                }
            }
        } catch (Exception e) {
            Logger.i(TAG, debugPackage + " is not installed or Signature is not matched.");
        }
    }

    public void unbind() {
        if (bindId.get() != -1 && bindService.get() != null) {
            try {
                bindService.get().unregister(bindId.get());
                context.get().unbindService(serviceConnection.get());
            } catch (RemoteException e) {
                // no-op
            }
        }
        bindId.set(-1);
        bindService.set(null);
        context.set(null);
        serviceConnection.set(null);
    }
}
