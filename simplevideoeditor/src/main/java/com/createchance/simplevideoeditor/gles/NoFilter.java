package com.createchance.simplevideoeditor.gles;

import android.opengl.GLES20;

import java.nio.FloatBuffer;

import static android.opengl.GLES10.glViewport;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;
import static com.createchance.simplevideoeditor.gles.OpenGlUtil.ShaderParam.TYPE_ATTRIBUTE;
import static com.createchance.simplevideoeditor.gles.OpenGlUtil.ShaderParam.TYPE_UNIFORM;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 2018/5/19
 */
public class NoFilter extends AbstractFilter {
    // Uniform
    private final String U_MATRIX = "u_Matrix";
    private final String U_TEXTURE_UNIT = "u_TextureUnit";

    // Attribute
    private final String A_POSITION = "a_Position";
    private final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    private FloatBuffer vertexPositionBuffer;
    private FloatBuffer textureCoordinateBuffer;

    NoFilter() {
        super(
                // vertex shader with matrix to change position.
                Shaders.BASE_VERTEX_SHADER,
                // fragment shader does nothing.
                Shaders.BASE_FRAGMENT_SHADER
        );
    }

    @Override
    protected void initParamMap() {
        shaderParamMap.clear();
        shaderParamMap.put(
                A_POSITION,
                new OpenGlUtil.ShaderParam(TYPE_ATTRIBUTE, A_POSITION)
        );
        shaderParamMap.put(
                A_TEXTURE_COORDINATES,
                new OpenGlUtil.ShaderParam(TYPE_ATTRIBUTE, A_TEXTURE_COORDINATES)
        );
        shaderParamMap.put(
                U_MATRIX,
                new OpenGlUtil.ShaderParam(TYPE_UNIFORM, U_MATRIX)
        );
        shaderParamMap.put(
                U_TEXTURE_UNIT,
                new OpenGlUtil.ShaderParam(TYPE_UNIFORM, U_TEXTURE_UNIT)
        );
    }

    @Override
    protected void onInitDone() {
        super.onInitDone();
        vertexPositionBuffer = OpenGlUtil.getFloatBuffer(
                new float[]{
                        -1.0f, 1.0f,
                        -1.0f, -1.0f,
                        1.0f, 1.0f,
                        1.0f, -1.0f,
                },
                BYTES_PER_FLOAT
        );
        textureCoordinateBuffer = OpenGlUtil.getFloatBuffer(
                new float[]{
                        0.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 0.0f,
                        1.0f, 1.0f,
                },
                BYTES_PER_FLOAT
        );

        // set matrix
        setUMatrix(OpenGlUtil.getIdentityMatrix());
    }

    @Override
    protected void onPreDraw() {
        glViewport(0, 0, surfaceWidth, surfaceHeight);
        // bind texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, inputTextureId);
        glUniform1i(shaderParamMap.get(U_TEXTURE_UNIT).location, 0);
    }

    @Override
    protected void onDraw() {
        glEnableVertexAttribArray(shaderParamMap.get(A_POSITION).location);
        vertexPositionBuffer.position(0);
        glVertexAttribPointer(
                shaderParamMap.get(A_POSITION).location,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexPositionBuffer);
        glEnableVertexAttribArray(shaderParamMap.get(A_TEXTURE_COORDINATES).location);
        textureCoordinateBuffer.position(0);
        glVertexAttribPointer(
                shaderParamMap.get(A_TEXTURE_COORDINATES).location,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                textureCoordinateBuffer);
        glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        glDisableVertexAttribArray(shaderParamMap.get(A_POSITION).location);
        glDisableVertexAttribArray(shaderParamMap.get(A_TEXTURE_COORDINATES).location);
    }

    @Override
    public boolean shouldDraw(long presentationTimeUs) {
        // we should draw all the time for no filter.
        return true;
    }

    public void setUMatrix(float[] matrix) {
        glUniformMatrix4fv(shaderParamMap.get(U_MATRIX).location, 1, false, matrix, 0);
    }
}
