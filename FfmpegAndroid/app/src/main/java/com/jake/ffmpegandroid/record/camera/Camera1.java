package com.jake.ffmpegandroid.record.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.v4.app.ActivityCompat;
import android.view.SurfaceHolder;

import com.jake.ffmpegandroid.common.VLog;

import java.io.IOException;
import java.util.List;

/**
 * 使用老的camera api
 *
 * @author jake
 * @since 2017/4/27 上午11:43
 */

public class Camera1 implements CameraImp {
    /**
     * 前置摄像头id
     */
    public int CAMERA_ID_FRONT = 1;
    /**
     * 后置摄像头id
     */
    public int CAMERA_ID_BACK = 0;
    protected Camera mCamera;
    protected int mCurrentCameraId = 0;
    private Size mPreviewSize = new Size(720, 1280);
    private Size mPictureSize = new Size(720, 1280);
    private int pictureFormat = ImageFormat.JPEG;
    private int previewFormat = ImageFormat.NV21;
    private PictureCallback mPictureCallback;
    private PreviewCallback mPreviewCallback;
    private CameraImpCallback mCameraImpCallback;
    private SurfaceTexture mPreviewSurfaceTexture;
    private SurfaceHolder mPreviewSurfaceHolder;
    private boolean hasAvailableCamera = false;
    private Context mAppContext;

    public Camera1(Context context) {
        mAppContext = context.getApplicationContext();
        init();
    }

