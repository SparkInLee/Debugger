package com.lee.sdk.framework;

import static com.lee.sdk.framework.Constants.*;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lee.sdk.service.IClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base Client which provides Base Commands:
 * code : -1 ---->  {@link Client#handleUnsupported()}
 * code : 1  ---->  {@link Client#dump()}
 * code : 2  ---->  {@link Client#copyFile(String)}
 * code : 3  ---->  {@link Client#copyDataBase(String)}
 * code : 4  ---->  {@link Client#copySharedPreference(String)}
 * code : 5  ---->  {@link Client#invokeStaticOrSingleTonMethod(String)}
 *
 * Developer can define their own command by creating a Class which extends from {@link Client},
 * and then write the method with {@link Command} Annotation.
 * Example:
 * <code>
 *     public CustomClient extends Client{
 *         ...
 *         @Command(code = 1001)
 *         public String testCommand(){
 *             return xxx;
 *         }
 *         ...
 *     }
 * </code>
 * The ReturnType of Command Method should be String, so that Client can send it to {@link Server},
 * and the return String should obey the {@link Server} protocol.
 *
 * @see Server
 * @see com.lee.sdk.service.DebuggerService
 *
 * @author jiangli
 */
public class Client extends IClient.Stub {
    private static final String TAG = "Client";

    protected Map<Integer, Method> commandMap = new HashMap<Integer, Method>();

    protected Context context;

    public Client(Context context) {
        this.context = context;
        registerCommand();
    }

    @Override
    public String executeCommand(int code, String param) throws RemoteException {
        Exception e = null;
        try {
            if (!commandMap.containsKey(code)) {
                code = -1;
            }
            Method method = commandMap.get(code);
            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 0) {
                return (String) commandMap.get(code).invoke(this);
            } else if (paramTypes.length == 1 && paramTypes[0].equals(String.class)) {
                return (String) commandMap.get(code).invoke(this, param);
            }
        } catch (Exception e1) {
            e = e1;
        }
        return generateResponse(e);
    }

    @Override
    public ParcelFileDescriptor getFileDescriptor() throws RemoteException {
        return null;
    }

    @Command(code = COMMAND_DUMP)
    private String dump() {
        try {
            JSONObject json = new JSONObject();

            JSONArray dataArr = new JSONArray();
            Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
            if (null != stacks && stacks.size() > 0) {
                for (Map.Entry<Thread, StackTraceElement[]> stack : stacks.entrySet()) {
                    JSONObject stackJson = new JSONObject();
                    stackJson.put("Thread", stack.getKey().toString());
                    stackJson.put("State", stack.getKey().getState());
                    if (null != stack.getValue()) {
                        JSONArray stackArr = new JSONArray();
                        for (StackTraceElement stackTraceElement : stack.getValue()) {
                            stackArr.add(stackTraceElement.toString());
                        }
                        stackJson.put("StackTrace", stackArr);
                    }
                    dataArr.add(stackJson);
                }
            }

            json.put(RESULT_TYPE, RESULT_TYPE_NORMAL_FILE);
            try {
                File file = context.getExternalCacheDir();
                file = new File(file.getAbsolutePath(), UUID.randomUUID().toString());
                FileWriter writer = new FileWriter(file);
                writer.write("[");
                int len = dataArr.size();
                for (int i = 0; i < len; i++) {
                    writer.write(dataArr.getJSONObject(i).toJSONString());
                    if (i < len - 1) {
                        writer.write(",");
                    }
                }
                writer.write("]");
                writer.flush();
                writer.close();
                JSONObject fileJson = new JSONObject();
                fileJson.put(RESULT_TYPE_FILE_PATH, file.getAbsolutePath());
                fileJson.put(RESULT_TYPE_FILE_SIZE, file.length());
                json.put(RESULT_DATA, fileJson);
                return JSON.toJSONString(json);
            } catch (Exception e) {
                // no-op
            }

            json.put(RESULT_TYPE, RESULT_TYPE_JSON);
            json.put(RESULT_DATA, dataArr);

            return JSON.toJSONString(json);
        } catch (Exception e) {
            return generateResponse(e);
        }
    }

    @Command(code = COMMAND_COPY_FILE)
    protected String copyFile(String fileName) {
        try {
            JSONObject json = new JSONObject();
            json.put(RESULT_TYPE, RESULT_TYPE_NORMAL_FILE);
            File originFile = new File(fileName);
            if (!originFile.exists()) {
                return generateResponse("file(" + fileName + ") not exists.");
            }
            FileInputStream in = new FileInputStream(fileName);
            File file = context.getExternalCacheDir();
            file = new File(file.getAbsolutePath(), UUID.randomUUID().toString());
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
            int len = -1;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            JSONObject fileJson = new JSONObject();
            fileJson.put(RESULT_TYPE_FILE_PATH, file.getAbsolutePath());
            fileJson.put(RESULT_TYPE_FILE_SIZE, file.length());
            json.put(RESULT_DATA, fileJson);
            return json.toJSONString();
        } catch (Exception e) {
            return generateResponse(e);
        }
    }

    @Command(code = COMMAND_COPY_DATABASE)
    private String copyDataBase(String dbName) {
        String databaseTemplate = "/data/data/%s/databases/%s";
        if (null == dbName) {
            return generateResponse("param illegal : no database name.");
        }
        return copyFile(String.format(databaseTemplate, context.getPackageName(), dbName));
    }

    @Command(code = COMMAND_COPY_SHARED_PREFERENCE)
    private String copySharedPreference(String sharedPrefsName) {
        String databaseTemplate = "/data/data/%s/shared_prefs/%s";
        if (null == sharedPrefsName) {
            return generateResponse("param illegal : no shared preferences name.");
        }
        return copyFile(String.format(databaseTemplate, context.getPackageName(), sharedPrefsName));
    }

    /**
     * The Param Protocol (Based on JSON):
     * <code>
     *     {
     *         "class":"class signature",
     *         "method":"method name",
     *         "param":[{
     *             "type":"Class Type",
     *             "value":xxx
     *         }]
     *     }
     * </code>
     * The class should be fully qualified, such as:
     *      {@link Client} should be "om.lee.sdk.framework.Client"
     * The method is the Method Name String;
     * The param is optional, which is used to resolve param type and value of the invoked method, and
     * the sequence should be same to the {@link Method#getParameterTypes()}.
     *
     * @param paramStr
     * @return
     */
    @Command(code = COMMAND_INVOKE_STATIC_OR_SINGLETON)
    private String invokeStaticOrSingleTonMethod(String paramStr) {
        try {
            JSONObject paramJson = JSON.parseObject(paramStr);
            Class klass = context.getClassLoader().loadClass(paramJson.getString("class"));
            String methodStr = paramJson.getString("method");
            Class<?>[] paramTypes = null;
            Object[] paramValues = null;
            if (paramJson.containsKey("param")) {
                TwoTuple<Class, Object>[] params = parseParam(paramJson.getJSONArray("param"));
                int paramLen = params.length;
                paramTypes = new Class[paramLen];
                paramValues = new Object[paramLen];
                for (int i = 0; i < paramLen; ++i) {
                    paramTypes[i] = params[i].getFirst();
                    paramValues[i] = params[i].getSecond();
                }
            }
            Method method = findMethodAndSetAccessible(klass, methodStr, paramTypes);
            if (null != method) {
                if ((method.getModifiers() & Modifier.STATIC) != 0) {
                    return generateResponse(invokeMethod(method, null, paramValues));
                } else {
                    Method getInstance = findMethodAndSetAccessible(klass, "getInstance", null);
                    if (null != getInstance
                            && (getInstance.getModifiers() & Modifier.STATIC) != 0
                            && getInstance.getReturnType().equals(klass)) {
                        return generateResponse(invokeMethod(method, klass.cast(getInstance.invoke(null)), paramValues));
                    } else {
                        getInstance = findMethodAndSetAccessible(klass, "newInstance", null);
                        if ((getInstance.getModifiers() & Modifier.STATIC) != 0
                                && getInstance.getReturnType().equals(klass)) {
                            return generateResponse(invokeMethod(method, klass.cast(getInstance.invoke(null)), paramValues));
                        }
                    }
                }
            }
            return generateResponse("unsupported method invocation");
        } catch (Exception e) {
            return generateResponse(e);
        }
    }

    /**
     * If the command is unsupported, the it will redirect here.
     *
     * @return
     */
    @Command(code = COMMAND_UNSUPPORTED)
    private String handleUnsupported() {
        JSONObject json = new JSONObject();
        json.put(RESULT_TYPE, RESULT_TYPE_STRING);
        json.put(RESULT_DATA, "unsupported command.");
        return JSON.toJSONString(json);
    }

    protected String generateResponse(String response) {
        JSONObject json = new JSONObject();
        json.put(RESULT_TYPE, RESULT_TYPE_STRING);
        json.put(RESULT_DATA, null != response ? response : "unknown error.");
        return JSON.toJSONString(json);
    }

    protected String generateResponse(Exception e) {
        JSONObject json = new JSONObject();
        json.put(RESULT_TYPE, RESULT_TYPE_STRING);
        json.put(RESULT_DATA, null != e ? e.getClass().getName() + " :\n\t\t" + e.getMessage() : "unknown error.");
        return JSON.toJSONString(json);
    }

    private TwoTuple<Class, Object>[] parseParam(JSONArray paramArr) {
        int len = paramArr.size();
        TwoTuple<Class, Object>[] params = new TwoTuple[len];
        for (int i = 0; i < len; ++i) {
            JSONObject paramJSON = paramArr.getJSONObject(i);
            String type = paramJSON.getString("type");
            TwoTuple<Class, Object> param = null;
            if (type.length() == 1) {
                switch (type.charAt(0)) {
                    case 'Z':
                        param = new TwoTuple<Class, Object>(Boolean.class, paramJSON.getBoolean("value"));
                        break;
                    case 'I':
                        param = new TwoTuple<Class, Object>(Integer.class, paramJSON.getInteger("value"));
                        break;
                    case 'F':
                        param = new TwoTuple<Class, Object>(Float.class, paramJSON.getFloat("value"));
                        break;
                    case 'J':
                        param = new TwoTuple<Class, Object>(Long.class, paramJSON.getLong("value"));
                        break;
                    case 'D':
                        param = new TwoTuple<Class, Object>(Double.class, paramJSON.getDouble("value"));
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported Type");
                }
            } else {
                String[] spilt = type.split("[\\./]");
                if (spilt[spilt.length - 1].equals("String")) {
                    param = new TwoTuple<Class, Object>(String.class, paramJSON.getString("value"));
                } else {
                    throw new IllegalArgumentException("Unsupported Type");
                }
            }
            params[i] = param;
        }
        return params;
    }

    private Method findMethodAndSetAccessible(Class findClass, String findMethod, Class<?>[] findParams) {
        while (findClass != null) {
            Method[] methods = findClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(findMethod)) {
                    boolean isFind = true;
                    Class<?>[] params = method.getParameterTypes();
                    if (null == findParams && params.length == 0) {
                        // find the method
                    } else if (params.length == findParams.length) {
                        int len = params.length;
                        for (int i = 0; i < len; ++i) {
                            if (!isTypeEqual(params[i], findParams[i])) {
                                isFind = false;
                                break;
                            }
                        }
                    }
                    if (isFind) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            findClass = findClass.getSuperclass();
        }
        return null;
    }

    private String invokeMethod(Method invokeMethod, Object invokeObject, Object[] params) throws InvocationTargetException, IllegalAccessException {
        if (null == invokeMethod) {
            throw new IllegalArgumentException("invoke method can not be null.");
        }
        if (null != params && params.length > 0) {
            return String.valueOf(invokeMethod.invoke(invokeObject, params));
        } else {
            return String.valueOf(invokeMethod.invoke(invokeObject));
        }
    }

    private static final Map<Class, Class> PRIMATE_TYPE_TRANSFORM = new HashMap<Class, Class>() {
        {
            put(boolean.class, Boolean.class);
            put(byte.class, Byte.class);
            put(char.class, Character.class);
            put(short.class, Short.class);
            put(int.class, Integer.class);
            put(long.class, Long.class);
            put(float.class, Float.class);
            put(double.class, Double.class);
        }
    };

    private boolean isTypeEqual(Class<?> c1, Class<?> c2) {
        if (PRIMATE_TYPE_TRANSFORM.containsKey(c1)) {
            c1 = PRIMATE_TYPE_TRANSFORM.get(c1);
        }
        if (PRIMATE_TYPE_TRANSFORM.containsKey(c2)) {
            c2 = PRIMATE_TYPE_TRANSFORM.get(c2);
        }
        return c1.equals(c2);
    }

    private void registerCommand() {
        Class klass = this.getClass();
        List<Method> methods = new ArrayList<Method>();
        while (null != klass && !klass.equals(Object.class)) {
            methods.addAll(Arrays.asList(klass.getDeclaredMethods()));
            klass = klass.getSuperclass();
        }
        List<Method> commands = new ArrayList<Method>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Command.class) && method.getReturnType().equals(String.class)) {
                method.setAccessible(true);
                commands.add(method);
            }
        }
        for (Method method : commands) {
            Command command = method.getAnnotation(Command.class);
            if (!commandMap.containsKey(command.code())) {
                commandMap.put(command.code(), method);
            } else {
                Logger.i(TAG, "Failed to register command(" + command.code() + "," + method.getName() + ") : Already Registered to Method(" + this.commandMap.get(command.code()).getName() + ")");
            }
        }
    }
}
