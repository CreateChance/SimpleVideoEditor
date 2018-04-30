package com.createchance.simplevideoeditor;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 28/04/2018
 */
public class NoFilter extends AbstractFilter {

    @Override
    protected void onCreate() {
        super.onCreate();

        mVertexShader = "attribute vec4 vPosition;\n" +
                "attribute vec2 vCoord;\n" +
                "uniform mat4 vMatrix;\n" +
                "\n" +
                "varying vec2 textureCoordinate;\n" +
                "\n" +
                "void main(){\n" +
                "    gl_Position = vMatrix*vPosition;\n" +
                "    textureCoordinate = vCoord;\n" +
                "}\n";
        mFragmentShader = "precision mediump float;\n" +
                "varying vec2 textureCoordinate;\n" +
                "uniform sampler2D vTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D( vTexture, textureCoordinate );\n" +
                "}\n";
    }

    @Override
    protected void onDraw() {
        super.onDraw();
    }

    @Override
    protected void onClear() {
        super.onClear();

        // clear screen to black
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
}
