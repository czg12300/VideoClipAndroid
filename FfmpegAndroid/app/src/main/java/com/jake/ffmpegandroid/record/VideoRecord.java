package com.jake.ffmpegandroid.record;

import android.text.TextUtils;

import com.jake.ffmpegandroid.common.VLog;
import com.jake.ffmpegandroid.record.videocodec.VideoCodec;
import com.jake.ffmpegandroid.record.videocodec.VideoCodecFactory;
import com.jake.ffmpegandroid.record.videocodec.VideoCodecParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 视频录制
 *
 * @author jake
 * @since 2017/5/2 下午6:07
 */

public class VideoRecord {
    private static final int DEFAULT_FRAME_RATE = 25;
    private Queue<Frame> mInputFrameQueue;
    private Queue<Queue<Frame>> mRawFrameQueue;
    private Queue<Frame> mHandledFrameQueue;
    private VideoCodec mVideoCodec;
    private boolean isAcceptFrame = false;
    private long lastFrameTimestamp;
    private ExecutorService mExecutorService;
    private boolean isStop = false;
    private volatile int frameRate = DEFAULT_FRAME_RATE;
    private volatile int positionFrameRate = DEFAULT_FRAME_RATE;
    private VideoCodecParameters mVideoCodecParameters;
    private String mFilePath;

    public VideoRecord() {
        //TODO
    }

    public void setVideoCodecParameters(VideoCodecParameters parameters) {
        if (parameters == null) {
            throw new NullPointerException("parameters can't be null");
        }
        this.mVideoCodecParameters = parameters;
        this.frameRate = parameters.frameRate;
        this.positionFrameRate = parameters.frameRate;
    }

    public void setPositionFrameRate(int positionFrameRate) {
        if (positionFrameRate > 0) {
            this.positionFrameRate = positionFrameRate;
        }
    }

    public void start(String filePath) {
        if (mVideoCodecParameters == null) {
            throw new NullPointerException("VideoCodecParameters is null,please setup VideoCodecParameters");
        }
        if (TextUtils.isEmpty(filePath)) {
            isAcceptFrame = false;
            return;
        }
        mFilePath = filePath;
        release();
        isStop = false;
        isAcceptFrame=true;
        mExecutorService = new ThreadPoolExecutor(5, 10, 5 * 1000, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(5));
        mRawFrameQueue = new LinkedBlockingQueue();
        mVideoCodec = VideoCodecFactory.getVideoCodec();
        mVideoCodec.setCallback(codecCallback);
        mVideoCodec.setExecutor(mExecutorService);
        mVideoCodec.start(mVideoCodecParameters);
        mExecutorService.execute(handleRawFrameTask);
        mExecutorService.execute(codecTask);
    }

    public void resume() {
        isAcceptFrame = true;
    }

    public void pause() {
        isAcceptFrame = false;
    }

    public void stop() {
        isStop = true;
        flushRawFrame();
    }

    private void release() {
        lastFrameTimestamp = -1;
        if (mRawFrameQueue != null) {
            mRawFrameQueue.clear();
            mRawFrameQueue = null;
        }
        if (mExecutorService != null) {
            mExecutorService.shutdownNow();
            mExecutorService = null;
        }
        if (mVideoCodec != null) {
            mVideoCodec.stop();
            mVideoCodec = null;
        }
    }

    private void flushRawFrame() {
        if (mInputFrameQueue != null) {
            mRawFrameQueue.add(mInputFrameQueue);
            mInputFrameQueue = null;
        }

    }

    public void record(byte[] data, int width, int height, long timestamp) {
        if (!isAcceptFrame) {
            return;
        }
        if (lastFrameTimestamp == -1 || (timestamp - lastFrameTimestamp >= 1000)) {
            if (mInputFrameQueue != null) {
                if (timestamp - lastFrameTimestamp >= 1000) {
                    mRawFrameQueue.offer(mInputFrameQueue);
                }
                mInputFrameQueue = null;
            }
            mInputFrameQueue = new LinkedBlockingQueue<>();
            lastFrameTimestamp = timestamp;
        } else {
            mInputFrameQueue.add(Frame.create(data, width, height, timestamp));
        }

    }


