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
    private int watermarkWidth, watermarkHeight;
    private int viewWidth, viewHeight;

    private Bitmap watermark;

    WaterMarkFilter(Bitmap watermark) {
        super();
        this.watermark = watermark;

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
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // set view port to focus water mark.
        GLES20.glViewport(
                watermarkPosX,
                watermarkPosY,
                watermarkWidth == 0 ? watermark.getWidth() : watermarkWidth,
                watermarkHeight == 0 ? watermark.getHeight() : watermarkHeight
        );
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        GLES20.glDisable(GLES20.GL_BLEND);
        // reset view port to origin.
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
    }
}
