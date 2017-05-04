package com.jake.ffmpegandroid.record.videocodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.jake.ffmpegandroid.common.VLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * 使用MediaCodec编码
 *
 * @author jake
 * @since 2017/5/2 下午5:25
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
public class MediaVideoCodec implements VideoCodec {
    private MediaCodec mMediaCodec;
    private Callback mCallback;
    private Executor mExecutor;
    private volatile boolean isFlush = false;
private int frameIndex=0;
    @Override
    public void setExecutor(Executor executor) {
        mExecutor = executor;
    }

    @Override
    public void start(VideoCodecParameters parameters) {
        if (parameters == null) {
            return;
        }
//        stop();
        VLog.d("MediaVideoCodec start");
        frameIndex=1;
        try {
            mMediaCodec = mMediaCodec.createEncoderByType(transCodecType(parameters.codecType));
            mMediaCodec.configure(getMediaFormatByParameters(parameters), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            VLog.d("MediaVideoCodec  mMediaCodec.start()");
            if (mExecutor != null) {
                VLog.d("MediaVideoCodec  mMediaCodec.start()");
                mExecutor.execute(outTask);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MediaFormat getMediaFormatByParameters(VideoCodecParameters parameters) {
        MediaFormat format = MediaFormat.createVideoFormat(transCodecType(parameters.codecType), parameters.width, parameters.height);
        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        format.setInteger(MediaFormat.KEY_BIT_RATE, parameters.bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, parameters.frameRate);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, parameters.frameRate);
        if (parameters.keyIFrameInterval > 0) {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, parameters.keyIFrameInterval);
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        }
        return format;
    }

    private String transCodecType(VideoCodecParameters.CodecType codecType) {
        String result = MediaFormat.MIMETYPE_VIDEO_AVC;
        switch (codecType) {
            case H264:
                result = MediaFormat.MIMETYPE_VIDEO_AVC;
                break;
            case H265:
                result = MediaFormat.MIMETYPE_VIDEO_HEVC;
                break;
        }
        return result;
    }

    @Override
    public void encode(byte[] data, int width, int height) {
        if (isFlush || mMediaCodec == null || data == null || data.length == 0) {
            return;
        }
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);

        VLog.d("MediaVideoCodec  encode(byte[] data, int width, int height) inputBufferIndex="+inputBufferIndex);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(data);
//            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length,  0,0);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length,  System.nanoTime() / 1000, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
        }
        if (mExecutor == null) {
            outTask.run();
        }


    }

    private Runnable outTask = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true) {
                if (isFlush) {
                    mMediaCodec.stop();
                    mMediaCodec.release();
                    mMediaCodec = null;
                    VLog.d("MediaVideoCodec  isFlush");
                    if (mCallback != null) {
                        mCallback.onFinish();
                    }
                    isFlush = false;
                    break;//结束线程
                }
                VLog.d("MediaVideoCodec  outTask");
                ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);
                    if (mCallback != null) {
                        mCallback.onReceive(outData);
                    }
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    public void stop() {
            isFlush = true;
    }

    @Override
    public void setCallback(Callback callback) {
        mCallback = callback;
    }
}