    private VideoCodec.Callback codecCallback = new VideoCodec.Callback() {
        FileOutputStream outputStream;
        private String lastFilePath;

        @Override
        public void onReceive(byte[] data) {
            VLog.d("VideoRecord codecCallback onReceive len"+data.length);
            try {
                createOutFile();
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onFinish() {
            clearOutputStream();
        }

        private void clearOutputStream() {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputStream = null;
            }
        }

        private void createOutFile() throws IOException {
            if (!TextUtils.equals(mFilePath, lastFilePath)) {
                File file = new File(mFilePath);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                if (outputStream != null) {
                    clearOutputStream();
                }
                outputStream = new FileOutputStream(file);
                lastFilePath = mFilePath;
            }
        }
    };
    private Runnable handleRawFrameTask = new Runnable() {
        private boolean isNeedThisFrame(int index, int targetTotal, int total) {
            if (targetTotal != total) {
                int remove = total - (total - targetTotal);
                if (targetTotal > total) {
                    remove = targetTotal % total;
                }
                if (remove <= 0) {
                    return true;
                }
                double removeSpitD = (double) total / (double) remove;
                boolean needReverse = false;
                if (removeSpitD < 2) {
                    removeSpitD = (double) total / (double) (total - remove);
                    needReverse = true;
                }
                int removeSpit = (int) (removeSpitD + 0.5);
                if ((index % removeSpit) == 0) {
                    return needReverse;
                }
                return !needReverse;
            } else {
                return true;
            }
        }


        @Override
        public void run() {
            sleepThread(1000);
            while (true) {
                if (mRawFrameQueue != null) {
                    VLog.d("VideoRecord handleRawFrameTask mRawFrameQueue len"+mRawFrameQueue.size());
                    if (mRawFrameQueue.isEmpty()) {
                        if (isStop) {
                            break;
                        } else {
                            sleepThread(1000);
                            continue;
                        }
                    }
                    Queue<Frame> frameQueue = mRawFrameQueue.poll();
                    if (frameQueue != null) {
                        if (mHandledFrameQueue == null) {
                            mHandledFrameQueue = new LinkedBlockingQueue();
                        }
                        int len = frameQueue.size();
                        if (len > 0) {
                            if (positionFrameRate >= frameRate) {//正常速率和视频加速
                                int index = 0;
                                while (!frameQueue.isEmpty()) {
                                    if (isNeedThisFrame(index, positionFrameRate, len)) {
                                        Frame frame = frameQueue.poll();
                                        if (frame != null) {
                                            mHandledFrameQueue.offer(frame);
                                        }
                                    }
                                    index++;
                                }
                            } else {//减速
                                int index = 0;
                                while (!frameQueue.isEmpty()) {
                                    Frame frame = frameQueue.poll();
                                    if (frame == null) {
                                        continue;
                                    }
                                    if (isNeedThisFrame(index, positionFrameRate, len)) {
                                        frame.writeTimes += 1;
                                    }
                                    if (positionFrameRate > len) {
                                        frame.writeTimes += positionFrameRate / len;
                                    }
                                    mHandledFrameQueue.offer(frame);
                                    index++;
                                }
                            }
                        }
                    }
                } else {
                    sleepThread(1000);
                }
            }
        }
    };

    private void sleepThread(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Runnable codecTask = new Runnable() {
        @Override
        public void run() {
            sleepThread(1000);
            while (true) {
                if (mHandledFrameQueue != null) {
                    if (mHandledFrameQueue.isEmpty()) {
                        if (isStop) {
                            release();
                            break;
                        } else {
                            sleepThread(1000);
                            continue;
                        }
                    }
                    Frame frame = mHandledFrameQueue.poll();
                    if (mVideoCodec == null) {
                        release();
                        break;
                    }
                    if (frame != null) {
                        for (int i = 0; i < frame.writeTimes; i++) {
                            mVideoCodec.encode(frame.data, frame.width, frame.height);
                        }
                    } else {
                        continue;
                    }
                } else {
                    sleepThread(30);
                }
            }
        }
    };

    private static class Frame {
        public static Frame create(byte[] data, int width, int height, long timestamp) {
            Frame frame = new Frame();
            frame.data = data;
            frame.width = width;
            frame.height = height;
            frame.timestamp = timestamp;
            frame.writeTimes = 1;
            return frame;
        }

        public long timestamp;
        public int width;
        public int height;
        public byte[] data;
        public int writeTimes = 1;
    }
}
