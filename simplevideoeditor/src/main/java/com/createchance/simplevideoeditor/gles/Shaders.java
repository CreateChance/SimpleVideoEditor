package com.createchance.simplevideoeditor.gles;

/**
 * ${DESC}
 *
 * @author createchance
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
                    "    v_TextureCoordinates = a_TextureCoordinates;\n" +
                    "}";

    static final String BASE_OES_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 v_TextureCoordinates;\n" +
                    "uniform samplerExternalOES u_TextureUnit;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D( u_TextureUnit, v_TextureCoordinates );\n" +
                    "}";

    static final String BASE_VIDEO_FRAME_ARRAY_FRAGMENT_SHADER =
            "varying highp vec2 v_TextureCoordinate;\n" +
                    "\n" +
                    "precision highp float;\n" +
                    "\n" +
                    "uniform sampler2D u_InputImageTexture;\n" +
                    "uniform sampler2D u_Curve;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "   highp vec4 textureColor;\n" +
                    "   highp vec4 textureColorRes;\n" +
                    "   highp float satVal = 65.0 / 100.0;\n" +
                    "\n" +
                    "   float xCoordinate = v_TextureCoordinate.x;\n" +
                    "   float yCoordinate = v_TextureCoordinate.y;\n" +
                    "\n" +
                    "   highp float redCurveValue;\n" +
                    "   highp float greenCurveValue;\n" +
                    "   highp float blueCurveValue;\n" +
                    "\n" +
                    "   textureColor = texture2D( u_InputImageTexture, vec2(xCoordinate, yCoordinate));\n" +
                    "   textureColorRes = textureColor;\n" +
                    "\n" +
                    "   redCurveValue = texture2D(u_Curve, vec2(textureColor.r, 0.0)).r; \n" +
                    "   greenCurveValue = texture2D(u_Curve, vec2(textureColor.g, 0.0)).g;\n" +
                    "   blueCurveValue = texture2D(u_Curve, vec2(textureColor.b, 0.0)).b;\n" +
                    "\n" +
                    "   highp float G = (redCurveValue + greenCurveValue + blueCurveValue);\n" +
                    "   G = G / 3.0;\n" +
                    "\n" +
                    "   redCurveValue = ((1.0 - satVal) * G + satVal * redCurveValue);\n" +
                    "   greenCurveValue = ((1.0 - satVal) * G + satVal * greenCurveValue);\n" +
                    "   blueCurveValue = ((1.0 - satVal) * G + satVal * blueCurveValue);\n" +
                    "   redCurveValue = (((redCurveValue) > (1.0)) ? (1.0) : (((redCurveValue) < (0.0)) ? (0.0) : (redCurveValue)));\n" +
                    "   greenCurveValue = (((greenCurveValue) > (1.0)) ? (1.0) : (((greenCurveValue) < (0.0)) ? (0.0) : (greenCurveValue)));\n" +
                    "   blueCurveValue = (((blueCurveValue) > (1.0)) ? (1.0) : (((blueCurveValue) < (0.0)) ? (0.0) : (blueCurveValue)));\n" +
                    "\n" +
                    "   redCurveValue = texture2D(u_Curve, vec2(redCurveValue, 0.0)).a;\n" +
                    "   greenCurveValue = texture2D(u_Curve, vec2(greenCurveValue, 0.0)).a;\n" +
                    "   blueCurveValue = texture2D(u_Curve, vec2(blueCurveValue, 0.0)).a; \n" +
                    "\n" +
                    "   highp vec4 base = vec4(redCurveValue, greenCurveValue, blueCurveValue, 1.0);\n" +
                    "   highp vec4 overlayer = vec4(250.0/255.0, 227.0/255.0, 193.0/255.0, 1.0);\n" +
                    "\n" +
                    "   textureColor = overlayer * base;\n" +
                    "   base = (textureColor - base) * 0.850980 + base;\n" +
                    "   textureColor = base; \n" +
                    "\n" +
                    "   gl_FragColor = vec4(textureColor.r, textureColor.g, textureColor.b, 1.0);\n" +
                    "}";

    static final String BASE_VIDEO_FRAME_LOOKUP_VERTEX_SHADER =
            "attribute vec4 a_Position;\n" +
                    "attribute vec2 a_InputTextureCoordinate;\n" +
                    " \n" +
                    "varying vec2 v_TextureCoordinate;\n" +
                    " \n" +
                    "uniform mat4 u_Matrix;\n" +
                    "void main()\n" +
                    "{\n" +
                    "    gl_Position = u_Matrix * a_Position;\n" +
                    "    v_TextureCoordinate = a_InputTextureCoordinate;\n" +
                    "}";

    static final String BASE_VIDEO_FRAME_LOOKUP_FRAGMENT_SHADER =
            "varying mediump vec2 v_TextureCoordinate;\n" +
                    " \n" +
                    " uniform sampler2D u_InputImageTexture;\n" +
                    " uniform sampler2D u_Curve; // lookup texture\n" +
                    " \n" +
                    " uniform lowp float u_Strength;\n" +
                    " \n" +
                    " void main()\n" +
                    " {\n" +
                    "     highp vec4 textureColor = texture2D(u_InputImageTexture, v_TextureCoordinate);\n" +
                    "     \n" +
                    "     highp float blueColor = textureColor.b * 63.0;\n" +
                    "     \n" +
                    "     highp vec2 quad1;\n" +
                    "     quad1.y = floor(floor(blueColor) / 8.0);\n" +
                    "     quad1.x = floor(blueColor) - (quad1.y * 8.0);\n" +
                    "     \n" +
                    "     highp vec2 quad2;\n" +
                    "     quad2.y = floor(ceil(blueColor) / 8.0);\n" +
                    "     quad2.x = ceil(blueColor) - (quad2.y * 8.0);\n" +
                    "     \n" +
                    "     highp vec2 texPos1;\n" +
                    "     texPos1.x = (quad1.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
                    "     texPos1.y = (quad1.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
                    "     \n" +
                    "     highp vec2 texPos2;\n" +
                    "     texPos2.x = (quad2.x * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.r);\n" +
                    "     texPos2.y = (quad2.y * 0.125) + 0.5/512.0 + ((0.125 - 1.0/512.0) * textureColor.g);\n" +
                    "     \n" +
                    "     lowp vec4 newColor1 = texture2D(u_Curve, texPos1);\n" +
                    "     lowp vec4 newColor2 = texture2D(u_Curve, texPos2);\n" +
                    "     \n" +
                    "     lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));\n" +
                    "     gl_FragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), u_Strength);\n" +
                    " }";
}
