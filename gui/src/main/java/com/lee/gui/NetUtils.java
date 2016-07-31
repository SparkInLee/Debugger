package com.lee.gui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NetUtils {
    private static final boolean _DEBUG = false;
    private static final String TAG = NetUtils.class.getSimpleName();
    private static final int BUFFER_SIZE = 4096;

    private static final String EXIT = "{\"code\":-1}";
    private static final String REQUEST_DUMP_TEMPLATE = "{\"code\":1,\"name\":\"%s\"}";
    private static final String REQUEST_COPY_FILE_TEMPLATE = "{\"code\":2,\"name\":\"%s\",\"param\":\"%s\"}";
    private static final String REQUEST_COPY_DATABASE_TEMPLATE = "{\"code\":3,\"name\":\"%s\",\"param\":\"%s\"}";
    private static final String REQUEST_COPY_SHARED_PREFS_TEMPLATE = "{\"code\":4,\"name\":\"%s\",\"param\":\"%s\"}";
    private static final String REQUEST_INVOKE_METHOD_TEMPLATE = "{\"code\":5,\"name\":\"%s\",\"param\":"
            + "{\"class\":\"%s\",\"method\":\"%s\"}" + "}";

    private final AtomicInteger seq = new AtomicInteger(0);
    private final Executor executor = Executors.newCachedThreadPool(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            // TODO Auto-generated method stub
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("Debug--" + seq.getAndIncrement());
            return thread;
        }
    });

    private String packageName;
    private String ip;
    private int port;
    private String rootDir;

    private final static NetUtils sInstance = new NetUtils();

    public static NetUtils getInstance() {
        return sInstance;
    }

    public void init(String packageName, String ip, int port, String rootDir) {
        this.packageName = packageName;
        this.ip = ip;
        this.port = port;
        this.rootDir = rootDir;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public void copyDatabase(final String dbName, final CallBack<String> callBack) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                SocketChannel clientChannel = null;
                try {
                    clientChannel = SocketChannel.open(new InetSocketAddress(ip, port));
                    File file = new File(rootDir, dbName);
                    sendCommandForFile(clientChannel,
                            String.format(REQUEST_COPY_DATABASE_TEMPLATE, packageName, dbName), file.getAbsolutePath(),
                            callBack);
                    sendCommand(clientChannel, EXIT);
                } catch (IOException e) {
                    if (null != callBack) {
                        callBack.error(e);
                    }
                } finally {
                    if (null != clientChannel) {
                        try {
                            clientChannel.close();
                        } catch (IOException e) {
                            // no-op
                        }
                    }
                }
            }
        });
    }

    public void copySharedPreference(final String spName, final CallBack<String> callBack) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                SocketChannel clientChannel = null;
                try {
                    clientChannel = SocketChannel.open(new InetSocketAddress(ip, port));
                    File file = new File(rootDir, spName);
                    sendCommandForFile(clientChannel,
                            String.format(REQUEST_COPY_SHARED_PREFS_TEMPLATE, packageName, spName),
                            file.getAbsolutePath(), callBack);
                    sendCommand(clientChannel, EXIT);
                } catch (IOException e) {
                    if (null != callBack) {
                        callBack.error(e);
                    }
                } finally {
                    if (null != clientChannel) {
                        try {
                            clientChannel.close();
                        } catch (IOException e) {
                            // no-op
                        }
                    }
                }
            }
        });
    }

    public void copyFile(final String fileName, final CallBack<String> callBack) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                SocketChannel clientChannel = null;
                try {
                    clientChannel = SocketChannel.open(new InetSocketAddress(ip, port));
                    String cmd = String.format(REQUEST_COPY_FILE_TEMPLATE, packageName, fileName);
                    String shortFileName = null;
                    String tempFileName = fileName;
                    tempFileName.replace("\\\\", "/");
                    int fileNameIndex = tempFileName.lastIndexOf("/");
                    while (fileNameIndex == tempFileName.length() - 1) {
                        tempFileName = tempFileName.substring(0, tempFileName.length() - 1);
                        fileNameIndex = tempFileName.lastIndexOf("/");
                    }
                    if (fileNameIndex == -1) {
                        shortFileName = tempFileName;
                    } else {
                        shortFileName = tempFileName.substring(fileNameIndex + 1, tempFileName.length());
                    }
                    File file = new File(rootDir, shortFileName);
                    sendCommandForFile(clientChannel, cmd, file.getAbsolutePath(), callBack);
                    sendCommand(clientChannel, EXIT);
                } catch (IOException e) {
                    if (null != callBack) {
                        callBack.error(e);
                    }
                } finally {
                    if (null != clientChannel) {
                        try {
                            clientChannel.close();
                        } catch (IOException e) {
                            // no-op
                        }
                    }
                }
            }
        });
    }

    public void dumpStackTrace(final CallBack<String> callBack) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                SocketChannel clientChannel = null;
                try {
                    clientChannel = SocketChannel.open(new InetSocketAddress(ip, port));
                    sendCommandForStr(clientChannel, String.format(REQUEST_DUMP_TEMPLATE, packageName), callBack);
                    sendCommand(clientChannel, EXIT);
                } catch (IOException e) {
                    if (null != callBack) {
                        callBack.error(e);
                    }
                } finally {
                    if (null != clientChannel) {
                        try {
                            clientChannel.close();
                        } catch (IOException e) {
                            // no-op
                        }
                    }
                }
            }
        });
    }

    public void saveDumpStackTrace(final CallBack<String> callBack) {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                SocketChannel clientChannel = null;
                try {
                    clientChannel = SocketChannel.open(new InetSocketAddress(ip, port));
                    sendCommandForStr(clientChannel, String.format(REQUEST_DUMP_TEMPLATE, packageName),
                            new CallBack<String>() {

                                @Override
                                public void response(String t) {
                                    // TODO Auto-generated method stub
                                    try {
                                        File file = new File(rootDir, "st.txt");
                                        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                                        writer.write(format(t));
                                        writer.flush();
                                        writer.close();
                                        if (null != callBack) {
                                            callBack.response(file.getName());
                                        }
                                    } catch (Exception e) {
                                        // TODO Auto-generated catch block
                                        if (null != callBack) {
                                            callBack.error(e);
                                        }
                                    }
                                }

                                @Override
                                public void error(Exception e) {
                                    // TODO Auto-generated method stub
                                    if (null != callBack) {
                                        callBack.error(e);
                                    }
                                }
                            });
                    sendCommand(clientChannel, EXIT);
                } catch (IOException e) {
                    if (null != callBack) {
                        callBack.error(e);
                    }
                } finally {
                    if (null != clientChannel) {
                        try {
                            clientChannel.close();
                        } catch (IOException e) {
                            // no-op
                        }
                    }
                }
            }
        });
    }

    public static void sendCommand(SocketChannel clientChannel, String command) throws IOException {
        byte[] commandBytes = command.getBytes("utf-8");
        log(TAG, "request : " + command);
        ByteBuffer buffer = ByteBuffer.allocate(commandBytes.length + 4);
        buffer.putInt(commandBytes.length + 4);
        buffer.put(commandBytes);
        buffer.flip();
        clientChannel.write(buffer);
        log(TAG, "response : ok");
    }

    public static void sendCommandForFile(SocketChannel clientChannel, String command, String fileName,
                                          CallBack<String> callBack) throws IOException {
        byte[] commandBytes = command.getBytes("utf-8");
        log(TAG, "request : " + command);
        ByteBuffer buffer = ByteBuffer.allocate(commandBytes.length + 4);
        buffer.putInt(commandBytes.length + 4);
        buffer.put(commandBytes);
        buffer.flip();
        clientChannel.write(buffer);
        FileOutputStream out = new FileOutputStream(fileName);
        buffer = ByteBuffer.allocate(4);
        while (buffer.hasRemaining())
            clientChannel.read(buffer);
        buffer.flip();
        if (buffer.remaining() == 4) {
            int read = 0;
            int size = buffer.getInt() - 4;
            buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while (read < size) {
                while (buffer.hasRemaining() && read < size) {
                    read += clientChannel.read(buffer);
                }
                if (buffer.position() < BUFFER_SIZE) {
                    byte[] outBytes = new byte[buffer.position()];
                    buffer.flip();
                    buffer.get(outBytes);
                    out.write(outBytes);
                } else {
                    out.write(buffer.array());
                }
                buffer.clear();
            }
            out.close();
            log(TAG, "respose : File = " + fileName + ", Size = " + size);
            if (null != callBack) {
                callBack.response(fileName);
            }
        } else {
            log(TAG, "read data error");
            callBack.error(new IllegalStateException("read data error"));
        }
    }

    public static void sendCommandForStr(SocketChannel clientChannel, String command, CallBack<String> callBack)
            throws IOException {
        byte[] commandBytes = command.getBytes("utf-8");
        log(TAG, "request : " + command);
        ByteBuffer buffer = ByteBuffer.allocate(commandBytes.length + 4);
        buffer.putInt(commandBytes.length + 4);
        buffer.put(commandBytes);
        buffer.flip();
        clientChannel.write(buffer);
        buffer = ByteBuffer.allocate(4);
        while (buffer.hasRemaining())
            clientChannel.read(buffer);
        buffer.flip();
        if (buffer.remaining() == 4) {
            int len = -1;
            buffer = ByteBuffer.allocate(buffer.getInt() - 4);
            while (buffer.hasRemaining())
                clientChannel.read(buffer);
            if (null != callBack) {
                callBack.response(new String(buffer.array()));
            }
            log(TAG, "response : ok");
        } else {
            log(TAG, "read data error");
            callBack.error(new IllegalStateException("read data error"));
        }
    }

    private String format(String jsonStr) {
        int level = 0;
        StringBuffer jsonForMatStr = new StringBuffer();
        for (int i = 0; i < jsonStr.length(); i++) {
            char c = jsonStr.charAt(i);
            if (level > 0 && '\n' == jsonForMatStr.charAt(jsonForMatStr.length() - 1)) {
                jsonForMatStr.append(getLevelStr(level));
            }
            switch (c) {
                case '{':
                case '[':
                    jsonForMatStr.append(c + "\n");
                    level++;
                    break;
                case ',':
                    jsonForMatStr.append(c + "\n");
                    break;
                case '}':
                case ']':
                    jsonForMatStr.append("\n");
                    level--;
                    jsonForMatStr.append(getLevelStr(level));
                    jsonForMatStr.append(c);
                    break;
                default:
                    jsonForMatStr.append(c);
                    break;
            }
        }

        return jsonForMatStr.toString();

    }

    private String getLevelStr(int level) {
        StringBuffer levelStr = new StringBuffer();
        for (int levelI = 0; levelI < level; levelI++) {
            levelStr.append("\t");
        }
        return levelStr.toString();
    }

    private static void log(String TAG, String msg) {
        if (_DEBUG) {
            System.out.println(TAG + "\t:\t" + msg);
        }
    }
}
