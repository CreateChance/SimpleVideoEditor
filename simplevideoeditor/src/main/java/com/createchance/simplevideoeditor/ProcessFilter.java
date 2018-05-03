package com.createchance.simplevideoeditor;

import android.content.res.Resources;
import android.opengl.GLES20;


/**
 * draw并不执行父类的draw方法,所以矩阵对它无效
 * Description:
 */
public class ProcessFilter extends AFilter {

    private AFilter mFilter;
    //创建离屏buffer
    private int[] fFrame = new int[1];
    private int[] fRender = new int[1];
    private int[] fTexture = new int[1];

    private int width;
    private int height;


    public ProcessFilter(Resources mRes) {
        super(mRes);
        mFilter = new NoFilter(mRes);
        float[] OM = MatrixUtils.getOriginalMatrix();
        MatrixUtils.flip(OM, false, true);//矩阵上下翻转
        mFilter.setMatrix(OM);
    }

    @Override
    protected void initBuffer() {

    }

    @Override
    protected void onCreate() {
        mFilter.create();
    }

    @Override
    public int getOutputTexture() {
        return fTexture[0];
    }

    @Override
    public void draw() {
        boolean b = GLES20.glIsEnabled(GLES20.GL_CULL_FACE);
        if (b) {
            GLES20.glDisable(GLES20.GL_CULL_FACE);
        }
        GLES20.glViewport(0, 0, width, height);
        bindFrameTexture(fFrame[0], fTexture[0]);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);
        mFilter.setTextureId(getTextureId());
        mFilter.draw();
        unBindFrameBuffer();
        if (b) {
            GLES20.glEnable(GLES20.GL_CULL_FACE);
        }
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        if (this.width != width && this.height != height) {
            this.width = width;
            this.height = height;
            mFilter.setSize(width, height);
            deleteFrameBuffer();
            GLES20.glGenFramebuffers(1, fFrame, 0);
            GLES20.glGenRenderbuffers(1, fRender, 0);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, fRender[0]);
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
                    width, height);
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                    GLES20.GL_RENDERBUFFER, fRender[0]);
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
            genTexturesWithParameter(1, fTexture, 0, GLES20.GL_RGBA, width, height);
        }
    }

    private void deleteFrameBuffer() {
        GLES20.glDeleteRenderbuffers(1, fRender, 0);
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);
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
