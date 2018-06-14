package com.example.okhof.pointcloudsardouble.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.PointCloud;
import com.example.okhof.pointcloudsardouble.R;

import java.nio.FloatBuffer;

// --Point Cloud Renderer Class--
/**
 * Created by Patrick Ley on 26.03.2018.
 * This is a modified version of the Point Cloud
 * Renderer class. It has been modified to allow
 * for the passing of point clouds in larger and
 * colored variants. The shader files were rewritten
 * for this purpose.
 */

public class PointClass {
    private static final String TAG = PointCloud.class.getSimpleName();

    private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
    private static final int FLOATS_PER_POINT = 4; // X,Y,Z,confidence.
    private static final int BYTES_PER_POINT = BYTES_PER_FLOAT * FLOATS_PER_POINT;
    private static final int INITIAL_BUFFER_POINTS = 1000;

    private int vbo;
    private int vboSize;

    private int programName;
    private int positionAttribute;
    private int modelViewProjectionUniform;
    private int colorUniform;
    private int pointSizeUniform;
    private int colorPointer;

    private int numPoints = 0;

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.
    private PointCloud lastPointCloud = null;

    public PointClass() {}

    /**
     * Allocates and initializes OpenGL resources needed by the plane renderer. Must be called on the
     * OpenGL thread, typically in.
     *
     * @param context Needed to access shader source.
     */
    public void createOnGlThread(Context context) {
        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError(TAG, "before create");

        int[] buffers = new int[1];
        GLES20.glGenBuffers(1, buffers, 0);
        vbo = buffers[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

        vboSize = INITIAL_BUFFER_POINTS * BYTES_PER_POINT;
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError(TAG, "buffer alloc");

        int vertexShader =
                com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, R.raw.point_cloud_vertex_test);
        int passthroughShader =
                com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.loadGLShader(
                        TAG, context, GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);

        programName = GLES20.glCreateProgram();
        GLES20.glAttachShader(programName, vertexShader);
        GLES20.glAttachShader(programName, passthroughShader);
        GLES20.glLinkProgram(programName);
        GLES20.glUseProgram(programName);

        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError("MyStuff", "program");

        positionAttribute = GLES20.glGetAttribLocation(programName, "a_Position");
        //colorUniform = GLES20.glGetUniformLocation(programName, "u_Color");
        modelViewProjectionUniform = GLES20.glGetUniformLocation(programName, "u_ModelViewProjection");
        pointSizeUniform = GLES20.glGetUniformLocation(programName, "u_PointSize");
        colorPointer = GLES20.glGetAttribLocation(programName, "a_Color");

        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError(TAG, "program  params");
    }

    /**
     * Updates the OpenGL buffer contents to the provided point. Repeated calls with the same point
     * cloud will be ignored.
     */
    public void update(FloatBuffer points, FloatBuffer color_pts) {

        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError(TAG, "before update");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);

        // If the VBO is not large enough to fit the new point cloud, resize it.
        numPoints = points.remaining() / FLOATS_PER_POINT;
        if (numPoints * BYTES_PER_POINT > vboSize) {
            while (numPoints * BYTES_PER_POINT > vboSize) {
                vboSize *= 2;
            }
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 2*vboSize, null, GLES20.GL_DYNAMIC_DRAW);
        }
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, 0, numPoints * BYTES_PER_POINT, points);
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER, numPoints * BYTES_PER_POINT, numPoints * BYTES_PER_POINT, color_pts);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError(TAG, "after update");
    }

    /**
     * Renders the point cloud. ArCore point cloud is given in world space.
     *
     * @param cameraView the camera view matrix for this frame, typically from {@link
     *     com.google.ar.core.Camera#getViewMatrix(float[], int)}.
     * @param cameraPerspective the camera projection matrix for this frame, typically from {@link
     *     com.google.ar.core.Camera#getProjectionMatrix(float[], int, float, float)}.
     */
    public void draw(float[] cameraView, float[] cameraPerspective, float pointSize) {
        float[] modelViewProjection = new float[16];
        Matrix.multiplyMM(modelViewProjection, 0, cameraPerspective, 0, cameraView, 0);

        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError(TAG, "Before draw");

        GLES20.glUseProgram(programName);
        GLES20.glEnableVertexAttribArray(positionAttribute);
        GLES20.glEnableVertexAttribArray(colorPointer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo);
        GLES20.glVertexAttribPointer(colorPointer, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, numPoints * BYTES_PER_POINT);
        GLES20.glVertexAttribPointer(positionAttribute, 4, GLES20.GL_FLOAT, false, BYTES_PER_POINT, 0);
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjection, 0);
        GLES20.glUniform1f(pointSizeUniform, pointSize);


        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, numPoints);
        GLES20.glDisableVertexAttribArray(positionAttribute);
        GLES20.glDisableVertexAttribArray(colorPointer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        com.google.ar.core.examples.java.helloar.rendering.ShaderUtil.checkGLError(TAG, "Draw");
    }
}
