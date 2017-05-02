package com.jake.ffmpegandroid.cameraimp;

import android.content.Context;
import android.os.Build;
import android.system.Os;

/**
 * 用于创建摄像头实现的实例
 *
 * @author jake
 * @since 2017/4/27 上午11:33
 */

public final class CameraImpFactory {
    private CameraImpFactory() {
    }

    public static final CameraImp getCameraImp(Context context) {
        CameraImp cameraImp = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            cameraImp = new Camera1(context);
        } else {
            cameraImp = new Camera2(context);
        }
        return cameraImp;
    }
}
