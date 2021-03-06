package extendedrenderer.shader;

import javax.vecmath.Vector3f;

import org.lwjgl.util.vector.Quaternion;

public interface IShaderRenderedEntity {

    Vector3f getPosition();
    Quaternion getQuaternion();
    Quaternion getQuaternionPrev();
    float getScale();
}
