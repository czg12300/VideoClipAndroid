package com.jake.ffmpegandroid.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;

import com.jake.ffmpegandroid.gpuimage.FilterFactory;
import com.jake.ffmpegandroid.gpuimage.FilterType;

import java.io.IOException;

/**
 * @author jake
 * @since 2017/4/24 下午9:54
 */

public class GlCameraHolderOld extends BaseCameraHolder {
//    private CameraRenderer mCameraRenderer;
//
//    public GlCameraHolderOld(GLSurfaceView glSurfaceView) {
//        setDefaultPreviewSize(800, 480);
//        mCameraRenderer = new CameraRenderer(glSurfaceView, onRendererListener);
//        mCameraRenderer.setFilter(FilterFactory.getFilter(FilterType.BEAUTY, glSurfaceView.getContext()));
//    }

    @Override
    protected void setCameraPreviewDisplay(Camera camera) {
//        SurfaceTexture texture = mCameraRenderer.getSurfaceTexture();
//        if (texture != null) {
//            try {
//                camera.setPreviewTexture(texture);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }
//
//    private CameraRenderer.OnRendererListener onRendererListener = new CameraRenderer.OnRendererListener() {
//        @Override
//        public void openRendererCamera(SurfaceTexture surfaceTexture) {
//            onResume();
//            final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
//            mCameraRenderer.updateRotation(getCameraDisplayOrientation(), previewSize.width, previewSize.height, isFacingFront());
//
//        }
//    };
//
//    public void onResume() {
//        if (mCameraRenderer.isSurfaceCreated()) {
//            openCamera(mPositionCameraId);
//            startPreview();
//        }
//    }
//
//    public void onPause() {
//        stopPreviewAndRelease();
//    }
//
//    public void onDestroy() {
//        if (isCameraOpen()) {
//            stopPreviewAndRelease();
//        }
//    }
}
