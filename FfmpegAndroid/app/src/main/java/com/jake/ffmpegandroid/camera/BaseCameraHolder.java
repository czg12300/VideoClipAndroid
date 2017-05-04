package com.jake.ffmpegandroid.camera;

import android.hardware.Camera;
import android.view.Surface;


import com.jake.ffmpegandroid.common.VLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 摄像头使用帮助基类
 *
 * @author jake
 * @since 2017/2/9 下午4:51
 */

public abstract class BaseCameraHolder {
    protected static final int DEFAULT_PICTURE_WIDTH = 1280;
    protected static final int DEFAULT_PICTURE_HEIGHT = 720;
    /**
     * 前置摄像头id
     */
    public static int CAMERA_ID_FRONT = 1;
    /**
     * 后置摄像头id
     */
    public static int CAMERA_ID_BACK = 0;
    protected Camera mCamera;
    protected int mPositionCameraId = 0;
    private Camera.Parameters mCameraParameters = null;
    protected Camera.ShutterCallback mShutterCallback;
    protected Camera.PictureCallback mRawCallback;
    protected Camera.PictureCallback mPostviewCallback;
    protected Camera.PictureCallback mJpegCallback;
    protected Camera.PreviewCallback mPreviewCallback;
    private int mRotation = 1;
    private ArrayList<CameraListener> mCameraListenerList;
    private int mCameraZoom = 10;
    private boolean mIsSetPreviewCallbackWithBuffer = false;
    protected byte[] mPreBuffer;
    private int mDefaultPreviewWidth = DEFAULT_PICTURE_WIDTH;
    private int mDefaultPreviewHeight = DEFAULT_PICTURE_HEIGHT;

