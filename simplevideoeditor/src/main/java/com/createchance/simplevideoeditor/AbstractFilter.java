package com.createchance.simplevideoeditor;

import android.opengl.GLES20;
import android.util.Log;

import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FALSE;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_NO_ERROR;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.GL_ZERO;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUseProgram;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 28/04/2018
 */
public abstract class AbstractFilter {

    private static final String TAG = AbstractFilter.class.getSimpleName();

    protected int mProgram;

    protected String mVertexShader;

    protected String mFragmentShader;

    public AbstractFilter() {

    }

    public final void create() {
        onCreate();
        if (!createProgram()) {
            onError();
        }
    }

    public final void draw() {
        if (mProgram == GL_ZERO) {
            Log.e(TAG, "draw, can not draw due to program is 0!");
            return;
        }

        clear();
        glUseProgram(mProgram);
        if (assertNoError("glUseProgram")) {
            onDraw();
        } else {
            mProgram = GL_ZERO;
        }
    }

    public final void setSurfaceSize(int width, int height) {
        onSurfaceSizeChanged(width, height);
    }

    private void clear() {
        // clear screen to white default.
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        onClear();
    }

    protected void onCreate() {

    }

    protected void onDraw() {

    }

    protected void onClear() {

    }

    protected void onSurfaceSizeChanged(int width, int height) {

    }

    protected void onError() {

    }

    protected final boolean assertNoError(String op) {
        int error;
        if ((error = glGetError()) != GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            return false;
        }

        return true;
    }

    private boolean createProgram() {
        int vertexShader = loadShader(GL_VERTEX_SHADER, mVertexShader);
        if (vertexShader == GL_ZERO) {
            Logger.e(TAG, "Vertex shader load error.");
            return false;
        }

        int fragmentShader = loadShader(GL_FRAGMENT_SHADER, mFragmentShader);
        if (fragmentShader == GL_ZERO) {
            Logger.e(TAG, "Fragment shader load error.");
            return false;
        }

        int program = glCreateProgram();
        if (program == GL_ZERO) {
            Logger.e(TAG, "Program create error.");
            return false;
        }

        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        int[] linkStatus = new int[1];
        glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == GL_FALSE) {
            Log.e(TAG, "createProgram, fail to link program, error log: " +
                    glGetProgramInfoLog(program));
            glDeleteProgram(program);
            return false;
        }

        mProgram = program;

        Logger.d(TAG, "Program create done.");

        return true;
    }

    private int loadShader(int shaderType, String source) {
        int shader = glCreateShader(shaderType);
        if (shader != 0) {
            glShaderSource(shader, source);
            glCompileShader(shader);
            int[] compiled = new int[1];
            glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Logger.e(TAG, "Could not compile shader:" + shaderType
                        + "GLES20 Error:" + glGetShaderInfoLog(shader));
                glDeleteShader(shader);
                shader = 0;
            }
        } else {
            Logger.e(TAG, "Shader type: " + shaderType + " create error. GL error: " + glGetError());
        }
        return shader;
    }
}
