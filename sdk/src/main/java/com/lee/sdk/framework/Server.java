package com.lee.sdk.framework;

import static com.lee.sdk.framework.Constants.*;

import android.os.RemoteException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.lee.sdk.service.IClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Debug Server which wait for the command from Monitor Client, then send the
 * Command to Application Client, and then send the response which returned by
 * Application Client to Monitor Client.
 *
 * Command Protocol (Based On JSON)
 * <code>
 *     {
 *         "code":1,
 *         "name":"com.lee.example",
 *         "param":"..."
 *     }
 * </code>
 * The code is used to resolve the Command Entry Point
 * The name is used to resolve the Application Client
 * The param is optional, and will be sent to Application without any processing.
 *
 * Response Protocol (Based on JSON)
 * <code>
 *     {
 *         "type":"0",
 *         "data":"..."
 *     }
 * </code>
 * The type should be:
 *      {@link Constants#RESULT_TYPE_STRING}
 *      {@link Constants#RESULT_TYPE_JSON}
 *      {@link Constants#RESULT_TYPE_NORMAL_FILE}
 * If the type is not {@link Constants#RESULT_TYPE_NORMAL_FILE}, then the data is String.
 * If the type is {@link Constants#RESULT_TYPE_NORMAL_FILE}, then the data is:
 * <code>
 *     {
 *         "path":"some path",
 *         "size":1024
 *     }
 * </code>
 * The path is the file path, and the size is the file size.
 *
 *
 * @see Client
 * @see com.lee.sdk.service.DebuggerService
 *
 * @author jiangli
 */
public class Server {
    private static final String TAG = "Server";

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private IClientManager clientManager = null;

    private ExecutorService service = Executors.newFixedThreadPool(5);

    private static Server sInstance = null;

    public static Server getInstance() {
        if (null == sInstance) {
            synchronized (Server.class) {
                if (null == sInstance) {
                    sInstance = new Server();
                }
            }
        }

        return sInstance;
    }

    private Server() {

    }

    public void start(IClientManager clientManager) {
        Logger.di(TAG, "server start");
        if (null == clientManager)
            throw new IllegalArgumentException("clientManager can not be null.");
        this.clientManager = clientManager;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocketChannel = ServerSocketChannel.open();
                    serverSocketChannel.configureBlocking(false);
                    ServerSocket serverSocket = serverSocketChannel.socket();
                    int retry = 0;
                    int port = DEFAULT_PORT;
                    while (true) {
                        try {
                            serverSocket.bind(new InetSocketAddress(DEFAULT_PORT));
                            break;
                        } catch (IOException e) {
                            if (retry++ > 10) {
                                throw e;
                            }
                        }
                    }
                    if (retry > 0) {
                        Logger.i(TAG, String.format("%d is in use, change to port :%d", DEFAULT_PORT, port));
                    }
                    selector = Selector.open();
                    serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                    while (true) {
                        selector.select();
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        if (null != selectionKeys && selectionKeys.size() > 0) {
                            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
                            while (selectionKeyIterator.hasNext()) {
                                SelectionKey tempSelectKey = selectionKeyIterator.next();
                                selectionKeyIterator.remove();
                                if (tempSelectKey.isAcceptable()) {
                                    try {
                                        ServerSocketChannel serverChannel = (ServerSocketChannel) tempSelectKey.channel();
                                        SocketChannel clientChannel = serverChannel.accept();
                                        clientChannel.configureBlocking(false);
                                        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE)
                                                .interestOps(SelectionKey.OP_READ);
                                    } catch (IOException e) {
                                        closeKeySafely(tempSelectKey);
                                    }
                                } else if (tempSelectKey.isReadable()) {
                                    tempSelectKey.interestOps(0);
                                    handleRequest(tempSelectKey);
                                } else if (tempSelectKey.isWritable()) {
                                    tempSelectKey.interestOps(0);
                                    handleResopnse(tempSelectKey);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    Logger.d(TAG, e);
                } finally {
                    stop();
                }
            }
        }).start();
    }

    public void stop() {
        Logger.di(TAG, "server stop");
        if (null != selector) {
            try {
                selector.close();
            } catch (IOException e) {
                // no-op
            } finally {
                selector = null;
            }
        }
        if (null != serverSocketChannel) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                // no-op
            } finally {
                serverSocketChannel = null;
            }
        }
    }

    private void handleRequest(final SelectionKey selectionKey) {
        service.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (selectionKey) {
                    Logger.dd(TAG, "handle request");
                    SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                    byte[] body = new byte[512];
                    ByteBuffer buffer = ByteBuffer.wrap(body);
                    buffer.clear();
                    try {
                        while (true) {
                            int len = clientChannel.read(buffer);
                            if (len >= body.length) {
                                int pos = buffer.position();
                                byte[] newBody = new byte[body.length + 256];
                                System.arraycopy(body, 0, newBody, 0, body.length);
                                body = newBody;
                                buffer = ByteBuffer.wrap(body);
                                buffer.position(pos);
                            } else {
                                break;
                            }
                        }
                        byte[] data = new byte[buffer.position()];
                        buffer.flip();
                        buffer.get(data);
                        if (null != selectionKey.attachment()) {
                            byte[] appendTo = (byte[]) selectionKey.attachment();
                            byte[] newData = new byte[data.length + appendTo.length];
                            System.arraycopy(appendTo, 0, newData, 0, appendTo.length);
                            System.arraycopy(data, 0, newData, appendTo.length, data.length);
                            data = newData;
                        }
                        selectionKey.attach(data);
                        buffer = ByteBuffer.wrap(data);
                        if (buffer.remaining() >= 4) {
                            int paramsLen = buffer.getInt();
                            Logger.d(TAG, "expected : " + paramsLen + ", actual : " + data.length);
                            if (paramsLen <= data.length) {
                                selectionKey.interestOps(SelectionKey.OP_WRITE);
                            } else {
                                selectionKey.interestOps(SelectionKey.OP_READ);
                            }
                        }
                    } catch (IOException e) {
                        Logger.d(TAG, e);
                        closeKeySafely(selectionKey);
                    }
                }
                selector.wakeup();
            }
        });
    }

    private void handleResopnse(final SelectionKey selectionKey) {
        service.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (selectionKey) {
                    try {
                        Logger.dd(TAG, "send response");
                        if (null != selectionKey.attachment()) {
                            String response = null;
                            ByteBuffer buffer = ByteBuffer.wrap((byte[]) selectionKey.attachment());
                            byte[] paramsData = new byte[buffer.getInt() - 4];
                            buffer.get(paramsData);
                            if (buffer.hasRemaining()) {
                                byte[] remainData = new byte[buffer.remaining()];
                                buffer.get(remainData);
                                selectionKey.attach(remainData);
                            } else {
                                selectionKey.attach(null);
                            }
                            JSONObject json = JSON.parseObject(new String(paramsData));
                            Logger.d(TAG, "request : " + json);
                            if (null != json) {
                                int code = json.getInteger("code");
                                if (-1 != code) {
                                    IClient client = clientManager.getClient(json.getString("name"));
                                    if (null != client) {
                                        try {
                                            JSONObject retJson = JSON.parseObject(client.executeCommand(code, json.getString("param")));
                                            Logger.d(TAG, JSON.toJSONString(retJson));
                                            if (null != retJson) {
                                                int type = retJson.getIntValue(RESULT_TYPE);
                                                switch (type) {
                                                    case RESULT_TYPE_STRING:
                                                        response = retJson.getString(RESULT_DATA);
                                                        break;
                                                    case RESULT_TYPE_JSON:
                                                        response = JSON.toJSONString(retJson.get(RESULT_DATA));
                                                        break;
                                                    case RESULT_TYPE_NORMAL_FILE:
                                                        JSONObject fileJson = retJson.getJSONObject(RESULT_DATA);
                                                        Logger.d(TAG, "response : file(size=" + fileJson.getIntValue(RESULT_TYPE_FILE_SIZE) + ",path=" + fileJson.getString(RESULT_TYPE_FILE_PATH) + ")");
                                                        int size = fileJson.getIntValue(RESULT_TYPE_FILE_SIZE);
                                                        File file = new File(fileJson.getString(RESULT_TYPE_FILE_PATH));
                                                        FileInputStream in = new FileInputStream(file);
                                                        SocketChannel clientChannel = (SocketChannel) selectionKey.channel();

                                                        buffer = ByteBuffer.allocate(4);
                                                        buffer.putInt(size + 4);
                                                        buffer.flip();
                                                        flushBuffer(clientChannel,buffer);

                                                        byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
                                                        buffer = ByteBuffer.wrap(buf);
                                                        int len = -1;
                                                        while ((len = in.read(buf)) != -1) {
                                                            buffer.position(0);
                                                            buffer.limit(len);
                                                            flushBuffer(clientChannel,buffer);
                                                        }
                                                        in.close();
                                                        file.delete();

                                                        selectionKey.interestOps(SelectionKey.OP_READ);
                                                        return;
                                                    default:
                                                        response = "illegal response.";
                                                }
                                            }
                                        } catch (RemoteException e) {
                                            clientManager.removeClient(json.getString("name"));
                                            response = "app disconnected.";
                                        }
                                    } else {
                                        response = "app unconnected.";
                                    }
                                } else {
                                    closeKeySafely(selectionKey);
                                    return;
                                }
                            } else {
                                response = "illegal request.";
                            }
                            if (null == response) {
                                response = "null";
                            }
                            Logger.d(TAG, "response : " + response);
                            SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                            byte[] responseBytes = response.getBytes("utf-8");
                            buffer = ByteBuffer.allocate(4);
                            buffer.putInt(responseBytes.length + 4);
                            buffer.flip();
                            flushBuffer(clientChannel,buffer);

                            buffer = ByteBuffer.wrap(responseBytes);
                            flushBuffer(clientChannel,buffer);

                            selectionKey.interestOps(SelectionKey.OP_READ);
                        } else {
                            Logger.e(TAG, "illegal state.");
                            closeKeySafely(selectionKey);
                        }
                    } catch (IOException e) {
                        Logger.d(TAG, e);
                        closeKeySafely(selectionKey);
                    } catch (JSONException e) {
                        try {
                            Logger.e(TAG, e);
                            SocketChannel clientChannel = (SocketChannel) selectionKey.channel();
                            byte[] responseBytes = "json maltformed".getBytes("utf-8");
                            ByteBuffer buffer = ByteBuffer.allocate(4);
                            buffer.putInt(responseBytes.length + 4);
                            buffer.flip();
                            flushBuffer(clientChannel, buffer);
                            buffer = ByteBuffer.wrap(responseBytes);
                            flushBuffer(clientChannel, buffer);
                            selectionKey.interestOps(SelectionKey.OP_READ);
                        } catch (IOException e1) {
                            Logger.d(TAG, e1);
                            closeKeySafely(selectionKey);
                        }
                    }
                }
                selector.wakeup();
            }
        });
    }

    private void flushBuffer(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private void closeKeySafely(SelectionKey selectionKey) {
        try {
            selectionKey.cancel();
            selectionKey.channel().close();
        } catch (IOException e) {
            // no-op
        }
    }
}
