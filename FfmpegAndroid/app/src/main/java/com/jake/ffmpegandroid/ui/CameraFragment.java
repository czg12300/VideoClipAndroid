package com.jake.ffmpegandroid.ui;

import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.jake.ffmpegandroid.R;
import com.jake.ffmpegandroid.camera.CameraRenderer;
import com.jake.ffmpegandroid.camera.GlCameraHolder;
import com.jake.ffmpegandroid.common.BaseWorkerFragment;
import com.jake.ffmpegandroid.common.LogUtil;

/**
 * 拍摄页面
 *
 * @author jake
 * @since 2017/4/24 下午9:21
 */

public class CameraFragment extends BaseWorkerFragment {
    private ImageView mBtnCamera;
    private ImageView mBtnToggle;
    private GLSurfaceView mGlPreview;
    private GlCameraHolder mCameraHolder;
    private TextView mTvFrameRate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public boolean isFullScreen() {
        return true;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlPreview = (GLSurfaceView) findViewById(R.id.gl_camera);
        mBtnCamera = (ImageView) findViewById(R.id.btn_camera);
        mBtnToggle = (ImageView) findViewById(R.id.btn_toggle);
        mTvFrameRate = (TextView) findViewById(R.id.tv_frame_rate);
        mBtnCamera.setOnClickListener(onClickListener);
        mBtnToggle.setOnClickListener(onClickListener);
        mCameraHolder = new GlCameraHolder(mGlPreview);
//        mCameraHolder.setPreviewCallback(previewCallback);
    }

    long lastTime;
    int frameRate = 0;
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            long now = System.currentTimeMillis();
            if (now - lastTime > 1000) {
                lastTime = now;
                mTvFrameRate.setText("当前帧率：" + frameRate);

                frameRate = 1;
            } else {
                frameRate++;
            }
        }
    };
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mBtnCamera) {
                if (mBtnCamera.isSelected()) {
                    mBtnCamera.setSelected(false);
                    mBtnCamera.setImageResource(android.R.drawable.ic_menu_camera);
                } else {
                    mBtnCamera.setImageResource(android.R.drawable.ic_media_pause);
                    mBtnCamera.setSelected(true);
                }
            } else if (v == mBtnToggle) {
//                mCameraHolder.toggleCamera();
            }
        }
    };


    @Override
    public void onResume() {
        super.onResume();

        mCameraHolder.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCameraHolder.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCameraHolder.onDestroy();
    }
}
