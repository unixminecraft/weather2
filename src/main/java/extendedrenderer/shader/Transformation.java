package extendedrenderer.shader;


import javax.vecmath.Vector3f;

import org.lwjgl.util.vector.Quaternion;

public class Transformation {

    private Matrix4fe modelMatrix;
    
    public Transformation() {
        modelMatrix = new Matrix4fe();
    }

    public Matrix4fe buildModelMatrix(IShaderRenderedEntity gameItem, Vector3f posCustom, float partialTicks) {
        Quaternion q = gameItem.getQuaternion();
        if (gameItem.getQuaternionPrev() != null) {
        	
        	Quaternion previous = gameItem.getQuaternionPrev();
        	Quaternion current = gameItem.getQuaternion();
        	
        	double dot;
        	double s1;
        	double s2;
        	double om;
        	double sinom;
        	
        	dot = current.x * previous.x + current.y * previous.y + current.z * previous.z + current.w * previous.w;
        	if(dot < 0) {
        		previous.x = -previous.x;
        		previous.y = -previous.y;
        		previous.z = -previous.z;
        		previous.w = -previous.w;
        		dot = -dot;
        	}
        	
        	if(1.0D - dot > 0.000001D) {
        		om = Math.acos(dot);
        		sinom = Math.sin(om);
        		s1 = Math.sin((1.0D - partialTicks) * om) / sinom;
        		s2 = Math.sin(partialTicks * om) / sinom;
        	}
        	else {
        		s1 = 1.0D - partialTicks;
        		s2 = partialTicks;
        	}
        	
        	float newX = (float) (s1 * previous.x + s2 * current.x);
        	float newY = (float) (s1 * previous.y + s2 * current.y);
        	float newZ = (float) (s1 * previous.z + s2 * current.z);
        	float newW = (float) (s1 * previous.w + s2 * current.w);
        	
        	q = new Quaternion(newX, newY, newZ, newW);
        }

        float scaleAdj = gameItem.getScale();

        //???
        scaleAdj *= 0.2F;

        Vector3f vecPos = posCustom != null ? posCustom : gameItem.getPosition();

        return modelMatrix.translationRotateScale(
                vecPos.x, vecPos.y, vecPos.z,
                q.x, q.y, q.z, q.w,
                scaleAdj, scaleAdj, scaleAdj);
    }
}
