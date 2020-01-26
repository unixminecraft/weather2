package extendedrenderer.shader;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import extendedrenderer.particle.ShaderManager;
import net.minecraft.client.renderer.OpenGlHelper;

public class InstancedMeshParticle extends Mesh {

	private static final int FLOAT_SIZE_BYTES = 4;

	private static final int VECTOR4F_SIZE_BYTES = 4 * FLOAT_SIZE_BYTES;

    public static final int MATRIX_SIZE_FLOATS = 4 * 4;

    private static final int MATRIX_SIZE_BYTES = MATRIX_SIZE_FLOATS * FLOAT_SIZE_BYTES;

    private static final int INSTANCE_SIZE_BYTES = MATRIX_SIZE_BYTES + FLOAT_SIZE_BYTES * (1 + 4)/* * 2 + FLOAT_SIZE_BYTES * 2*/;

    public static final int INSTANCE_SIZE_FLOATS = MATRIX_SIZE_FLOATS + 1 + 4;// * 2 + 2;

    public final int numInstances;

    public final int instanceDataVBO;

    public FloatBuffer instanceDataBuffer;

    public int curBufferPos = 0;

    /**
     * TODO: despite the mesh only being a size of 2 vbos instead of 5, lowering this to 2 breaks something somehow (no rendering)
     * need to figure out where to fix so i can optimize memory usage
     * not even sure if the memory is unoptimized, theres just gaps in the memory used probably
     *
     * fixed, didnt account for attrib location values in shader program
     */
    public static int vboSizeMesh = 2;

    public InstancedMeshParticle(float[] positions, float[] textCoords, int[] indices, int numInstances) {
        super(positions, textCoords, indices);

        this.numInstances = numInstances;

        ShaderManager.glBindVertexArray(vaoId);

        // Model Matrix
        instanceDataVBO = GL15.glGenBuffers();
        vboIdList.add(instanceDataVBO);
        instanceDataBuffer = BufferUtils.createFloatBuffer(numInstances * INSTANCE_SIZE_FLOATS);//MemoryUtil.memAllocFloat(numInstances * INSTANCE_SIZE_FLOATS);
        OpenGlHelper.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceDataVBO);
        int start = vboSizeMesh;
        int strideStart = 0;
        for (int i = 0; i < 4; i++) {
            ShaderManager.glVertexAttribPointer(start, 4, GL11.GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
            ShaderManager.glVertexAttribDivisor(start, 1);
            start++;
            strideStart += VECTOR4F_SIZE_BYTES;
        }

        //TODO: might become UV lightmap coord in future
        //brightness
        ShaderManager.glVertexAttribPointer(start, 1, GL11.GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
        ShaderManager.glVertexAttribDivisor(start, 1);
        start++;
        strideStart += FLOAT_SIZE_BYTES;

        /**
         * TODO: rbg and alpha for colorization
         * storm darkening uses lower rgb values to darken
         * everything uses alpha for fading in and out
         *
         */

        //rgba
        ShaderManager.glVertexAttribPointer(start, 4, GL11.GL_FLOAT, false, INSTANCE_SIZE_BYTES, strideStart);
        ShaderManager.glVertexAttribDivisor(start, 1);
        start++;
        strideStart += VECTOR4F_SIZE_BYTES;

        OpenGlHelper.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        ShaderManager.glBindVertexArray(0);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (this.instanceDataBuffer != null) {
            this.instanceDataBuffer = null;
        }
    }

    public void initRenderVBO1() {
        int start = vboSizeMesh;
        //model matrix + brightness
        int numElements = 5 + 1;
        for (int i = 0; i < numElements; i++) {
            ShaderManager.glEnableVertexAttribArray(start + i);
        }
    }

    public void endRenderVBO1() {

        int start = vboSizeMesh;
        //model matrix + brightness
        int numElements = 5 + 1;
        for (int i = 0; i < numElements; i++) {
            ShaderManager.glDisableVertexAttribArray(start + i);
        }
    }
}
