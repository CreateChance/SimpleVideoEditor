package com.createchance.simplevideoeditor.gles;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE3;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 2018/6/3
 */
public class VideoFrameLookupFilter extends AbstractFilter {

    // Attribute
    private final String A_POSITION = "a_Position";
    private final String A_TEXTURE_COORDINATES = "a_InputTextureCoordinate";

    // Uniform
    private final String U_INPUT_IMAGE_TEXTURE = "u_InputImageTexture";
    private final String U_CURVE = "u_Curve";
    private final String U_STRENGTH = "u_Strength";

    private FloatBuffer vertexPositionBuffer;
    private FloatBuffer textureCoordinateBuffer;

    private Bitmap curve;
    private float strength;

    public VideoFrameLookupFilter(Bitmap curve, float strength) {
        super(Shaders.BASE_VIDEO_FRAME_LOOKUP_VERTEX_SHADER,
                Shaders.BASE_VIDEO_FRAME_LOOKUP_FRAGMENT_SHADER);
        this.curve = curve;
        this.strength = strength;
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

        vertexPositionBuffer = OpenGlUtil.getFloatBuffer(
                new float[]{
                        -1.0f, -1.0f,
                        1.0f, -1.0f,
                        -1.0f, 1.0f,
                        1.0f, 1.0f,
                },
                BYTES_PER_FLOAT
        );
        // default rotation is 0 degree
        textureCoordinateBuffer = OpenGlUtil.getFloatBuffer(
                new float[]{
                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f,
                },
                BYTES_PER_FLOAT
        );

        // set uniform var
        glUniform1f(shaderParamMap.get(U_STRENGTH).location, strength);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, OpenGlUtil.loadTexture(curve));
        glUniform1i(shaderParamMap.get(U_CURVE).location, 3);
    }

    @Override
    protected void onDraw() {
        glViewport(0, 0, surfaceWidth, surfaceHeight);
        glEnableVertexAttribArray(shaderParamMap.get(A_POSITION).location);
        vertexPositionBuffer.position(0);
        glVertexAttribPointer(
                shaderParamMap.get(A_POSITION).location,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * BYTES_PER_FLOAT,
                vertexPositionBuffer);
        glEnableVertexAttribArray(shaderParamMap.get(A_TEXTURE_COORDINATES).location);
        textureCoordinateBuffer.position(0);
        glVertexAttribPointer(
                shaderParamMap.get(A_TEXTURE_COORDINATES).location,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * BYTES_PER_FLOAT,
                textureCoordinateBuffer);
        glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        glDisableVertexAttribArray(shaderParamMap.get(A_POSITION).location);
        glDisableVertexAttribArray(shaderParamMap.get(A_TEXTURE_COORDINATES).location);
    }

    @Override
    protected void onSetInputTextureId() {
        super.onSetInputTextureId();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTextureId);
        glUniform1i(shaderParamMap.get(U_INPUT_IMAGE_TEXTURE).location, 0);
    }
}
