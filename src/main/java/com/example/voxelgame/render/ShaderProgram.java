package com.example.voxelgame.render;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL33C.*;

public final class ShaderProgram implements AutoCloseable {

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        Objects.requireNonNull(vertexShaderSource);
        Objects.requireNonNull(fragmentShaderSource);

        int vertexShaderId = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShaderId = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException(
                    "Failed to link shader program: " + glGetProgramInfoLog(programId)
            );
        }

        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    private int getUniformLocation(String name) {
        Integer cached = uniformCache.get(name);
        if (cached != null) return cached;

        int location = glGetUniformLocation(programId, name);
        if (location < 0) {
            throw new IllegalArgumentException("Unknown uniform: " + name);
        }

        uniformCache.put(name, location);
        return location;
    }

    public void setMatrix4(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    public void setInt(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    public void setFloat(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }

    public void setVector2(String name, float x, float y) {
        glUniform2f(getUniformLocation(name), x, y);
    }

    public void setVector3(String name, float x, float y, float z) {
        glUniform3f(getUniformLocation(name), x, y, z);
    }

    public void setVector4(String name, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    @Override
    public void close() {
        glDeleteProgram(programId);
    }

    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException(
                    "Failed to compile shader: " + glGetShaderInfoLog(shader)
            );
        }

        return shader;
    }
}
