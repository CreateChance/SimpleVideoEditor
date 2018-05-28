package com.createchance.simplevideoeditor.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 2018/5/19
 */
public class WaterMarkFilter extends NoFilter {

    private static final String TAG = "WaterMarkFilter";

    private int watermarkPosX, watermarkPosY;
    private float scaleFactor;
    private Bitmap watermark;

    private int fTextureSize = 2;
    private int[] fFrame = new int[1];
    private int[] fTexture = new int[fTextureSize];

    WaterMarkFilter(Bitmap watermark, int posX, int posY, float scaleFactor) {
        super();
        this.watermark = watermark;
        this.watermarkPosX = posX;
        this.watermarkPosY = posY;
        this.scaleFactor = scaleFactor;

        int[] textures = new int[1];
        // 生成纹理
        GLES20.glGenTextures(1, textures, 0);
        // 生成纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        // 设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        // 设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // 设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        // 设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, watermark, 0);
        setUTextureUnit(textures[0]);

        createFrameBuffer();
    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        bindFrame();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // set view port to focus water mark.
        GLES20.glViewport(
                watermarkPosX,
                watermarkPosY,
                (int) (watermark.getWidth() / scaleFactor),
                (int) (watermark.getHeight() / scaleFactor)
        );
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        GLES20.glDisable(GLES20.GL_BLEND);
        // reset view port to origin.
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
    }

    @Override
    protected void onPostDraw() {
        super.onPostDraw();
        unBindFrame();
        deleteFrameBuffer();
    }

    private void createFrameBuffer() {
        GLES20.glGenFramebuffers(1, fFrame, 0);
        genTextures();
    }

    private void genTextures() {
        GLES20.glGenTextures(fTextureSize, fTexture, 0);
        for (int i = 0; i < fTextureSize; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fTexture[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, viewWidth, viewHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        }
    }

    private void bindFrame() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fTexture[0], 0);
    }

    private void unBindFrame() {
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    private void deleteFrameBuffer() {
        GLES20.glDeleteFramebuffers(1, fFrame, 0);
        GLES20.glDeleteTextures(1, fTexture, 0);
    }
}
