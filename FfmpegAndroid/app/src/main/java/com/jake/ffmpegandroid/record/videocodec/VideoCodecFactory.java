package com.jake.ffmpegandroid.record.videocodec;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.RequiresApi;

/**
 * 创建视频编码的工厂
 *
 * @author jake
 * @since 2017/5/2 下午6:01
 */

public final class VideoCodecFactory {
    private VideoCodecFactory() {
    }

    static boolean useMediaCodec = true;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static final VideoCodec getVideoCodec() {
        VideoCodec videoCodec = null;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || !useMediaCodec) {
            videoCodec = new FfmpegVideoCodec();
        } else {
            videoCodec = new MediaVideoCodec();
        }
        return videoCodec;
    }
}
