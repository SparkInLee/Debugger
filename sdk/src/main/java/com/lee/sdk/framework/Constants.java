package com.lee.sdk.framework;

/**
 * @author jiangli
 */
public class Constants {

    public static final int DEFAULT_PORT = 8990;

    public static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * It's better to make sure that Custom Command ID is greater than 1000.
     */
    public static final int COMMAND_UNSUPPORTED = -1;
    public static final int COMMAND_DUMP = 1;
    public static final int COMMAND_COPY_FILE = 2;
    public static final int COMMAND_COPY_DATABASE = 3;
    public static final int COMMAND_COPY_SHARED_PREFERENCE = 4;
    public static final int COMMAND_INVOKE_STATIC_OR_SINGLETON = 5;

    public static final String RESULT_TYPE = "type";
    public static final String RESULT_DATA = "data";

    public static final int RESULT_TYPE_STRING = 0x0;
    public static final int RESULT_TYPE_JSON = 0x1;
    public static final int RESULT_TYPE_NORMAL_FILE = 0x2;

    public static final String RESULT_TYPE_FILE_PATH = "path";
    public static final String RESULT_TYPE_FILE_SIZE = "size";
}
