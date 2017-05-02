package com.jake.ffmpegandroid.camera;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.ViewGroup;

import com.jake.ffmpegandroid.cameraimp.CameraImp;
import com.jake.ffmpegandroid.cameraimp.CameraImpFactory;
import com.jake.ffmpegandroid.cameraimp.CameraUtils;
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

    public GlCameraHolder(GLSurfaceView glSurfaceView) {
        mGLSurfaceView = glSurfaceView;
        mCameraImp = CameraImpFactory.getCameraImp(glSurfaceView.getContext());
        mCameraImp.setParameters(CameraImp.CameraImpParametersBuilder.create()
                .setPictureSize(new CameraImp.Size(1080, 1920))
                .setPreviewSize(new CameraImp.Size(640, 480))
                .setPictureFormat(ImageFormat.JPEG)
                .setPreviewFormat(ImageFormat.YV12)
                .build());
        mOrientation = Surface.ROTATION_0;
        mCameraImp.setCameraImpCallback(cameraImpCallback);
        mCameraRenderer = new CameraRenderer(glSurfaceView, onRendererListener);
        mCameraRenderer.setFilter(FilterFactory.getFilter(FilterType.BEAUTY, glSurfaceView.getContext()));

    }

    public void setPreviewCallback(CameraImp.PreviewCallback callback) {
        mCameraImp.setPreviewCallback(callback);
    }

    public CameraImp getCameraImp() {
        return mCameraImp;
    }

    private CameraImp.CameraImpCallback cameraImpCallback = new CameraImp.CameraImpCallback() {
        @Override
        public void onCameraOpened(final CameraImp cameraImp, final int width, final int height) {
            LogUtil.d("width:height =" + width + ":" + height);
            mGLSurfaceView.post(new Runnable() {
                @Override
                public void run() {
                    int sw = mGLSurfaceView.getResources().getDisplayMetrics().widthPixels;
                    int sh = mGLSurfaceView.getResources().getDisplayMetrics().widthPixels;
                    int glWidth, glHeight;
//                            if (mOrientation == Surface.ROTATION_90 || mOrientation == Surface.ROTATION_270) {
//                                glWidth = sh;
//                                glHeight = sh * height / width;
//                            } else {
                    glWidth = sw;
                    if (width > height) {
                        glHeight = sw * width / height;
                    } else {
                        glHeight = sw * height / width;
                    }
//                            }
                    LogUtil.d("width:height =" + glWidth + ":" + glHeight);
                    ViewGroup.LayoutParams lp = mGLSurfaceView.getLayoutParams();
                    lp.width = glWidth;
                    lp.height = glHeight;
                    mCameraRenderer.updateRotation(CameraUtils.getDegreesByOrientation(mOrientation, cameraImp), cameraImp.isFacingFront());
                    mGLSurfaceView.setLayoutParams(lp);
                    mCameraRenderer.updateSize(glWidth, glHeight);

                }
            });
            onResume();
        }

        @Override
        public void onCameraClosed() {

        }
    };

    public void onConfigChange(int orientation) {
        mOrientation = orientation;
        mCameraRenderer.updateRotation(CameraUtils.getDegreesByOrientation(orientation, mCameraImp), mCameraImp.isFacingFront());

    }

    public void toggleCamera() {
        mCameraImp.toggleCamera();
    }

    private CameraRenderer.OnRendererListener onRendererListener = new CameraRenderer.OnRendererListener() {
        @Override
        public void openRendererCamera(SurfaceTexture texture) {
            mCameraImp.setDisplay(texture);

            mCameraImp.openBackCamera();

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
//        orientationEventListener.disable();
    }
}
