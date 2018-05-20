package com.createchance.simplevideoeditor.gles;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
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
public class OesFilter extends AbstractFilter {
    // Uniform
    private final String U_MATRIX = "u_Matrix";
    private final String U_TEXTURE_UNIT = "u_TextureUnit";

    // Attribute
    private final String A_POSITION = "a_Position";
    private final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";

    private FloatBuffer vertexPositionBuffer;
    private FloatBuffer textureCoordinateBuffer;

    private int textureId;

    OesFilter() {
        super(
                Shaders.BASE_OES_VERTEX_SHADER,
                Shaders.BASE_OES_FRAGMENT_SHADER
        );

        vertexPositionBuffer = OpenGlUtil.getFloatBuffer(
                new float[]{
                        -1.0f, 1.0f,
                        -1.0f, -1.0f,
                        1.0f, 1.0f,
                        1.0f, -1.0f,
                },
                BYTES_PER_FLOAT
        );
        // default rotation is 0 degree
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
    protected void onClear() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    @Override
    protected void onDraw() {
        glEnableVertexAttribArray(shaderParamMap.get(A_POSITION).location);
        glVertexAttribPointer(
                shaderParamMap.get(A_POSITION).location,
                2,
                GLES20.GL_FLOAT,
                false,
                0,
                vertexPositionBuffer);
        glEnableVertexAttribArray(shaderParamMap.get(A_TEXTURE_COORDINATES).location);
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

    public void setUMatrix(float[] matrix) {
        glUniformMatrix4fv(shaderParamMap.get(U_MATRIX).location, 1, false, matrix, 0);
    }

    public void setUTextureUnit(int textureId) {
        this.textureId = textureId;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glUniform1i(shaderParamMap.get(U_TEXTURE_UNIT).location, 0);
    }

    public void setRotation(int rotation) {
        float[] coord;
        switch (rotation) {
            case 0:
                coord = new float[]{
                        0.0f, 0.0f,
                        0.0f, 1.0f,
                        1.0f, 0.0f,
                        1.0f, 1.0f,
                };
                break;
            case 90:
                coord = new float[]{
                        0.0f, 1.0f,
                        1.0f, 1.0f,
                        0.0f, 0.0f,
                        1.0f, 0.0f
                };
                break;
            case 180:
                coord = new float[]{
                        1.0f, 1.0f,
                        1.0f, 0.0f,
                        0.0f, 1.0f,
                        0.0f, 0.0f,
                };
                break;
            case 270:
                coord = new float[]{
                        1.0f, 0.0f,
                        0.0f, 0.0f,
                        1.0f, 1.0f,
                        0.0f, 1.0f
                };
                break;
            default:
                return;
        }
        textureCoordinateBuffer.clear();
        textureCoordinateBuffer.put(coord);
        textureCoordinateBuffer.position(0);
    }
}
