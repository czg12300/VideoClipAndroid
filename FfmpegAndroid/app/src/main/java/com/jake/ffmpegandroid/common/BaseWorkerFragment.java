package com.jake.ffmpegandroid.common;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;

/**
 * 可以执行子线程任务的fragment
 *
 * @author jake
 * @since 2017/4/9 下午12:40
 */

public class BaseWorkerFragment extends BaseFragment {
    private Handler mThreadHandler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HandlerThread workThread = new HandlerThread("fragment_worker");
        workThread.start();
        mThreadHandler = new Handler(workThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                handleThreadMessage(msg);
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mThreadHandler != null) {
            mThreadHandler.removeCallbacksAndMessages(null);
            mThreadHandler.getLooper().quit();
        }
    }

    /**
     * 处理Thread的message
     *
     * @param msg
     */
    protected void handleThreadMessage(Message msg) {
    }

    protected void sendThreadMessage(Message msg) {
        if (mThreadHandler != null) {
            mThreadHandler.sendMessage(msg);
        }
    }

    protected void sendThreadMessageDelayed(Message msg, long delayMillis) {
        if (mThreadHandler != null) {
            mThreadHandler.sendMessageDelayed(msg, delayMillis);
        }
    }

    protected void sendThreadEmptyMessage(int what) {
        if (mThreadHandler != null) {
            mThreadHandler.sendEmptyMessage(what);
        }
    }

    protected void sendThreadEmptyMessageDelayed(int what, long delayMillis) {
        if (mThreadHandler != null) {
            mThreadHandler.sendEmptyMessageDelayed(what, delayMillis);
        }
    }

    protected void postToThread(Runnable task) {
        if (mThreadHandler != null) {
            mThreadHandler.post(task);
        }
    }

    protected void postToThreadDelayed(Runnable task, long delayMillis) {
        if (mThreadHandler != null) {
            mThreadHandler.postDelayed(task, delayMillis);
        }
    }

    protected Message obtainThreadMessage() {
        return mThreadHandler != null ? mThreadHandler.obtainMessage() : null;
    }

    protected Handler getThreadHandler() {
        return mThreadHandler;
    }
}
