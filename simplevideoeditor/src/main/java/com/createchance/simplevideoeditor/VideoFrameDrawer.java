package com.createchance.simplevideoeditor;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;

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
     * 用于后台绘制的变换矩阵
     */
    private float[] OM;
    /**
     * 用于显示的变换矩阵
     */
    private float[] SM = new float[16];
    private SurfaceTexture surfaceTexture;
    /**
     * 可选择画面的滤镜
     */
    private RotationOESFilter mPreFilter;
    /**
     * 显示的滤镜
     */
    private AFilter mShow;
    private AFilter mProcessFilter;
    /**
     * 绘制水印的滤镜
     */
    private final GroupFilter mBeFilter;

    /**
     * 控件的长宽
     */
    private int viewWidth;
    private int viewHeight;

    /**
     * 创建离屏buffer
     */
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[1];
    /**
     * 用于视频旋转的参数
     */
    private int rotation;

    public VideoFrameDrawer(Resources res) {
        mPreFilter = new RotationOESFilter(res);//旋转相机操作
        mShow = new NoFilter(res);
        mBeFilter = new GroupFilter(res);

        mProcessFilter = new ProcessFilter(res);

        OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM, false, true);//矩阵上下翻转
//        mShow.setMatrix(OM);

        WaterMarkFilter waterMarkFilter = new WaterMarkFilter(res);
        waterMarkFilter.setWaterMark(BitmapFactory.decodeResource(res, R.drawable.watermark));

        waterMarkFilter.setPosition(0, 70, 0, 0);
        mBeFilter.addFilter(waterMarkFilter);

    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int texture[] = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        Log.d(TAG, "onSurfaceCreated, texture id: " + texture[0] + ", error: " + GLES20.glGetError());
        Log.d(TAG, "onSurfaceCreated, thread: " + Thread.currentThread().getName());
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        surfaceTexture = new SurfaceTexture(texture[0]);
        mPreFilter.create();
        mPreFilter.setTextureId(texture[0]);

        mBeFilter.create();
        mProcessFilter.create();
        mShow.create();
    }

    public void onVideoChanged(VideoInfo info) {
        setRotation(info.rotation);
        if (info.rotation == 0 || info.rotation == 180) {
            MatrixUtils.getShowMatrix(SM, info.width, info.height, viewWidth, viewHeight);
        } else {
            MatrixUtils.getShowMatrix(SM, info.height, info.width, viewWidth, viewHeight);
        }

        mPreFilter.setMatrix(SM);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);

        GLES20.glGenFramebuffers(1, fFrame, 0);
        genTexturesWithParameter(1, fTexture, 0, GLES20.GL_RGBA, viewWidth, viewHeight);

        mBeFilter.setSize(viewWidth, viewHeight);
        mProcessFilter.setSize(viewWidth, viewHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        surfaceTexture.updateTexImage();
        bindFrameTexture(fFrame[0], fTexture[0]);
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        mPreFilter.draw();
        unBindFrameBuffer();

        mBeFilter.setTextureId(fTexture[0]);
        mBeFilter.draw();

        mProcessFilter.setTextureId(mBeFilter.getOutputTexture());
        mProcessFilter.draw();

        mShow.setTextureId(mProcessFilter.getOutputTexture());
        mShow.draw();
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
        if (mPreFilter != null) {
            mPreFilter.setRotation(this.rotation);
        }
    }

    public void checkGlError(String s) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(s + ": glError " + error);
        }
    }

    private void genTexturesWithParameter(int size, int[] textures, int start,
                                          int gl_format, int width, int height) {
        GLES20.glGenTextures(size, textures, start);
        for (int i = 0; i < size; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, gl_format, width, height,
                    0, gl_format, GLES20.GL_UNSIGNED_BYTE, null);
            useTexParameter();
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    private void useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void bindFrameTexture(int frameBufferId, int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    private void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
}
