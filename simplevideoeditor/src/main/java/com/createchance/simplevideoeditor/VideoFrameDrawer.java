package com.createchance.simplevideoeditor;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glViewport;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 30/04/2018
 */
public class VideoFrameDrawer implements GLSurfaceView.Renderer {

    private static final String TAG = "VideoFrameDrawer";

    private WatermarkFilter mWatermarkFilter;

    private SurfaceTexture mSurfaceTexture;

    public int mSurfaceWidth, mSurfaceHeight;

    /**
     * 创建离屏buffer
     */
    private int[] mOutScreenFrame = new int[1];
    private int[] mOutScreenTexture = new int[1];

    public VideoFrameDrawer() {
        VideoWatermarkAddAction watermarkAddAction =
                (VideoWatermarkAddAction) EditParamsMap.loadParams(
                        EditParamsMap.KEY_VIDEO_WATER_MARK_ADD_ACTION);
        this.mWatermarkFilter = new WatermarkFilter.Builder()
                .watermark(watermarkAddAction.getWatermark())
                .posX(watermarkAddAction.getXPos())
                .posY(watermarkAddAction.getYPos())
                .build();
    }

    public void setWatermarkFilter(WatermarkFilter filter) {
        this.mWatermarkFilter = filter;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Logger.d(TAG, "onSurfaceCreated");
        int[] textures = new int[1];
        glGenTextures(1, textures, 0);
        glBindTexture(GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        mSurfaceTexture = new SurfaceTexture(textures[0]);
        mWatermarkFilter.create();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        GLES20.glDeleteFramebuffers(1, mOutScreenFrame, 0);
        GLES20.glDeleteTextures(1, mOutScreenTexture, 0);

        GLES20.glGenFramebuffers(1, mOutScreenFrame, 0);
        OpenGLUtils.genTexturesWithParameter(
                1, mOutScreenTexture, 0, GLES20.GL_RGBA, mSurfaceWidth, mSurfaceHeight);

        mWatermarkFilter.setSurfaceSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mSurfaceTexture.updateTexImage();
        glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        mWatermarkFilter.draw();
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }
}
