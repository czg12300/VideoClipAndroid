package com.jake.ffmpegandroid.common;

import android.content.Context;
import android.os.Bundle;

/**
 * 页面跳转工具
 *
 * @author jake
 * @since 2017/4/7 下午3:45
 */

public class JumpUtils {
    public static void startFragment(Context context, Class<?> clazz, Bundle bundle) {
        startFragment(context, clazz, bundle, true);
    }

    public static void startFragment(Context context, Class<?> clazz) {
        startFragment(context, clazz, null);
    }

    public static void startFragment(Context context, Class<?> clazz, Bundle bundle, boolean needAnim) {
        if (clazz != null) {
            ContainerActivity.start(context, clazz.getName(), bundle, needAnim);
        }
    }

    public static void startFragmentForResult(Context context, Class<?> clazz, Bundle bundle, int requestCode) {
        startFragmentForResult(context, clazz, bundle, true, requestCode);
    }

    public static void startFragmentForResult(Context context, Class<?> clazz, Bundle bundle, boolean needAnim, int requestCode) {
        if (clazz != null) {
            ContainerActivity.startForResult(context, clazz.getName(), bundle, needAnim, requestCode);
        }
    }
}
