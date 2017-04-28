package com.jake.ffmpegandroid.cameraimp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.jake.ffmpegandroid.common.LogUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用camera2 api
 *
 * @author jake
 * @since 2017/4/27 上午11:45
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class Camera2 implements CameraImp {
    //后置摄像头id
    private String CAMERA_ID_BACK = "0";
    //前置摄像头id
    private String CAMERA_ID_FRONT = "1";
    private CameraDevice mDevice;
    private CameraManager mCameraManager;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mPictureImageReader;
    private ImageReader mPreviewImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private Handler mThreadHandler;
    private PictureCallback mPictureCallback;
    private PreviewCallback mPreviewCallback;
    private CameraImpCallback mCameraImpCallback;
    private SurfaceTexture mPreviewSurfaceTexture;
    private SurfaceHolder mPreviewSurfaceHolder;
    private boolean hasAvailableCamera = false;
    private String mCurrentCameraId;
    private Size mPreviewSize = new Size(720, 1280);
    private Size mPictureSize = new Size(720, 1280);
    private int pictureFormat = ImageFormat.JPEG;
    private int previewFormat = ImageFormat.NV21;
    private Context mAppContext;
    private int mDisplayOrientation;

    public Camera2(Context context) {
        mAppContext = context.getApplicationContext();
        mCameraManager = (CameraManager) mAppContext.getSystemService(Context.CAMERA_SERVICE);
        HandlerThread works = new HandlerThread("Camera2");
        works.start();
        mThreadHandler = new Handler(works.getLooper());
        try {
            init();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            hasAvailableCamera = false;
        }
    }

    private void init() throws CameraAccessException {
        String[] ids = mCameraManager.getCameraIdList();
        if (ids != null && ids.length > 0) {
            hasAvailableCamera = true;
            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null ||
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    continue;
                }
                int internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == CameraCharacteristics.LENS_FACING_BACK) {
                    CAMERA_ID_BACK = id;
                } else if (internal == CameraCharacteristics.LENS_FACING_FRONT) {
                    CAMERA_ID_FRONT = id;
                }
            }
        }
        mCurrentCameraId = CAMERA_ID_BACK;
    }

    private CameraImp getCameraImp() {
        return this;
    }

    //    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mDevice = cameraDevice;
            try {
                updateCameraParameters();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mDevice = null;
            if (mCameraImpCallback != null) {
                mCameraImpCallback.onCameraClosed();
            }
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mDevice = null;
        }

    };

    private void updateCameraParameters() throws CameraAccessException {
        CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            List<Size> previewSizes = CameraUtils.transArrayToList(map.getOutputSizes(SurfaceTexture.class));
            mPreviewSize = CameraUtils.getLargeSize(previewSizes, mPreviewSize.width, mPreviewSize.height);
            List<Size> pictureSizes = CameraUtils.transArrayToList(map.getOutputSizes(SurfaceTexture.class));
            mPictureSize = CameraUtils.getLargeSize(pictureSizes, mPictureSize.width, mPictureSize.height);
        }
        List<Surface> surfaceList = new ArrayList<>();
        mPreviewBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        if (mPreviewSurfaceTexture != null) {
            Surface textureSurface = new Surface(mPreviewSurfaceTexture);
            surfaceList.add(textureSurface);
            mPreviewBuilder.addTarget(textureSurface);
        }
        if (mPreviewSurfaceHolder != null) {
            surfaceList.add(mPreviewSurfaceHolder.getSurface());
            mPreviewBuilder.addTarget(mPreviewSurfaceHolder.getSurface());
        }
        if (mPictureCallback != null) {
            if (mPictureImageReader == null) {
                mPictureImageReader = ImageReader.newInstance(mPictureSize.width, mPictureSize.height, pictureFormat, 2);
                mPictureImageReader.setOnImageAvailableListener(mPictureOnImageAvailableListener, mThreadHandler);
            }
            mPreviewBuilder.addTarget(mPictureImageReader.getSurface());
            surfaceList.add(mPictureImageReader.getSurface());
        }
        if (mPreviewCallback != null) {
            if (mPreviewImageReader == null) {
                mPreviewImageReader = ImageReader.newInstance(mPreviewSize.width, mPreviewSize.height, previewFormat, 2);
                mPreviewImageReader.setOnImageAvailableListener(mPreviewOnImageAvailableListener, mThreadHandler);
            }
            mPreviewBuilder.addTarget(mPreviewImageReader.getSurface());
            surfaceList.add(mPreviewImageReader.getSurface());
        }
        if (surfaceList != null) {
            mDevice.createCaptureSession(surfaceList, mCaptureSessionStateCallback, mThreadHandler);
        }
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION,getCameraDisplayOrientation());
//        mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, (sensorOrientation + getCameraDisplayOrientation() * (isFacingFront() ? 1 : -1) + 360) % 360);
        mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, mPreviewBuilder.get(CaptureRequest.CONTROL_AF_MODE));
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            //TODO
        }
    };
    CameraCaptureSession.StateCallback mCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCameraCaptureSession = session;
            if (mCameraImpCallback != null) {
                mCameraImpCallback.onCameraOpened(getCameraImp(), mPreviewSize.width, mPreviewSize.height);
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            release();
        }
    };


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
    public void openFrontCamera() {
        mCurrentCameraId = CAMERA_ID_FRONT;
        openCamera();
    }

    @Override
    public void openBackCamera() {
        mCurrentCameraId = CAMERA_ID_BACK;
        openCamera();
    }

    @Override
    public void toggleCamera() {
        if (TextUtils.equals(mCurrentCameraId, CAMERA_ID_FRONT)) {
            openFrontCamera();
        } else {
            openBackCamera();
        }
    }

    private void openCamera() {
        if (!hasAvailableCamera) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(mAppContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            LogUtil.e("没有照相机权限");
            return;
        }
        try {
            mCameraManager.openCamera(mCurrentCameraId, mStateCallback, mThreadHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void takePicture() {
        if (!isCameraOpen()) {
            return;
        }
        try {
            mPreviewBuilder = mDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            updateCameraParameters();
            mCameraCaptureSession.stopRepeating();
            mCameraCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback, mThreadHandler);
        } catch (CameraAccessException e) {
            LogUtil.e("Cannot capture a still picture." + e.getMessage());
        }
    }

    @Override
    public boolean isFacingFront() {
        return TextUtils.equals(mCurrentCameraId, CAMERA_ID_FRONT);
    }

    private boolean isCameraOpen() {
        return mDevice != null;
    }

    @Override
    public void startPreview() {
        if (!isCameraOpen()) {
            return;
        }

        try {
//            mCameraCaptureSession.capture(mPreviewBuilder.build(), mCaptureCallback, mThreadHandler);
            if (mCameraCaptureSession != null && mPreviewBuilder != null) {
                mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mCaptureCallback, mThreadHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void stopPreview() {
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void release() {
        if (mThreadHandler != null) {
            mThreadHandler.getLooper().quit();
            mThreadHandler = null;
        }
        if (mPictureImageReader != null) {
            mPictureImageReader.close();
            mPictureImageReader = null;
        }
        if (mPreviewImageReader != null) {
            mPreviewImageReader.close();
            mPreviewImageReader = null;
        }
    }

    @Override
    public int getMaxZoom() {
        if (isCameraOpen()) {
            try {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
                Float distance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                if (distance != null) {
                    LogUtil.d("max zoom :" + distance);
                    return distance.intValue();
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    @Override
    public void setZoom(int zoom) {
        if (isCameraOpen()) {
            try {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
                Float distance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                if (distance != null) {
                    if (zoom > distance.intValue()) {
                        zoom = distance.intValue();
                    } else if (zoom < 0) {
                        zoom = 0;
                    }
                    mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                    mPreviewBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, Integer.valueOf(zoom).floatValue());
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getZoom() {
        if (isCameraOpen()) {
            try {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
                Float distance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
                if (distance != null) {
                    distance = mPreviewBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE);
                    LogUtil.d(" zoom :" + distance);
                    return distance.intValue();
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        return 0;
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
        if (mPreviewBuilder != null) {
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }

    }

    @Override
    public void closeFlashLight() {
        if (mPreviewBuilder != null) {
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }
    }

    @Override
    public void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
    }

    private int getSensorOrientation() {
        int ret = 0;
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
            if (characteristics != null) {
                ret = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return ret;
    }

    @Override
    public int getCameraDisplayOrientation() {
        int degrees = 0;
        switch (mDisplayOrientation) {
            case Surface.ROTATION_0:
                degrees = 90;
                break;
            case Surface.ROTATION_90:
                degrees = 0;
                break;
            case Surface.ROTATION_180:
                degrees = 270;
                break;
            case Surface.ROTATION_270:
                degrees = 180;
                break;
        }
        int displayDegree;
        if (isFacingFront()) {
            displayDegree = degrees % 360;
            displayDegree = (360 - displayDegree) % 360;  // compensate the mirror
        } else {
            displayDegree=degrees;
//            displayDegree = (360 - degrees) % 360;
        }
        LogUtil.d("displayDegree"+displayDegree);
        return displayDegree;
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


    private ImageReader.OnImageAvailableListener mPictureOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    if (mPictureCallback != null) {
                        mPictureCallback.onPictureFrame(data, image.getWidth(), image.getHeight(), getCameraImp());
                    }
                }
            }

        }
    };
    private ImageReader.OnImageAvailableListener mPreviewOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    if (mPreviewCallback != null) {
                        mPreviewCallback.onPreviewFrame(data, image.getWidth(), image.getHeight(), getCameraImp());
                    }
                }
            }
        }
    };
}