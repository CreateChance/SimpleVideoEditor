package com.createchance.simplevideoeditor.gles;

import android.content.Context;
import android.opengl.GLES20;

import com.createchance.simplevideoeditor.Config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static android.opengl.GLES20.glUseProgram;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 2018/5/19
 */
abstract class AbstractFilter {
    private static final String TAG = "AbstractFilter";

    protected final int BYTES_PER_FLOAT = 4;

    protected int programId;

    protected Map<String, OpenGlUtil.ShaderParam> shaderParamMap = new HashMap<>();

    AbstractFilter(String vertexSource, String fragmentSource) {
        programId = OpenGlUtil.buildProgram(
                OpenGlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexSource),
                OpenGlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        );
        if (Config.DEBUG && !OpenGlUtil.validateProgram(programId)) {
            throw new IllegalStateException("Program id: " + programId + " invalid!");
        }
        initParamMap();
        OpenGlUtil.getShaderParams(programId, shaderParamMap);
        glUseProgram(programId);
    }

    AbstractFilter(Context context, int vertexSourceRes, int fragmentSourceRes) {
        programId = OpenGlUtil.buildProgram(
                OpenGlUtil.loadShader(context, GLES20.GL_VERTEX_SHADER, vertexSourceRes),
                OpenGlUtil.loadShader(context, GLES20.GL_FRAGMENT_SHADER, fragmentSourceRes)
        );
        if (Config.DEBUG && !OpenGlUtil.validateProgram(programId)) {
            throw new IllegalStateException("Program id: " + programId + " invalid!");
        }
        initParamMap();
        OpenGlUtil.getShaderParams(programId, shaderParamMap);
        glUseProgram(programId);
    }

    AbstractFilter(File vertexSourceFile, File fragmentSourceFile) {
        programId = OpenGlUtil.buildProgram(
                OpenGlUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexSourceFile),
                OpenGlUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSourceFile)
        );
        if (Config.DEBUG && !OpenGlUtil.validateProgram(programId)) {
            throw new IllegalStateException("Program id: " + programId + " invalid!");
        }
        initParamMap();
        OpenGlUtil.getShaderParams(programId, shaderParamMap);
        glUseProgram(programId);
    }

    protected abstract void initParamMap();

    protected abstract void onClear();

    protected abstract void onDraw();

    public void draw() {
        onClear();
        glUseProgram(programId);
        onDraw();
    }
}
