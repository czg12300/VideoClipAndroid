package com.jake.ffmpegandroid.record.videocodec;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * 视频编码器的实现
 *
 * @author jake
 * @since 2017/5/2 下午5:25
 */

public interface VideoCodec {
    void setExecutor(Executor executor);

    void start(VideoCodecParameters parameters);

    void encode(byte[] data, int width, int height);

    void stop();

    void setCallback(Callback callback);

    interface Callback {
        void onReceive(byte[] data);

        void onFinish();
    }
}
