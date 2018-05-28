package com.createchance.simplevideoeditor.gles;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.createchance.simplevideoeditor.MatrixUtils;
import com.createchance.simplevideoeditor.R;
import com.createchance.simplevideoeditor.WaterMarkFilter;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 30/04/2018
 */
public class VideoFrameDrawer implements GLSurfaceView.Renderer {

    private static final String TAG = "VideoFrameDrawer";

    /**
     * 用于显示的变换矩阵
     */
    private float[] SM = new float[16];
    private SurfaceTexture surfaceTexture;

    /**
     * Filter draw origin frames of video to texture.
     */
    private OesFilter mOesFilter;
    /**
     * 显示的滤镜
     */
    private NoFilter mShow;

    /**
     * 绘制水印的滤镜
     */
    private WaterMarkFilter mWaterMarkFilter;

    /**
     * 控件的长宽
     */
    private int viewWidth;
    private int viewHeight;

    /**
     * 用于视频旋转的参数
     */
    private int rotation;

    public VideoFrameDrawer(Resources res) {
        mOesFilter = new OesFilter();
        mShow = new NoFilter();
        mWaterMarkFilter = new WaterMarkFilter(res);

        mWaterMarkFilter.setWaterMark(BitmapFactory.decodeResource(res, R.drawable.watermark));

        mWaterMarkFilter.setPosition(0, 70, 0, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int texture = OpenGlUtil.createOneOesTexture();
        surfaceTexture = new SurfaceTexture(texture);
        mOesFilter.setUTextureUnit(texture);
        mWaterMarkFilter.create();
    }

    public void onVideoChanged(VideoInfo info) {
        setRotation(info.rotation);
        if (info.rotation == 0 || info.rotation == 180) {
            MatrixUtils.getShowMatrix(SM, info.width, info.height, viewWidth, viewHeight);
        } else {
            MatrixUtils.getShowMatrix(SM, info.height, info.width, viewWidth, viewHeight);
        }

        mOesFilter.setUMatrix(SM);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        mOesFilter.setViewSize(width, height);

        mWaterMarkFilter.setSize(viewWidth, viewHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        surfaceTexture.updateTexImage();
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        mOesFilter.draw();
        mWaterMarkFilter.setTextureId(mOesFilter.getTextureId());
        mWaterMarkFilter.draw();

        mShow.setUTextureUnit(mWaterMarkFilter.getOutputTexture());
        mShow.draw();
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        if (mOesFilter != null) {
            mOesFilter.setRotation(this.rotation);
        }
    }

    public void checkGlError(String s) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(s + ": glError " + error);
        }
    }
}
