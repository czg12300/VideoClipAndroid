package com.jake.ffmpegandroid.ui;

import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jake.ffmpegandroid.R;
import com.jake.ffmpegandroid.camera.GlCameraHolder;
import com.jake.ffmpegandroid.record.camera.CameraImp;
import com.jake.ffmpegandroid.common.BaseWorkerFragment;
import com.jake.ffmpegandroid.common.VLog;
import com.jake.ffmpegandroid.record.VideoRecord;
import com.jake.ffmpegandroid.record.videocodec.VideoCodecParameters;

import java.io.File;

/**
 * 拍摄页面
 *
 * @author jake
 * @since 2017/4/24 下午9:21
 */

public class CameraFragment extends BaseWorkerFragment {
    private ImageView mBtnCamera;
    private ImageView mBtnToggle;
    private ImageView mBtnFlash;
    private GLSurfaceView mGlPreview;
    private GlCameraHolder mCameraHolder;
    private TextView mTvFrameRate;
    private VideoRecord mVideoRecord;
    private int state = 0;

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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mCameraHolder.onConfigChange(getActivity().getWindowManager().getDefaultDisplay().getRotation());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGlPreview = (GLSurfaceView) findViewById(R.id.gl_camera);
        mBtnCamera = (ImageView) findViewById(R.id.btn_camera);
        mBtnToggle = (ImageView) findViewById(R.id.btn_toggle);
        mBtnFlash = (ImageView) findViewById(R.id.btn_flash);
        mTvFrameRate = (TextView) findViewById(R.id.tv_frame_rate);
        mBtnCamera.setOnClickListener(onClickListener);
        mBtnToggle.setOnClickListener(onClickListener);
        mBtnFlash.setOnClickListener(onClickListener);
        mCameraHolder = new GlCameraHolder(mGlPreview);
        mCameraHolder.setPreviewCallback(previewCallback);
        mVideoRecord = new VideoRecord();
    }

    long lastTime;
    int frameRate = 0;
    private CameraImp.PreviewCallback previewCallback = new CameraImp.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, int width, int height, CameraImp cameraImp) {
            if (state == 1) {
                mVideoRecord.setVideoCodecParameters(VideoCodecParameters.VideoCodecParametersBuilder.create()
                        .setCodecType(VideoCodecParameters.CodecType.H264)
                        .setBitRate(2 * 1024 * 1024)
                        .setFrameRate(25)
                        .setKeyIFrameInterval(1)
                        .setWidth(width)
                        .setHeight(height)
                        .build());
                mVideoRecord.start(getTempFile());
                state = 2;
            } else if (state == 2) {
                mVideoRecord.record(data, width, height, System.currentTimeMillis());
            } else if (state == 3) {
                mVideoRecord.pause();
                state = 0;
            } else if (state == 4) {
                mVideoRecord.resume();
                state = 0;
            } else if (state == 5) {
                mVideoRecord.stop();
                state = 0;
            }
            long now = System.currentTimeMillis();
            if (now - lastTime > 1000) {
                lastTime = now;
                mTvFrameRate.setText("帧率：" + frameRate);
//                VLog.d("onPreviewFrame frame rate=" + frameRate + "data len=" + data.length + " width=" + width + " height=" + height);
                frameRate = 1;
            } else {
                frameRate++;
            }
        }
    };

    private String getTempFile() {
        return new File(Environment.getExternalStorageDirectory(), "AAAAA/out.h264").getAbsolutePath();
    }

    private long lastStartTime;
    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mBtnCamera) {
                if (mBtnCamera.isSelected()) {
                    state = 5;
                    mBtnCamera.setSelected(false);
                    mBtnCamera.setImageResource(android.R.drawable.ic_menu_camera);
                    long now = System.currentTimeMillis();
                    Toast.makeText(v.getContext(), "视频长度为" + (now - lastStartTime) / 1000 + " s", Toast.LENGTH_LONG).show();
                    lastStartTime = now;

                } else {
                    mBtnCamera.setImageResource(android.R.drawable.ic_media_pause);
                    mBtnCamera.setSelected(true);
                    state = 1;
                    lastStartTime = System.currentTimeMillis();
                }
            } else if (v == mBtnToggle) {
                mCameraHolder.toggleCamera();
            } else if (v == mBtnFlash) {
                if (v.isSelected()) {
                    v.setSelected(false);
                    mCameraHolder.getCameraImp().closeFlashLight();
                    mBtnFlash.setImageResource(android.R.drawable.ic_lock_power_off);
                } else {
                    v.setSelected(true);
                    mBtnFlash.setImageResource(android.R.drawable.ic_btn_speak_now);
                    mCameraHolder.getCameraImp().openFlashLight();
                }
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
