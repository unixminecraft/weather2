package extendedrenderer.particle;

import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

import CoroUtil.forge.CULog;

/**
 * Created by corosus on 25/05/17.
 *
 * Manages checking support for instanced rendering
 *
 * Minimum opengl version required for core method use is 3.3 due to use of glVertexAttribDivisor which is part of Instanced Arrays feature
 * Otherwise will attempt to use ARB versions of methods if available
 */
public class ShaderManager {

    private static boolean check = true;

    private static boolean canUseShadersInstancedRendering = false;

    private static boolean useARBInstancedRendering = false;
    private static boolean useARBShaders = false;
    private static boolean useARBVBO = false;
    private static boolean useARBVAO = false;
    private static boolean useARBInstancedArrays = false;

    public static boolean canUseShadersInstancedRendering() {
        if (check) {
            check = false;
            queryGLCaps();
        }
        return canUseShadersInstancedRendering;
    }

    public static void disableShaders() {
        canUseShadersInstancedRendering = false;
    }

    private static void queryGLCaps() {
        ContextCapabilities contextcapabilities = GLContext.getCapabilities();

        CULog.log("Extended Renderer: Detected GLSL version: " + GL11.glGetString(GL_SHADING_LANGUAGE_VERSION));

        useARBVBO = !contextcapabilities.OpenGL15 && contextcapabilities.GL_ARB_vertex_buffer_object;

        if (contextcapabilities.OpenGL21 ||
                (contextcapabilities.GL_ARB_vertex_shader &&
                contextcapabilities.GL_ARB_fragment_shader &&
                contextcapabilities.GL_ARB_shader_objects)) {
            if (contextcapabilities.OpenGL21) {
                useARBShaders = false;
            } else {
                useARBShaders = true;
            }

            if (contextcapabilities.OpenGL33 ||
                    (contextcapabilities.GL_ARB_draw_instanced &&
                    contextcapabilities.GL_ARB_instanced_arrays &&
                    contextcapabilities.GL_ARB_vertex_array_object)) {
                canUseShadersInstancedRendering = true;

                if (contextcapabilities.OpenGL33) {
                    useARBInstancedRendering = false;
                    useARBInstancedArrays = false;
                    useARBVAO = false;
                } else {
                    useARBInstancedRendering = true;
                    useARBInstancedArrays = true;
                    useARBVAO = true;
                }
            } else {
                CULog.log("Extended Renderer WARNING: Unable to use instanced rendering shaders, OpenGL33: " + contextcapabilities.OpenGL33 + ", (" +
                        "GL_ARB_draw_instanced: " + contextcapabilities.GL_ARB_draw_instanced + ", " +
                        "GL_ARB_instanced_arrays: " + contextcapabilities.GL_ARB_instanced_arrays + ", " +
                        "GL_ARB_vertex_array_object: " + contextcapabilities.GL_ARB_vertex_array_object + ")");
                canUseShadersInstancedRendering = false;
            }
        } else {
            CULog.log("Extended Renderer WARNING: Unable to use shaders, OpenGL21: " + contextcapabilities.OpenGL21 + ", (" +
                    "GL_ARB_vertex_shader: " + contextcapabilities.GL_ARB_vertex_shader + ", " +
                    "GL_ARB_fragment_shader: " + contextcapabilities.GL_ARB_fragment_shader + ", " +
                    "GL_ARB_shader_objects: " + contextcapabilities.GL_ARB_shader_objects + ")");
            canUseShadersInstancedRendering = false;
        }
    }

    public static void glDrawElementsInstanced(int mode, int indices_count, int type, long indices_buffer_offset, int primcount) {
        if (useARBInstancedRendering) {
            ARBDrawInstanced.glDrawElementsInstancedARB(mode, indices_count, type, indices_buffer_offset, primcount);
        } else {
            GL31.glDrawElementsInstanced(mode, indices_count, type, indices_buffer_offset, primcount);
        }

    }

    public static void glShaderSource(int shader, CharSequence string) {
        if (useARBShaders) {
            ARBShaderObjects.glShaderSourceARB(shader, string);
        } else {
            GL20.glShaderSource(shader, string);
        }
    }

    public static void glBindAttribLocation(int program, int index, CharSequence name) {
        if (useARBShaders) {
            ARBVertexShader.glBindAttribLocationARB(program, index, name);
        } else {
            GL20.glBindAttribLocation(program, index, name);
        }
    }

    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long buffer_buffer_offset) {
        if (useARBShaders) {
            ARBVertexShader.glVertexAttribPointerARB(index, size, type, normalized, stride, buffer_buffer_offset);
        } else {
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, buffer_buffer_offset);
        }
    }

    public static void glEnableVertexAttribArray(int index) {
        if (useARBShaders) {
            ARBVertexShader.glEnableVertexAttribArrayARB(index);
        } else {
            GL20.glEnableVertexAttribArray(index);
        }
    }

    public static void glDisableVertexAttribArray(int index) {
        if (useARBShaders) {
            ARBVertexShader.glDisableVertexAttribArrayARB(index);
        } else {
            GL20.glDisableVertexAttribArray(index);
        }
    }

    public static void glBufferData(int target, FloatBuffer data, int usage) {
        if (useARBVBO) {
            ARBVertexBufferObject.glBufferDataARB(target, data, usage);
        } else {
            GL15.glBufferData(target, data, usage);
        }
    }

    public static void glBufferData(int target, IntBuffer data, int usage) {
        if (useARBVBO) {
            ARBVertexBufferObject.glBufferDataARB(target, data, usage);
        } else {
            GL15.glBufferData(target, data, usage);
        }
    }

    public static void glVertexAttribDivisor(int index, int divisor) {
        if (useARBInstancedArrays) {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        } else {
            GL33.glVertexAttribDivisor(index, divisor);
        }
    }

    public static void glBindVertexArray(int array) {
        if (useARBVAO) {
            ARBVertexArrayObject.glBindVertexArray(array);
        } else {
            GL30.glBindVertexArray(array);
        }
    }

    public static void glDeleteVertexArrays(int array) {
        if (useARBVAO) {
            ARBVertexArrayObject.glDeleteVertexArrays(array);
        } else {
            GL30.glDeleteVertexArrays(array);
        }
    }

    public static int glGenVertexArrays() {
        if (useARBVAO) {
            return ARBVertexArrayObject.glGenVertexArrays();
        } else {
            return GL30.glGenVertexArrays();
        }
    }

    public static void resetCheck() {
        check = true;
    }
}
