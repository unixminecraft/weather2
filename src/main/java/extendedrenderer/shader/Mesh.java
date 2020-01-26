package extendedrenderer.shader;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import extendedrenderer.particle.ShaderManager;
import net.minecraft.client.renderer.OpenGlHelper;

/**
 * Created by corosus on 08/05/17.
 */
public class Mesh {

    protected int vaoId;

    protected List<Integer> vboIdList = new ArrayList<>();

    private int vertexCount;

    public Mesh(float[] positions, float[] textCoords, int[] indices) {
        vertexCount = indices.length;

        FloatBuffer verticesBuffer = null;
        IntBuffer indicesBuffer = null;
        FloatBuffer textCoordsBuffer = null;
        try {
            vaoId = ShaderManager.glGenVertexArrays();
            ShaderManager.glBindVertexArray(vaoId);
            int posVboId = OpenGlHelper.glGenBuffers();
            vboIdList.add(posVboId);
            verticesBuffer = BufferUtils.createFloatBuffer(positions.length);//MemoryUtil.memAllocFloat(vertices.length);
            verticesBuffer.put(positions).flip();
            OpenGlHelper.glBindBuffer(GL15.GL_ARRAY_BUFFER, posVboId);
            ShaderManager.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
            // Define structure of the data
            ShaderManager.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
            //might not be needed, but added when downgrading to GLSL 120

            //tex vbo
            int texVboId = OpenGlHelper.glGenBuffers();
            vboIdList.add(texVboId);
            textCoordsBuffer = BufferUtils.createFloatBuffer(textCoords.length);
            textCoordsBuffer.put(textCoords).flip();
            OpenGlHelper.glBindBuffer(GL15.GL_ARRAY_BUFFER, texVboId);
            ShaderManager.glBufferData(GL15.GL_ARRAY_BUFFER, textCoordsBuffer, GL15.GL_STATIC_DRAW);
            ShaderManager.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0);

            //index vbo
            int idxVboId = OpenGlHelper.glGenBuffers();
            vboIdList.add(idxVboId);
            indicesBuffer = BufferUtils.createIntBuffer(indices.length);
            indicesBuffer.put(indices).flip();
            OpenGlHelper.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, idxVboId);
            ShaderManager.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

            // Unbind the VBO
            OpenGlHelper.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            // Unbind the VAO
            ShaderManager.glBindVertexArray(0);
        } finally {
            /**
             * TODO: test if we need to actually free the memory since we have to use BufferUtils.createFloatBuffer instead of MemoryUtil.memAllocFloat
             * "It's not trivial because I want to make it optional and using jemalloc requires explicit je_free calls to avoid leaking memory.
             * Existing usages of BufferUtils do not have that requirement and will have to be adjusted accordingly."
             *
             * BufferUtils is more automatic, doesnt need freeing, but can be slower and risks memory fragmentation, MemoryUtil gives more control and responsibility
             */
        }
    }

    public void initRender() {

        // Draw the mesh
        ShaderManager.glBindVertexArray(getVaoId());
        ShaderManager.glEnableVertexAttribArray(0);
        ShaderManager.glEnableVertexAttribArray(1);
    }

    public void endRender() {
        // Restore state
        ShaderManager.glDisableVertexAttribArray(0);
        ShaderManager.glDisableVertexAttribArray(1);
        ShaderManager.glBindVertexArray(0);
    }

    private int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public void cleanup() {
        ShaderManager.glDisableVertexAttribArray(0);

        // Delete the VBO
        OpenGlHelper.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            OpenGlHelper.glDeleteBuffers(vboId);
        }

        // Delete the VAO
        ShaderManager.glBindVertexArray(0);
        ShaderManager.glDeleteVertexArrays(vaoId);
    }
}