    private void init() {
        int num = Camera.getNumberOfCameras();
        VLog.d("num=" + num);
        VLog.d("CAMERA_ID_BACK=" + CAMERA_ID_BACK);
        VLog.d("CAMERA_ID_FRONT=" + CAMERA_ID_FRONT);
        if (num > 0) {
            hasAvailableCamera = true;
            for (int i = 0; i < num; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    CAMERA_ID_BACK = i;
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    CAMERA_ID_FRONT = i;
                }
            }
        } else {
            hasAvailableCamera = false;
        }
        VLog.d("CAMERA_ID_BACK=" + CAMERA_ID_BACK);
        VLog.d("CAMERA_ID_FRONT=" + CAMERA_ID_FRONT);
    }


    private boolean isCameraOpen() {
        return mCamera != null;
    }


    private void openCamera() {
        if (!hasAvailableCamera) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(mAppContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            VLog.e("没有照相机权限");
            return;
        }
        if (mCamera != null) {
            release();
        }
        try {
            mCamera = Camera.open(mCurrentCameraId);
        } catch (Exception e) {
            e.printStackTrace();
            mCamera = null;
        }
        if (!isCameraOpen()) {
            return;
        }
        updateCameraParameters();
        if (mPreviewSurfaceTexture != null) {
            try {
                mCamera.setPreviewTexture(mPreviewSurfaceTexture);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        if (mPreviewSurfaceHolder != null) {
            try {
                mCamera.setPreviewDisplay(mPreviewSurfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
//        mCamera.setDisplayOrientation(getCameraDisplayOrientation());
        mCamera.addCallbackBuffer(new byte[mPreviewSize.width * mPreviewSize.height * 3 / 2]);
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                camera.addCallbackBuffer(data);
                if (mPreviewCallback != null) {
                    mPreviewCallback.onPreviewFrame(data, mPreviewSize.width, mPreviewSize.height, getCameraImp());
                }
            }
        });
        if (mCameraImpCallback != null) {
            mCameraImpCallback.onCameraOpened(getCameraImp(), mPreviewSize.width, mPreviewSize.height);
        }
    }

    /**
     * 更新摄像头参数设置
     */
    private void updateCameraParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        mPreviewSize = CameraUtils.getLargeSize(CameraUtils.transArrayToList(parameters.getSupportedPreviewSizes()), mPreviewSize.width, mPreviewSize.height);
        mPictureSize = CameraUtils.getLargeSize(CameraUtils.transArrayToList(parameters.getSupportedPictureSizes()), mPictureSize.width, mPictureSize.height);
        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        parameters.setPictureSize(mPictureSize.width, mPictureSize.height);
        if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        if (parameters.isZoomSupported()) {
            parameters.setZoom(0);
        }
        List<int[]> fpsList = parameters.getSupportedPreviewFpsRange();
        if (fpsList != null && fpsList.size() > 0) {
            int[] range = {0, 0};
            for (int[] num : fpsList) {
                if (num[0] > range[0]) {
                    range[0] = num[0];
                    range[1] = num[1];
                    VLog.d("fpsList " + num[0] + " : " + num[1]);
                }
            }
            parameters.setPreviewFpsRange(range[0], range[1]);
        }
        parameters.setPictureFormat(pictureFormat);
        parameters.setPreviewFormat(previewFormat);
        mCamera.setParameters(parameters);
    }


    @Override
    public void takePicture() {
        mCamera.takePicture(null, null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                if (mPictureCallback != null) {
                    mPictureCallback.onPictureFrame(data, mPictureSize.width, mPictureSize.height, getCameraImp());
                }
            }
        });
    }

    private CameraImp getCameraImp() {
        return this;
    }

    @Override
    public void stopPreview() {
        if (isCameraOpen()) {
            mCamera.stopPreview();//停掉原来摄像头的预览
        }
    }

    @Override
    public void startPreview() {
        if (isCameraOpen()) {
            mCamera.startPreview();
        }
    }

    @Override
    public void toggleCamera() {
        if (mCurrentCameraId == CAMERA_ID_FRONT) {
            openBackCamera();
        } else {
            openFrontCamera();
        }
    }

    @Override
    public void openBackCamera() {
        mCurrentCameraId = CAMERA_ID_BACK;
        openCamera();
    }

    @Override
    public void openFrontCamera() {
        mCurrentCameraId = CAMERA_ID_FRONT;
        openCamera();
    }

    @Override
    public boolean isFacingFront() {
        boolean result = false;
        if (isCameraOpen()) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(mCurrentCameraId, cameraInfo);
            result = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        return result;
    }


    @Override
    public void setZoom(int zoom) {
        if (isCameraOpen()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                parameters.setZoom(zoom);
                mCamera.setParameters(parameters);
            }
        }
    }

    @Override
    public int getZoom() {
        int ret = 0;
        if (isCameraOpen()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                ret = parameters.getZoom();
            }
        }
        return ret;
    }

    @Override
    public int getMaxZoom() {
        int ret = 0;
        if (isCameraOpen()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                ret = parameters.getMaxZoom();
            }
        }
        return ret;
    }

    @Override
    public void setParameters(CameraImpParameters parameters) {
        if (parameters != null) {
            pictureFormat = parameters.pictureFormat;
            previewFormat = parameters.previewFormat;
            if (parameters.previewSize != null) {
                mPreviewSize = parameters.previewSize;
            }
            if (parameters.pictureSize != null) {
                mPictureSize = parameters.pictureSize;
            }
        }
    }

    @Override
    public void setDisplay(SurfaceTexture surfaceTexture) {
        mPreviewSurfaceTexture = surfaceTexture;
    }

    @Override
    public void setDisplay(SurfaceHolder holder) {
        mPreviewSurfaceHolder = holder;
    }


    @Override
    public void release() {
        if (mCamera != null) {
            stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();//释放资源
            mCamera = null;//取消原来摄像头
        }
    }

    @Override
    public boolean zoomIn() {
        if (isCameraOpen()) {
            int max = getMaxZoom();
            if (max > 0) {
                int zoom = getZoom();
                zoom += max / 10;
                if (zoom > max) {
                    zoom = max;
                }
                setZoom(zoom);
                return true;
            }

        }
        return false;
    }

    @Override
    public boolean zoomOut() {
        if (isCameraOpen()) {
            int max = getMaxZoom();
            if (max > 0) {
                int zoom = getZoom();
                zoom -= max / 10;
                if (zoom < 0) {
                    zoom = 0;
                }
                setZoom(zoom);
                return true;
            }

        }
        return false;
    }

    @Override
    public void openFlashLight() {
        if (isCameraOpen()) {
            Camera.Parameters parameter = mCamera.getParameters();
            parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameter);
        }
    }

    @Override
    public void closeFlashLight() {
        if (isCameraOpen()) {
            Camera.Parameters parameter = mCamera.getParameters();
            parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameter);
        }
    }

    @Override
    public void setPictureCallback(PictureCallback pictureCallback) {
        this.mPictureCallback = pictureCallback;
    }

    @Override
    public void setPreviewCallback(PreviewCallback previewCallback) {
        this.mPreviewCallback = previewCallback;
    }

    @Override
    public void setCameraImpCallback(CameraImpCallback callback) {
        this.mCameraImpCallback = callback;
    }

}
