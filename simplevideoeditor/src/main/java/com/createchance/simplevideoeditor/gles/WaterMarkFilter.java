package com.createchance.simplevideoeditor.gles;

import android.graphics.Bitmap;
import android.graphics.Path;
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

    public WaterMarkFilter(Bitmap watermark, int posX, int posY, float scaleFactor) {
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
        //对画面进行矩阵旋转
//        setUMatrix(OpenGlUtil.flip(OpenGlUtil.getIdentityMatrix(),false,true));
        setUMatrix(OpenGlUtil.getIdentityMatrix());

        setInputTextureId(textures[0]);
    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        // set view port to focus water mark.
        GLES20.glViewport(
                watermarkPosX,
                watermarkPosY,
                (int) (watermark.getWidth() * scaleFactor),
                (int) (watermark.getHeight() * scaleFactor)
        );
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
    }

    @Override
    protected void onPostDraw() {
        super.onPostDraw();
        GLES20.glDisable(GLES20.GL_BLEND);
        // reset view port to origin.
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
    }
}
