package com.createchance.simplevideoeditor;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_DST_ALPHA;
import static android.opengl.GLES20.GL_SRC_COLOR;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glViewport;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 28/04/2018
 */
public class WatermarkFilter extends NoFilter {
    /**
     * Position of this water mark to put.
     */
    private int mPosX, mPosY;
    /**
     * Target surface's width and height.
     */
    private int mSurfaceWidth, mSurfaceHeight;
    /**
     * Actual width and height of texture.
     */
    private int mTextureWidth, mTextureHeight;

    private Bitmap mBitmap;

    private int mTextureId;

    private WatermarkFilter() {

    }

    @Override
    protected void onCreate() {
        super.onCreate();
        createTexture();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        bindTexture();

        glViewport(mPosX, mPosY, mTextureWidth, mTextureHeight);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_COLOR, GL_DST_ALPHA);
        glDisable(GL_BLEND);
        glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
    }

    @Override
    protected void onClear() {
        super.onClear();
    }

    @Override
    protected void onSurfaceSizeChanged(int width, int height) {
        super.onSurfaceSizeChanged(width, height);
        mSurfaceWidth = width;
        mSurfaceHeight = height;
    }

    private void bindTexture() {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, mTextureId);
    }

    private void createTexture() {
        if (mBitmap != null) {
            int[] textures = new int[1];
            //生成纹理
            GLES20.glGenTextures(1, textures, 0);
            //生成纹理
            glBindTexture(GL_TEXTURE_2D, textures[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, mBitmap, 0);
            //对画面进行矩阵旋转
            OpenGLUtils.flip(OpenGLUtils.getOriginalMatrix(), false, true);

            mTextureId = textures[0];
        }
    }

    public static class Builder {
        private WatermarkFilter filter = new WatermarkFilter();

        public Builder posX(int posX) {
            filter.mPosX = posX;

            return this;
        }

        public Builder posY(int posY) {
            filter.mPosY = posY;

            return this;
        }

        public Builder watermark(Bitmap bitmap) {
            filter.mBitmap = bitmap;
            filter.mTextureWidth = bitmap.getWidth();
            filter.mTextureHeight = bitmap.getHeight();

            return this;
        }

        public WatermarkFilter build() {
            return filter;
        }
    }
}
