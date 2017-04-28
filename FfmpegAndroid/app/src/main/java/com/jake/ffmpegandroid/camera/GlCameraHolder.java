package com.jake.ffmpegandroid.camera;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.view.OrientationEventListener;
import android.view.ViewGroup;

import com.jake.ffmpegandroid.cameraimp.CameraImp;
import com.jake.ffmpegandroid.cameraimp.CameraImpFactory;
import com.jake.ffmpegandroid.common.LogUtil;
import com.jake.ffmpegandroid.gpuimage.FilterFactory;
import com.jake.ffmpegandroid.gpuimage.FilterType;

/**
 * @author jake
 * @since 2017/4/24 下午9:54
 */

public class GlCameraHolder {
    private CameraRenderer mCameraRenderer;
    private CameraImp mCameraImp;
    private int mOrientation;
    private GLSurfaceView mGLSurfaceView;
    private OrientationEventListener orientationEventListener;

    public GlCameraHolder(GLSurfaceView glSurfaceView) {
        mGLSurfaceView = glSurfaceView;
        mCameraImp = CameraImpFactory.getCameraImp(glSurfaceView.getContext());
        mCameraImp.setParameters(CameraImp.CameraImpParametersBuilder.create()
                .setPictureSize(new CameraImp.Size(1080, 1920))
                .setPreviewSize(new CameraImp.Size(720, 1280))
                .setPictureFormat(ImageFormat.JPEG)
                .setPreviewFormat(ImageFormat.NV21)
                .build());
        mOrientation = Configuration.ORIENTATION_PORTRAIT;
        mCameraImp.setDisplayOrientation(mOrientation);
        mCameraRenderer = new CameraRenderer(glSurfaceView, onRendererListener);
        mCameraRenderer.setFilter(FilterFactory.getFilter(FilterType.BEAUTY, glSurfaceView.getContext()));
        orientationEventListener = new OrientationEventListener(mGLSurfaceView.getContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                mOrientation = orientation;
            }
        };
        orientationEventListener.enable();

    }

    private CameraRenderer.OnRendererListener onRendererListener = new CameraRenderer.OnRendererListener() {
        @Override
        public void openRendererCamera(SurfaceTexture texture) {
            mCameraImp.setDisplay(texture);
            mCameraImp.openBackCamera();
            mCameraImp.setCameraImpCallback(new CameraImp.CameraImpCallback() {
                @Override
                public void onCameraOpened(final CameraImp cameraImp, final int width, final int height) {
                    LogUtil.d("width:height =" + width + ":" + height);
                    mGLSurfaceView.post(new Runnable() {
                        @Override
                        public void run() {
                            int sw = mGLSurfaceView.getResources().getDisplayMetrics().widthPixels;
                            ViewGroup.LayoutParams lp = mGLSurfaceView.getLayoutParams();
                            lp.width = width;
                            lp.height = height;
//                            if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
//                                lp.height = sw * height / width;
//                            } else {
//                                if (width > height) {
//                                    lp.height = sw * width / height;
//                                } else {
//                                    lp.height = sw * height / width;
//                                }
//                            }
                            LogUtil.d("width:height =" + lp.width + ":" + lp.height );
                            mGLSurfaceView.setLayoutParams(lp);
                        }
                    });

                    onResume();
                    mCameraRenderer.updateRotation(cameraImp.getCameraDisplayOrientation(), width, height, cameraImp.isFacingFront());
                }

                @Override
                public void onCameraClosed() {

                }
            });


        }
    };

    public void onResume() {
        if (mCameraRenderer.isSurfaceCreated()) {
            mCameraImp.startPreview();
        }
    }

    public void onPause() {
        mCameraImp.stopPreview();
    }

    public void onDestroy() {
        orientationEventListener.disable();
    }
}
