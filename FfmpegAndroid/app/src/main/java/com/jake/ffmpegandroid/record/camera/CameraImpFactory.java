package com.jake.ffmpegandroid.record.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * 用于创建摄像头实现的实例
 *
 * @author jake
 * @since 2017/4/27 上午11:33
 */

public final class CameraImpFactory {
    static boolean useCamera2 = false;

    private CameraImpFactory() {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static final CameraImp getCameraImp(Context context) {
        CameraImp cameraImp = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || !useCamera2) {
            cameraImp = new Camera1(context);
        } else {
            cameraImp = new Camera2(context);
        }
        return cameraImp;
    }
}
