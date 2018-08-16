package com.createchance.simplevideoeditor.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.createchance.simplevideoeditor.Logger;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/5/19
 */
public class WaterMarkFilter extends NoFilter {

    private static final String TAG = "WaterMarkFilter";

    private int mWatermarkPosX, mWatermarkPosY;
    private int mWatermarkWidth, mWatermarkHeight;
    private Bitmap mWatermark;
    private float mScaleFactor = 1.0f;
    private long mStartPosMs, mDurationMs;

    private WaterMarkFilter() {

    }

    @Override
    protected void onInitDone() {
        super.onInitDone();
        //对画面进行矩阵旋转
        setUMatrix(OpenGlUtil.getIdentityMatrix());

        setInputTextureId(OpenGlUtil.loadTexture(mWatermark));
    }

    @Override
    public boolean shouldDraw(long presentationTimeUs) {
        if (mStartPosMs == 0 && mDurationMs == 0) {
            return super.shouldDraw(presentationTimeUs);
        }

        return presentationTimeUs >= mStartPosMs * 1000 &&
                presentationTimeUs <= (mStartPosMs + mDurationMs) * 1000;

    }

    @Override
    protected void onPreDraw() {
        super.onPreDraw();
        // set view port to focus water mark.
        GLES20.glViewport(
                mWatermarkPosX,
                mWatermarkPosY,
                (int) (mWatermarkWidth * mScaleFactor),
                (int) (mWatermarkHeight * mScaleFactor)
        );
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
    }

    @Override
    protected void onPostDraw() {
        super.onPostDraw();
        GLES20.glDisable(GLES20.GL_BLEND);
        // reset view port to origin.
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
    }

    public boolean checkRational(long videoDuration) {
        if (mWatermark == null || mWatermark.isRecycled()) {
            Logger.e(TAG, "Water mark is null or recycled! Your watermark: " + mWatermark);
            return false;
        }

        if (mWatermarkPosX < 0 || mWatermarkPosY < 0) {
            Logger.e(TAG, "Water mark position must >= 0! Your position, x: " +
                    mWatermarkPosY +
                    ", y: " +
                    mWatermarkPosY);
            return false;
        }

        if (mScaleFactor < 0) {
            Logger.e(TAG, "Scale factor must >= 0! Your scale factor: " + mScaleFactor);
            return false;
        }

        if (mStartPosMs < 0 || mDurationMs < 0) {
            Logger.e(TAG, "Start pos and duration must >= 0! Your start pos: " +
                    mStartPosMs +
                    ", duration: " +
                    mDurationMs);
            return false;
        }

        if (mStartPosMs == 0 && mDurationMs > 0) {
            if (mDurationMs > videoDuration) {
                Logger.e(TAG, "Start pos is 0, but duration is out of video duration: " +
                        videoDuration +
                        ", your duration: " +
                        mDurationMs);
                return false;
            }
        }

        if (mStartPosMs > 0 && mDurationMs == 0) {
            if (mStartPosMs >= videoDuration) {
                Logger.e(TAG, "Duration is 0, but start is out of video duration: " +
                        videoDuration +
                        ", your start pos: " +
                        mStartPosMs);
                return false;
            }

            // adjust mDuration to rest section.
            mDurationMs = videoDuration - mStartPosMs;
            Logger.w(TAG, "Your start pos is > 0, but duration is 0, so we adjust duration to : " + mDurationMs);
        }

        return true;
    }

    public static class Builder {
        private WaterMarkFilter waterMarkFilter = new WaterMarkFilter();

        public Builder watermark(Bitmap watermark) {
            waterMarkFilter.mWatermark = watermark;
            waterMarkFilter.mWatermarkWidth = watermark.getWidth();
            waterMarkFilter.mWatermarkHeight = watermark.getHeight();

            return this;
        }

        public Builder position(int posX, int posY) {
            waterMarkFilter.mWatermarkPosX = posX;
            waterMarkFilter.mWatermarkPosY = posY;

            return this;
        }

        public Builder scaleFactor(float factor) {
            waterMarkFilter.mScaleFactor = factor;

            return this;
        }

        public Builder startFrom(long startMs) {
            waterMarkFilter.mStartPosMs = startMs;

            return this;
        }

        public Builder duration(long durationMs) {
            waterMarkFilter.mDurationMs = durationMs;

            return this;
        }

        public WaterMarkFilter build() {
            return waterMarkFilter;
        }
    }
}