    /**
     * 获取前后摄像头的id
     */
    static {
        int num = Camera.getNumberOfCameras();
        VLog.d("num=" + num);
        VLog.d("CAMERA_ID_BACK=" + CAMERA_ID_BACK);
        VLog.d("CAMERA_ID_FRONT=" + CAMERA_ID_FRONT);
        for (int i = 0; i < num; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                CAMERA_ID_BACK = i;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                CAMERA_ID_FRONT = i;
            }
        }
        VLog.d("CAMERA_ID_BACK=" + CAMERA_ID_BACK);
        VLog.d("CAMERA_ID_FRONT=" + CAMERA_ID_FRONT);
    }

    /**
     * 添加摄像头监听
     *
     * @param listener
     */
    public void addCameraListener(CameraListener listener) {
        if (listener == null) {
            return;
        }
        if (mCameraListenerList == null) {
            mCameraListenerList = new ArrayList<>();
        }
        mCameraListenerList.add(listener);
    }

    /**
     * 移除摄像头监听
     *
     * @param listener
     */
    public void removeCameraListener(CameraListener listener) {
        if (listener == null || mCameraListenerList == null) {
            return;
        }
        mCameraListenerList.remove(listener);
    }

    /**
     * 设置屏幕方向
     *
     * @param rotation
     */
    public void setRotation(int rotation) {
        mRotation = rotation;
    }

    public void setPreviewCallback(Camera.PreviewCallback callback) {
        mPreviewCallback = callback;
    }

    public void setPreviewCallback(Camera.PreviewCallback callback, boolean isWithBuffer) {
        mPreviewCallback = callback;
        mIsSetPreviewCallbackWithBuffer = isWithBuffer;
    }

    public void setShutterCallback(Camera.ShutterCallback callback) {
        mShutterCallback = callback;
    }

    public void setRawPictureCallback(Camera.PictureCallback callback) {
        mRawCallback = callback;
    }

    public void setPostviewPictureCallback(Camera.PictureCallback callback) {
        mPostviewCallback = callback;
    }

    public void setJpegPictureCallback(Camera.PictureCallback callback) {
        mJpegCallback = callback;
    }

    /**
     * 重置
     */
    public void reset() {
        stopPreviewAndRelease();
        openCamera(mPositionCameraId);
    }

    /**
     * 打开闪光灯
     */
    public void openFlashLight() {
        if (isCameraOpen()) {
            Camera.Parameters parameter = mCamera.getParameters();
            parameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameter);
        }
    }

    /**
     * 关闭闪光灯
     */
    public void closeFlashLight() {
        if (isCameraOpen()) {
            Camera.Parameters parameter = mCamera.getParameters();
            parameter.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameter);
        }
    }

    /**
     * 打开摄像头，通过设置设置头id，CAMERA_ID_FRONT、CAMERA_ID_BACK
     *
     * @param cameraId
     */
    public void openCamera(int cameraId) {
        stopPreviewAndRelease();
        mCamera = Camera.open(cameraId);
        notifyCameraOpened();
        mCameraParameters = getDefaultParameters();
        int displayDegree = getCameraDisplayOrientation();
        if (mCameraParameters.isZoomSupported()) {
            mCameraParameters.setZoom(mCameraZoom);
        }
        List<int[]> fpsList = mCameraParameters.getSupportedPreviewFpsRange();
        if (fpsList!=null&&fpsList.size()>0) {
            int[] range = {0, 0};
            for (int[] num : fpsList) {
                if (num[0] > range[0]) {
                    range[0] = num[0];
                    range[1] = num[1];
                    VLog.d("fpsList " + num[0] + " : " + num[1]);
                }
            }
            mCameraParameters.setPreviewFpsRange(range[0], range[1]);
        }
        mCamera.setParameters(mCameraParameters);
        mCamera.setDisplayOrientation(displayDegree);
        if (mPreviewCallback != null) {
            if (mIsSetPreviewCallbackWithBuffer) {
                Camera.Size size = mCameraParameters.getPreviewSize();
                if (mPreBuffer == null) {
                    mPreBuffer = new byte[size.width * size.height * 3 / 2];
                }
                mCamera.addCallbackBuffer(mPreBuffer);
                mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
            } else {
                mCamera.setPreviewCallback(mPreviewCallback);
            }

        }
        setCameraPreviewDisplay(mCamera);
        changeZoom(0);
    }

    private void notifyCameraOpened() {
        if (mCameraListenerList != null && mCameraListenerList.size() > 0) {
            for (CameraListener listener : mCameraListenerList) {
                if (listener != null) {
                    listener.onCameraOpened(mCamera);
                }
            }
        }
    }

    /**
     * 停止预览并释放摄像头
     */
    public void stopPreviewAndRelease() {
        if (isCameraOpen()) {
            stopPreview();//停掉原来摄像头的预览
            mCamera.setPreviewCallback(null);
            mCamera.release();//释放资源
            mCamera = null;//取消原来摄像头
        }
    }

    /**
     * 自动对焦
     */
    public void autoFocus() {
        if (isCameraOpen()) {
            mCamera.autoFocus(null);
        }
    }

    public void autoFocus(Camera.AutoFocusCallback callback) {
        if (isCameraOpen()) {
            mCamera.autoFocus(callback);
        }
    }

    /**
     * 照相
     */
    public void takePicture() {
        mCamera.takePicture(mShutterCallback, mRawCallback, mPostviewCallback, mJpegCallback);
    }

    /**
     * 对焦成功后照相
     */
    public void takePictureWithAutoFocus() {
        autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    mCamera.takePicture(mShutterCallback, mRawCallback, mPostviewCallback, mJpegCallback);
                }
            }
        });

    }

    public Camera getCamera() {
        return mCamera;
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (isCameraOpen()) {
            mCamera.stopPreview();//停掉原来摄像头的预览
        }
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        if (isCameraOpen()) {
            mCamera.startPreview();
        }
    }

    /**
     * 切换摄像头
     */
    public void toggleCamera() {
        if (mPositionCameraId == CAMERA_ID_FRONT) {
            chooseBackCamera();
        } else {
            chooseFrontCamera();
        }
    }

    /**
     * 选择后置摄像头
     */
    public void chooseBackCamera() {
        mPositionCameraId = CAMERA_ID_BACK;
        openCamera(mPositionCameraId);
        startPreview();
    }

    /**
     * 判断是否为前置摄像头
     *
     * @return
     */
    public boolean isFacingFront() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mPositionCameraId, cameraInfo);
        return cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    /**
     * 选择前置摄像头
     */
    public void chooseFrontCamera() {
        mPositionCameraId = CAMERA_ID_FRONT;
        openCamera(mPositionCameraId);
        startPreview();
    }

    /**
     * 设置camera的参数
     *
     * @param parameter
     */
    public void setCameraParameters(Camera.Parameters parameter) {
        mCameraParameters = parameter;
    }

    public void setCameraParametersWithNotify(Camera.Parameters parameter) {
        mCameraParameters = parameter;
        if (isCameraOpen()) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    public Camera.Parameters getDefaultParameters() {
        if (isCameraOpen()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.getSupportedFocusModes()
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            List<Camera.Size> preSizes = parameters.getSupportedPreviewSizes();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            Camera.Size largeSize = getLargeSize(preSizes, mDefaultPreviewWidth, mDefaultPreviewHeight);
            parameters.setPreviewSize(largeSize.width, largeSize.height);
            List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
            Camera.Size picSize = getLargeSize(pictureSizes, mDefaultPreviewWidth, mDefaultPreviewHeight);
            parameters.setPictureSize(picSize.width, picSize.height);
            return parameters;
        }
        return null;
    }

    public void setDefaultPreviewSize(int width, int height) {
        mDefaultPreviewWidth = width;
        mDefaultPreviewHeight = height;
    }

    /**
     * 设置缩放比例
     *
     * @param zoom
     */
    public void setCameraZoom(int zoom) {
        mCameraZoom = zoom;
    }

    /**
     * 改变缩放比例
     *
     * @param zoom
     */
    public void changeZoom(int zoom) {
        mCameraZoom = zoom;
        if (isCameraOpen()) {
            Camera.Parameters parameters = mCamera.getParameters();
            if (parameters.isZoomSupported()) {
                parameters.setZoom(zoom);
                mCamera.setParameters(parameters);
            }
        }
    }

    /**
     * 获取当前缩放比例
     *
     * @return
     */
    public int getZoom() {
        int ret = 0;
        if (isCameraOpen()) {
            Camera.Parameters parameters = mCamera.getParameters();
            ret = parameters.getZoom();
        }
        return ret;
    }

    /**
     * 获取最大缩放比例
     *
     * @return
     */
    public int getMaxZoom() {
        int ret = 0;
        if (isCameraOpen()) {
            Camera.Parameters parameters = mCamera.getParameters();
            ret = parameters.getMaxZoom();
        }
        return ret;
    }

    /**
     * Android API: Display Orientation Setting
     * Just change screen display orientation,
     * the rawFrame data never be changed.
     */
    protected int getCameraDisplayOrientation() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mPositionCameraId, info);
        int degrees = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int displayDegree;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayDegree = (info.orientation + degrees) % 360;
            displayDegree = (360 - displayDegree) % 360;  // compensate the mirror
        } else {
            displayDegree = (info.orientation - degrees + 360) % 360;
        }
        return displayDegree;
    }

    /**
     * 设置预览surface
     *
     * @param camera
     */
    protected abstract void setCameraPreviewDisplay(Camera camera);

    protected boolean isCameraOpen() {
        return mCamera != null;
    }

    /**
     * 1.找出和屏幕宽高都一样的，如果有直接返回
     * 2.找出所有比率相等的，取出和参考宽度最相近的一个
     * 3.如果没有比率相等的，找出比率差距在0.1内的，取出和参考宽度最相近的一个
     * 4.如果比率相同的最大size的宽度小于屏幕宽高的2/3,则查看0.1比率的最大值
     * 5.如果0.1比率的最大size的宽度也小于屏幕宽高的2/3，取第一个 6.如果没有，取第一个
     *
     * @param list   被查找的数组
     * @param width  参考的宽度
     * @param height 参考用的高度
     * @return
     */
    private Camera.Size getLargeSize(List<Camera.Size> list, int width, int height) {
        if (width > height) {
            int tempwidth = width;
            width = height;
            height = tempwidth;
        }
        // 存放宽高与屏幕宽高相同的size
        Camera.Size size = null;
        // 存放比率相同的最大size
        Camera.Size largeSameRatioSize = null;
        // 存放比率差距0.1的最大size
        Camera.Size largeRatioSize = null;
        float scrwhRatio = width * 1.0f / height * 1.0f;
        for (Camera.Size preSize : list) {
            float tempRatio = preSize.width * 1.0f / preSize.height * 1.0f;
            if (preSize.width < preSize.height) {
                tempRatio = preSize.width * 1.0f / preSize.height * 1.0f;
                if (preSize.width == width && preSize.height == height) {
                    size = preSize;
                    break;
                }
            } else if (preSize.width > preSize.height) {
                tempRatio = preSize.height * 1.0f / preSize.width * 1.0f;
                if (preSize.height == width && preSize.width == height) {
                    size = preSize;
                    break;
                }
            }

            if (tempRatio == scrwhRatio) {
                if (largeSameRatioSize == null) {
                    largeSameRatioSize = preSize;
                } else {
                    if (Math.abs(largeSameRatioSize.width - width) > Math.abs(preSize.width - width)) {
                        largeSameRatioSize = preSize;
                    }
                }
            }

            float ratioDistance = Math.abs(tempRatio - scrwhRatio);
            if (ratioDistance < 0.1) {
                if (largeRatioSize == null) {
                    largeRatioSize = preSize;
                } else {
                    if (Math.abs(largeRatioSize.width - width) > Math.abs(preSize.width - width)) {
                        largeRatioSize = preSize;
                    }
                }
            }
        }

        if (size != null) {
            return size;
        } else if (largeSameRatioSize != null) {
            if (Math.abs(largeSameRatioSize.width - width) < (width * 1.0f / 3.0f)) {
                return largeSameRatioSize;
            } else if (largeRatioSize != null) {
                if (Math.abs(largeRatioSize.width - width) < (width * 1.0f / 3.0f)) {
                    return largeRatioSize;
                } else {
                    return list.get(0);
                }
            } else {
                return list.get(0);
            }
        } else if (largeRatioSize != null) {
            if (Math.abs(largeRatioSize.width - width) < (width * 1.0f / 3.0f)) {
                return largeRatioSize;
            } else {
                return list.get(0);
            }
        } else {
            return list.get(0);
        }
    }

    /**
     * 摄像头监听
     */
    public static interface CameraListener {
        void onCameraOpened(Camera camera);
    }
}
