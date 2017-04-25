package com.jake.ffmpegandroid.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Looper;


import com.jake.ffmpegandroid.common.LogUtil;
import com.jake.ffmpegandroid.gpuimage.OpenGlUtils;
import com.jake.ffmpegandroid.gpuimage.Rotation;
import com.jake.ffmpegandroid.gpuimage.TextureRotationUtil;
import com.jake.ffmpegandroid.gpuimage.filter.CameraInputFilter;
import com.jake.ffmpegandroid.gpuimage.filter.GPUImageFilter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * camera 渲染器，主要实现GLSurfaceView.Renderer，渲染摄像头的数据，并实现实时滤镜
 *
 * @author jake
 * @since 2017/3/29 下午2:26
 */
public class CameraRenderer implements GLSurfaceView.Renderer {
    public GLSurfaceView mGLSurfaceView;
    /**
     * 所选择的滤镜，类型为MagicBaseGroupFilter
     * 1.mCameraInputFilter将SurfaceTexture中YUV数据绘制到FrameBuffer
     * 2.mFilter将FrameBuffer中的纹理绘制到屏幕中
     */
    protected GPUImageFilter mFilter;

    /**
     * SurfaceTexure纹理id
     */
    protected int mTextureId = OpenGlUtils.NO_TEXTURE;

    /**
     * 顶点坐标
     */
    protected final FloatBuffer mGLCubeBuffer;

    /**
     * 纹理坐标
     */
    protected final FloatBuffer mGLTextureBuffer;

    /**
     * GLSurfaceView的宽高
     */
    protected int mSurfaceWidth, mSurfaceHeight;

    /**
     * 图像宽高
     */
    protected int mImageWidth, mImageHeight;
    private final CameraInputFilter mCameraInputFilter;

    private SurfaceTexture mSurfaceTexture;
    private OnRendererListener mOnRendererListener;
    private boolean mIsSurfaceCreated = false;

    public boolean isSurfaceCreated() {
        return mIsSurfaceCreated;
    }

    public CameraRenderer(GLSurfaceView glSurfaceView, OnRendererListener listener) {
        mGLSurfaceView = glSurfaceView;
        mOnRendererListener = listener;
        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);

        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(this);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mCameraInputFilter = new CameraInputFilter();
    }

    private void checkIsMainThread() {
        String tag = "当前不是在主线程";
        if (Looper.getMainLooper() == Looper.myLooper()) {
            tag = "当前是在主线程";
        }
        LogUtil.d("CameraRenderer " + tag);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        checkIsMainThread();
        mIsSurfaceCreated = true;
        GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);
        mCameraInputFilter.init();
        if (mTextureId == OpenGlUtils.NO_TEXTURE) {
            mTextureId = OpenGlUtils.getExternalOESTextureID();
            if (mTextureId != OpenGlUtils.NO_TEXTURE) {
                mSurfaceTexture = new SurfaceTexture(mTextureId);
                mSurfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
//                mGLSurfaceView.post(new Runnable() {
//                    @Override
//                    public void run() {
                        if (mOnRendererListener != null) {
                            mOnRendererListener.openRendererCamera();
                        }
//                    }
//                });

            }
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        checkIsMainThread();
        GLES20.glViewport(0, 0, width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        onFilterChanged();
        if (mSurfaceTexture == null)
            return;
        mSurfaceTexture.updateTexImage();
        float[] mtx = new float[16];
        mSurfaceTexture.getTransformMatrix(mtx);
        mCameraInputFilter.setTextureTransformMatrix(mtx);
        int id = mTextureId;
        if (mFilter == null) {
            mCameraInputFilter.onDrawFrame(mTextureId, mGLCubeBuffer, mGLTextureBuffer);
        } else {
//            id = mCameraInputFilter.onDrawToTexture(mTextureId);
            mFilter.onDrawFrame(id, mGLCubeBuffer, mGLTextureBuffer);
        }
    }

    public void updateRotation(int orientation, Camera.Size size, boolean isFacingFront) {
        orientation += 270;
        if (isFacingFront) {
            orientation = (360 - orientation) % 360;
        }
        if (orientation == 90 || orientation == 270) {
            mImageWidth = size.height;
            mImageHeight = size.width;
        } else {
            mImageWidth = size.width;
            mImageHeight = size.height;
        }
        mCameraInputFilter.onInputSizeChanged(mImageWidth, mImageHeight);
        float[] textureCords = TextureRotationUtil.getRotation(Rotation.fromInt(orientation),
                true, false);
        mGLTextureBuffer.clear();
        mGLTextureBuffer.put(textureCords).position(0);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        checkIsMainThread();
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mSurfaceTexture == null) {
            return;
        }
        mSurfaceTexture.updateTexImage();
        float[] mtx = new float[16];
        mSurfaceTexture.getTransformMatrix(mtx);
        mCameraInputFilter.setTextureTransformMatrix(mtx);
        int id = mTextureId;
        if (mFilter == null) {
            mCameraInputFilter.onDrawFrame(mTextureId, mGLCubeBuffer, mGLTextureBuffer);
        } else {
            id = mCameraInputFilter.onDrawToTexture(mTextureId);
            mFilter.onDrawFrame(id, mGLCubeBuffer, mGLTextureBuffer);
        }
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestRender();
        }
    };

    private void onFilterChanged() {
        mCameraInputFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
        if (mFilter != null) {
            mCameraInputFilter.initCameraFrameBuffer(mImageWidth, mImageHeight);
            mFilter.onDisplaySizeChanged(mSurfaceWidth, mSurfaceHeight);
            mFilter.onInputSizeChanged(mImageWidth, mImageHeight);
        } else {
            mCameraInputFilter.destroyFramebuffers();
        }

    }

    /**
     * 设置滤镜
     *
     * @param gpuImageFilter
     */
    public void setFilter(final GPUImageFilter gpuImageFilter) {
        if (mIsSurfaceCreated) {
            mGLSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mFilter != null) {
                        mFilter.destroy();
                    }
                    mFilter = null;
                    mFilter = gpuImageFilter;
                    if (mFilter != null) {
                        mFilter.init();
                    }
                    onFilterChanged();
                }
            });
            requestRender();
        } else {
            mGLSurfaceView.post(new Runnable() {
                @Override
                public void run() {
                    setFilter(gpuImageFilter);
                }
            });
        }
    }

    public GPUImageFilter getFilter() {
        return mFilter;
    }

    public void requestRender() {
        if (mGLSurfaceView != null) {
            mGLSurfaceView.requestRender();
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public static interface OnRendererListener {
        void openRendererCamera();
    }
}
