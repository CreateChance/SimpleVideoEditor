package com.createchance.simplevideoeditor.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import com.createchance.simplevideoeditor.Logger;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static com.createchance.simplevideoeditor.gles.OpenGlUtil.ShaderParam.TYPE_UNIFORM;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 2018/6/3
 */
public class VideoFrameLookupFilter extends AbstractFilter {

    private static final String TAG = "VideoFrameLookupFilter";

    // Attribute
    private final String A_POSITION = "a_Position";
    private final String A_TEXTURE_COORDINATES = "a_InputTextureCoordinate";

    // Uniform
    private final String U_MATRIX = "u_Matrix";
    private final String U_INPUT_IMAGE_TEXTURE = "u_InputImageTexture";
    private final String U_CURVE = "u_Curve";
    private final String U_STRENGTH = "u_Strength";

    private FloatBuffer mVertexPositionBuffer;
    private FloatBuffer mTextureCoordinateBuffer;

    private Bitmap mCurve;
    private float mStrength = 1.0f;

    private int mCurveTextureId;

    private long mStartPosMs, mDurationMs;

    private VideoFrameLookupFilter() {
        super(Shaders.BASE_VIDEO_FRAME_LOOKUP_VERTEX_SHADER,
                Shaders.BASE_VIDEO_FRAME_LOOKUP_FRAGMENT_SHADER);
    }

    @Override
    protected void initParamMap() {
        shaderParamMap.put(A_POSITION, new OpenGlUtil.ShaderParam(
                OpenGlUtil.ShaderParam.TYPE_ATTRIBUTE,
                A_POSITION
        ));
        shaderParamMap.put(A_TEXTURE_COORDINATES, new OpenGlUtil.ShaderParam(
                OpenGlUtil.ShaderParam.TYPE_ATTRIBUTE,
                A_TEXTURE_COORDINATES
        ));
        shaderParamMap.put(U_MATRIX, new OpenGlUtil.ShaderParam(
                TYPE_UNIFORM,
                U_MATRIX)
        );
        shaderParamMap.put(U_INPUT_IMAGE_TEXTURE, new OpenGlUtil.ShaderParam(
                OpenGlUtil.ShaderParam.TYPE_UNIFORM,
                U_INPUT_IMAGE_TEXTURE
        ));
        shaderParamMap.put(U_CURVE, new OpenGlUtil.ShaderParam(
                OpenGlUtil.ShaderParam.TYPE_UNIFORM,
                U_CURVE
        ));
        shaderParamMap.put(U_STRENGTH, new OpenGlUtil.ShaderParam(
                OpenGlUtil.ShaderParam.TYPE_UNIFORM,
                U_STRENGTH
        ));
    }

    @Override
    protected void onInitDone() {
        super.onInitDone();

        mVertexPositionBuffer = OpenGlUtil.getFloatBuffer(
                new float[]{
                        -1.0f, 1.0f,
                        -1.0f, -1.0f,
                        1.0f, 1.0f,
                        1.0f, -1.0f,
                },
                BYTES_PER_FLOAT
        );
        // default rotation is 0 degree
        mTextureCoordinateBuffer = OpenGlUtil.getFloatBuffer(
                new float[]{
                        0.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 0.0f,
                        1.0f, 1.0f,
                },
                BYTES_PER_FLOAT
        );

        // set uniform var
        glUniformMatrix4fv(
                shaderParamMap.get(U_MATRIX).location,
                1,
                false,
                OpenGlUtil.flip(OpenGlUtil.getIdentityMatrix(), false, true),
                0);
        glUniform1f(shaderParamMap.get(U_STRENGTH).location, mStrength);
        glActiveTexture(GL_TEXTURE1);
        mCurveTextureId = OpenGlUtil.loadTexture(mCurve);
        glBindTexture(GL_TEXTURE_2D, mCurveTextureId);
        glUniform1i(shaderParamMap.get(U_CURVE).location, 1);
    }

    @Override
    protected void onDraw() {
        glViewport(0, 0, surfaceWidth, surfaceHeight);
        glEnableVertexAttribArray(shaderParamMap.get(A_POSITION).location);
        mVertexPositionBuffer.position(0);
        glVertexAttribPointer(
                shaderParamMap.get(A_POSITION).location,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * BYTES_PER_FLOAT,
                mVertexPositionBuffer);
        glEnableVertexAttribArray(shaderParamMap.get(A_TEXTURE_COORDINATES).location);
        mTextureCoordinateBuffer.position(0);
        glVertexAttribPointer(
                shaderParamMap.get(A_TEXTURE_COORDINATES).location,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * BYTES_PER_FLOAT,
                mTextureCoordinateBuffer);

        // bind textures
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTextureId);
        glUniform1i(shaderParamMap.get(U_INPUT_IMAGE_TEXTURE).location, 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, mCurveTextureId);
        glUniform1i(shaderParamMap.get(U_CURVE).location, 1);

        glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        glDisableVertexAttribArray(shaderParamMap.get(A_POSITION).location);
        glDisableVertexAttribArray(shaderParamMap.get(A_TEXTURE_COORDINATES).location);
    }

    @Override
    public boolean shouldDraw(long presentationTimeUs) {
        if (mStartPosMs == 0 && mDurationMs == 0) {
            return true;
        }

        return presentationTimeUs >= mStartPosMs * 1000 &&
                presentationTimeUs <= (mStartPosMs + mDurationMs) * 1000;
    }

    @Override
    protected void onSetInputTextureId() {
        super.onSetInputTextureId();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTextureId);
        glUniform1i(shaderParamMap.get(U_INPUT_IMAGE_TEXTURE).location, 0);
    }

    public boolean checkRational(long videoDuration) {
        if (mCurve == null || mCurve.isRecycled()) {
            Logger.e(TAG, "Curve is null or recycled! Curve: " + mCurve);
            return false;
        }

        if (mStrength < 0 || mStrength > 1) {
            Logger.e(TAG, "Strength must be  0f <= strength <= 1f, your strength: " + mStrength);
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
        private VideoFrameLookupFilter lookupFilter = new VideoFrameLookupFilter();

        public Builder curve(Bitmap curve) {
            lookupFilter.mCurve = curve;

            return this;
        }

        public Builder strength(float strength) {
            lookupFilter.mStrength = strength;

            return this;
        }

        public Builder startFrom(long startPosMs) {
            lookupFilter.mStartPosMs = startPosMs;

            return this;
        }

        public Builder duration(long durationMs) {
            lookupFilter.mDurationMs = durationMs;

            return this;
        }

        public VideoFrameLookupFilter build() {
            return lookupFilter;
        }
    }
}
