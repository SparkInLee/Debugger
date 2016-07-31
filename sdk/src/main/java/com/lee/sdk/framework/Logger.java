package com.lee.sdk.framework;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author jiangli
 */
public class Logger {

    private static final int DEBUG = 0;
    private static final int INFO = 1;
    private static final int ERROR = 2;
    private static final int level = DEBUG;

    private static final String divideLine = " ---------------------------- ";

    public static void d(String TAG, String msg) {
        if (level <= DEBUG) {
            Log.d(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + msg);
        }
    }

    public static void d(String TAG, Exception e) {
        if (level <= DEBUG) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            Log.e(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + writer.toString());
            try {
                writer.close();
            } catch (IOException e1) {
                // no-op
            }
        }
    }

    public static void i(String TAG, String msg) {
        if (level <= INFO) {
            Log.i(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + msg);
        }
    }

    public static void i(String TAG, Exception e) {
        if (level <= INFO) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            Log.i(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + writer.toString());
            try {
                writer.close();
            } catch (IOException e1) {
                // no-op
            }
        }
    }

    public static void e(String TAG, String msg) {
        if (level <= ERROR) {
            Log.e(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + msg);
        }
    }

    public static void e(String TAG, Exception e) {
        if (level <= ERROR) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            Log.e(TAG, Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")\t" + writer.toString());
            try {
                writer.close();
            } catch (IOException e1) {
                // no-op
            }
        }
    }

    public static void dd(String TAG, String msg) {
        d(TAG, divideMessage(msg));
    }

    public static void di(String TAG, String msg) {
        i(TAG, divideMessage(msg));
    }

    public static void de(String TAG, String msg) {
        e(TAG, divideMessage(msg));
    }

    private static String divideMessage(String msg) {
        return divideLine + msg + divideLine;
    }
}
