package com.createchance.simplevideoeditor.gles;

import android.opengl.GLES20;

import com.createchance.simplevideoeditor.Config;

import java.util.HashMap;
import java.util.Map;

import static android.opengl.GLES20.glUseProgram;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 2018/5/19
 */
public abstract class AbstractFilter {
    private static final String TAG = "AbstractFilter";

    protected final int BYTES_PER_FLOAT = 4;

    protected int programId;

    protected Map<String, OpenGlUtil.ShaderParam> shaderParamMap = new HashMap<>();

    protected int surfaceWidth, surfaceHeight;

    protected int inputTextureId;

    private final String vertexSource, fragmentSource;

    AbstractFilter(String vertexSource, String fragmentSource) {
        this.vertexSource = vertexSource;
        this.fragmentSource = fragmentSource;
    }

    protected abstract void initParamMap();

    protected abstract void onDraw();

    public abstract boolean shouldDraw(long presentationTime);

    protected void onInitDone() {

    }

    protected void onPreDraw() {

    }

    protected void onPostDraw() {

    }

    protected void onViewSizeChanged() {

    }

    protected void onSetInputTextureId() {

    }

    final void init() {
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
        onInitDone();
    }

    public final void draw() {
        glUseProgram(programId);
        onPreDraw();
        onDraw();
        onPostDraw();
    }

    public final void setViewSize(int viewWidth, int viewHeight) {
        this.surfaceWidth = viewWidth;
        this.surfaceHeight = viewHeight;
        onViewSizeChanged();
    }

    public final void setInputTextureId(int textureId) {
        this.inputTextureId = textureId;
    }
}
