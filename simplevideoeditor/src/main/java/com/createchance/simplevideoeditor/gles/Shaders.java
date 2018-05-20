package com.createchance.simplevideoeditor.gles;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 2018/5/19
 */
class Shaders {
    static final String BASE_VERTEX_SHADER =
            "uniform mat4 u_Matrix;\n" +
                    "\n" +
                    "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_TextureCoordinates;\n" +
                    "\n" +
                    "varying vec2 v_TextureCoordinates;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    v_TextureCoordinates = a_TextureCoordinates;\n" +
                    "    gl_Position = u_Matrix * a_Position;\n" +
                    "}\n";

    static final String BASE_FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "\n" +
                    "uniform sampler2D u_TextureUnit;\n" +
                    "varying vec2 v_TextureCoordinates;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates);\n" +
                    "}\n";

    static final String BASE_OES_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_TextureCoordinates;\n" +
                    "uniform mat4 u_Matrix;\n" +
                    "varying vec2 v_TextureCoordinates;\n" +
                    "\n" +
                    "void main(){\n" +
                    "    gl_Position = u_Matrix * a_Position;\n" +
                    "    textureCoordinate = v_TextureCoordinates;\n" +
                    "}";

    static final String BASE_OES_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 v_TextureCoordinates;\n" +
                    "uniform samplerExternalOES u_TextureUnit;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D( u_TextureUnit, v_TextureCoordinates );\n" +
                    "}";
}
