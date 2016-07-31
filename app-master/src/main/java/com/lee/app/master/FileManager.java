package com.lee.app.master;

import android.os.Environment;

import com.lee.sdk.framework.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by jianglee on 7/31/16.
 */
public final class FileManager {
    private static final String TAG = "FileManager";

    private static final String HOME = "L-Debugger/master";

    private static final String CONFIG = "config.properties";
    private static final String CONFIG_DEBUG = "debug";

    private File homeDir;

    private final Properties sConfig = new Properties();

    private static FileManager sInstance = null;

    public static FileManager getInstance() {
        if (null == sInstance) {
            synchronized (FileManager.class) {
                if (null == sInstance) {
                    sInstance = new FileManager();
                }
            }
        }

        return sInstance;
    }

    private FileManager() {
        ensureExist();
        readConfig();
    }

    private void ensureExist() {
        homeDir = new File(Environment.getExternalStorageDirectory(), HOME);
        if (!homeDir.exists()) {
            if (!homeDir.mkdirs()) {
                throw new RuntimeException("Can not create home directory.");
            }
        }
    }

    private void readConfig() {
        File configFile = new File(homeDir, CONFIG);
        if (configFile.exists()) {
            try {
                sConfig.load(new FileInputStream(configFile));
            } catch (IOException e) {
                Logger.e(TAG, e);
            }
        }
    }

    public boolean isDebug() {
        return Boolean.parseBoolean(sConfig.getProperty(CONFIG_DEBUG, "false"));
    }
}
