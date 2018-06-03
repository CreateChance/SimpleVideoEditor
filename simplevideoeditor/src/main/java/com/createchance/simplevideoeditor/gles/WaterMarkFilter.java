package com.createchance.simplevideoeditor.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;

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

    public WaterMarkFilter(Bitmap watermark, int posX, int posY, float scaleFactor) {
        super();
        this.watermarkPosX = posX;
        this.watermarkPosY = posY;
        this.watermarkWidth = (int) (watermark.getWidth() * scaleFactor);
        this.watermarkHeight = (int) (watermark.getHeight() * scaleFactor);
        //对画面进行矩阵旋转
//        setUMatrix(OpenGlUtil.flip(OpenGlUtil.getIdentityMatrix(),false,true));
        setUMatrix(OpenGlUtil.getIdentityMatrix());

        setInputTextureId(OpenGlUtil.loadTexture(watermark));
    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        // set view port to focus water mark.
        GLES20.glViewport(
                watermarkPosX,
                watermarkPosY,
                watermarkWidth,
                watermarkHeight
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
