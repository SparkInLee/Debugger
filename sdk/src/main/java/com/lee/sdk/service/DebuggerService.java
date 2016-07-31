package com.lee.sdk.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import com.lee.sdk.framework.IClientManager;
import com.lee.sdk.framework.Logger;
import com.lee.sdk.framework.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Debugger is the bridge between The Debug Application and The Monitor Client.
 * Firstly, Debugger will start a {@link Server} based on Socket when DebuggerService is running;
 * Secondly, Debugger will storage the Debug Application when the Application register to Debugger.
 * When all the steps is done, the Monitor Client can send Command to Debugger, and Debugger get
 * the response by invoking {@link com.lee.sdk.framework.Client#executeCommand(int, String)}, and
 * then send the response to Monitor Client.
 * The Client will be removed when the Application unbind DebuggerService or get {@link RemoteException}
 * when invoke the Application. And the strategy may be changed in the future, maybe unregister the
 * Application by using {@link IBinder#linkToDeath(IBinder.DeathRecipient, int)}
 *
 * @see Server
 * @see com.lee.sdk.framework.DebuggerUtil#bind(Context, IClient)
 *
 * @author jiangli
 */
public class DebuggerService extends Service implements IClientManager {
    private static final String TAG = "DebuggerService";

    private Object lock = new Object();
    private Map<Integer, IClient> idToClients = new HashMap<Integer, IClient>();
    private Map<String, Integer> nameToClients = new HashMap<String, Integer>();
    private AtomicInteger clientId = new AtomicInteger(1);

    class Debugger extends IDebugger.Stub {

        @Override
        public int register(String name, IBinder client) throws RemoteException {
            Logger.i(TAG, "register");
            IClient asClient = null;
            if (client instanceof IClient) {
                asClient = (IClient) client;
            } else {
                asClient = IClient.Stub.asInterface(client);
            }
            int id = -1;
            synchronized (lock) {
                if (nameToClients.containsKey(name)) {
                    id = nameToClients.get(name);
                    idToClients.put(id, asClient);
                } else {
                    id = clientId.getAndIncrement();
                    idToClients.put(id, asClient);
                    nameToClients.put(name, id);
                }
            }
            Logger.i(TAG, "app register : {package:" + name + ",id:" + id + "}");
            return id;
        }

        @Override
        public void unregister(int id) throws RemoteException {
            Logger.i(TAG, "unregister");
            synchronized (lock) {
                idToClients.remove(id);
                Logger.i(TAG, "app unregister : {id:" + id + "}");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.i(TAG, "onBind");
        return new Debugger();
    }

    @Override
    public void onCreate() {
        Server.getInstance().start(this);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Server.getInstance().stop();
        super.onDestroy();
    }

    @Override
    public IClient getClient(String name) {
        synchronized (lock) {
            IClient client = null;
            Integer id = nameToClients.get(name);
            if (null != id) {
                client = idToClients.get(id);
                if (null == client) {
                    nameToClients.remove(name);
                }
            }
            return client;
        }
    }

    @Override
    public void removeClient(String name) {
        synchronized (lock) {
            Integer id = nameToClients.get(name);
            idToClients.remove(id);
            nameToClients.remove(name);
            Logger.i(TAG, "app remove : {package:" + name + ",id:" + id + "}");
        }
    }
}
