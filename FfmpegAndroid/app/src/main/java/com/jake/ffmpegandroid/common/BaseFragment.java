package com.jake.ffmpegandroid.common;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * fragment基类
 *
 * @author liuxiong
 * @since 2017/3/8 15:11
 */
public class BaseFragment extends Fragment {
    private Handler mUiHandler;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                handleUiMessage(msg);
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUiHandler != null) {
            mUiHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 处理ui的message
     *
     * @param msg
     */
    protected void handleUiMessage(Message msg) {
    }

    protected void sendUiMessage(Message msg) {
        if (mUiHandler != null) {
            mUiHandler.sendMessage(msg);
        }
    }

    protected void sendUiMessageDelayed(Message msg, long delayMillis) {
        if (mUiHandler != null) {
            mUiHandler.sendMessageDelayed(msg, delayMillis);
        }
    }

    protected void sendUiEmptyMessage(int what) {
        if (mUiHandler != null) {
            mUiHandler.sendEmptyMessage(what);
        }
    }

    protected void sendUiEmptyMessageDelayed(int what, long delayMillis) {
        if (mUiHandler != null) {
            mUiHandler.sendEmptyMessageDelayed(what, delayMillis);
        }
    }

    protected void postToUi(Runnable task) {
        if (mUiHandler != null) {
            mUiHandler.post(task);
        }
    }

    protected void postToUiDelayed(Runnable task, long delayMillis) {
        if (mUiHandler != null) {
            mUiHandler.postDelayed(task, delayMillis);
        }
    }

    protected Message obtainUiMessage() {
        return mUiHandler != null ? mUiHandler.obtainMessage() : null;
    }

    protected Handler getUiHandler() {
        return mUiHandler;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //防止Fragment叠加事件透传
        view.setClickable(true);
    }

    public View findViewById(int id) {
        return getView() != null ? getView().findViewById(id) : null;
    }

    /**
     * 用于判断是否需要全屏
     *
     * @return
     */
    public boolean isFullScreen() {
        return false;
    }

    /**
     * 回退到上一个页面
     */
    public boolean onBackPressed() {
        finish();
        return true;
    }

    /**
     * 结束activity
     */
    public void finish() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
