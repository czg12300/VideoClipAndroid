package com.jake.ffmpegandroid.cameraimp;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.view.SurfaceHolder;

/**
 * camera实现
 *
 * @author jake
 * @since 2017/4/26 下午2:51
 */

public interface CameraImp {
    interface CameraImpCallback {
        void onCameraOpened(CameraImp cameraImp, int width, int height);

        void onCameraClosed();
    }

    interface PreviewCallback {
        void onPreviewFrame(byte[] data, int width, int height, CameraImp cameraImp);
    }

    interface PictureCallback {
        void onPictureFrame(byte[] data, int width, int height, CameraImp cameraImp);
    }

    void setPictureCallback(PictureCallback pictureCallback);

    void setPreviewCallback(PreviewCallback previewCallback);

    void setCameraImpCallback(CameraImpCallback callback);

    void setParameters(CameraImpParameters parameters);

    void setDisplay(SurfaceTexture surfaceTexture);

    void setDisplay(SurfaceHolder holder);

    void openFrontCamera();

    void openBackCamera();

    void toggleCamera();

    void takePicture();

    boolean isFacingFront();

    void startPreview();

    void stopPreview();

    void release();

    int getMaxZoom();

    void setZoom(int zoom);

    int getZoom();

    boolean zoomIn();

    boolean zoomOut();

    void openFlashLight();

    void closeFlashLight();

    void setDisplayOrientation(int displayOrientation);

    int getCameraDisplayOrientation();

    public static class CameraImpParametersBuilder {
        private CameraImpParameters parameters;

        private CameraImpParametersBuilder() {
            parameters = new CameraImpParameters();
        }

        public CameraImpParametersBuilder setPictureSize(Size size) {
            parameters.pictureSize = size;
            return this;
        }

        public CameraImpParametersBuilder setPreviewSize(Size size) {
            parameters.previewSize = size;
            return this;
        }

        public CameraImpParametersBuilder setPreviewFormat(int format) {
            parameters.previewFormat = format;
            return this;
        }

        public CameraImpParametersBuilder setPictureFormat(int format) {
            parameters.pictureFormat = format;
            return this;
        }


        public CameraImpParameters build() {
            if (parameters.pictureSize == null) {
                parameters.pictureSize = new Size(1920, 1080);
            }
            if (parameters.previewSize == null) {
                parameters.previewSize = new Size(1920, 1080);
            }
            return parameters;
        }

        public static CameraImpParametersBuilder create() {
            return new CameraImpParametersBuilder();
        }
    }

    public static class CameraImpParameters {
        public Size pictureSize;
        public Size previewSize;
        public int pictureFormat = ImageFormat.JPEG;
        public int previewFormat = ImageFormat.NV21;
    }

    public static class Size {
        public int width;
        public int height;

        public Size() {
        }

        public Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Size)) {
                return false;
            }
            Size s = (Size) obj;
            return width == s.width && height == s.height;
        }

        @Override
        public int hashCode() {
            return width * 32713 + height;
        }


        public int getHeight() {
            return height;
        }

        public int getWidth() {
            return width;
        }
    }
}