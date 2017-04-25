package com.jake.ffmpegandroid.common;

import android.util.Log;

/**
 * 日志工具
 * 
 * @author chenys
 */
public class LogUtil {

    private static final String TAG = "xiuge_log";

    private static boolean isDebug = true;

    /**
     * 是否处于调试模式
     *
     */
    public static boolean isDebug() {
        return isDebug;
    }

    /**
     * 设置是否调试模式
     * 
     * @param debug
     */
    public static void setDebug(boolean debug) {
        isDebug = debug;
    }

    public static void d(String msg) {
        if (isDebug && msg != null) {
            Log.d(TAG, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (isDebug && msg != null) {
            Log.d(tag, msg);
        }
    }

    public static void e(String msg) {
        if (isDebug && msg != null) {
            Log.e(TAG, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (isDebug && msg != null) {
            Log.e(tag, msg);
        }
    }

}
