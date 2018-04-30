package com.createchance.simplevideoeditor;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ${DESC}
 *
 * @author gaochao1-iri
 * @date 28/04/2018
 */
public class OpenGLUtils {
    public String readGlslFromFile(File file) throws IOException {
        StringBuilder text = new StringBuilder();
        InputStream inputStream = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            text.append(line);
            text.append('\n');
        }

        return text.toString();
    }

    public String readGlslFromResource(Context context, int resId) throws IOException {
        StringBuilder text = new StringBuilder();

        InputStream inputStream = context.getResources().openRawResource(resId);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            text.append(line);
            text.append('\n');
        }

        return text.toString();
    }

    public static float[] flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m, 0, x ? -1 : 1, y ? -1 : 1, 1);
        }
        return m;
    }

    public static float[] getOriginalMatrix() {
        return new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
    }

    public static void genTexturesWithParameter(int size, int[] textures, int start,
                                                int glFormat, int width, int height) {
        GLES20.glGenTextures(size, textures, start);
        for (int i = 0; i < size; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, glFormat, width, height,
                    0, glFormat, GLES20.GL_UNSIGNED_BYTE, null);
            useTexParameter();
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public static void useTexParameter(int glWrapS, int gWrapT, int glMinFilter,
                                       int glMagFilter) {
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, glWrapS);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, gWrapT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, glMinFilter);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, glMagFilter);
    }

    public static void useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    public static void bindFrameTexture(int frameBufferId, int textureId) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }
}
