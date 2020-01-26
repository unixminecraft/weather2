package extendedrenderer.shader;

import java.nio.FloatBuffer;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

/**
 * Created by corosus on 08/05/17.
 */
public class Matrix4fe extends Matrix4f {

	private static final long serialVersionUID = 5342089086832176634L;
	
	private byte properties;

    public Matrix4fe() {
        this.m00 = 1.0F;
        this.m11 = 1.0F;
        this.m22 = 1.0F;
        this.m33 = 1.0F;
    }

    public static void get(Matrix4f m, int offset, FloatBuffer src) {
        m.m00 = src.get(offset);
        m.m01 = src.get(offset + 1);
        m.m02 = src.get(offset + 2);
        m.m03 = src.get(offset + 3);
        m.m10 = src.get(offset + 4);
        m.m11 = src.get(offset + 5);
        m.m12 = src.get(offset + 6);
        m.m13 = src.get(offset + 7);
        m.m20 = src.get(offset + 8);
        m.m21 = src.get(offset + 9);
        m.m22 = src.get(offset + 10);
        m.m23 = src.get(offset + 11);
        m.m30 = src.get(offset + 12);
        m.m31 = src.get(offset + 13);
        m.m32 = src.get(offset + 14);
        m.m33 = src.get(offset + 15);
    }

    private void properties(int properties) {
        this.properties = 0;
    }

    private Matrix4fe rotateX(float ang, Matrix4fe dest) {
        if((this.properties & 4) != 0) {
            return this;//dest.rotationX(ang);
        } else {
            float sin = (float)Math.sin((double)ang);
            float cos = (float)cosFromSin((double)sin, (double)ang);
            float rm21 = -sin;
            float nm10 = this.m10 * cos + this.m20 * sin;
            float nm11 = this.m11 * cos + this.m21 * sin;
            float nm12 = this.m12 * cos + this.m22 * sin;
            float nm13 = this.m13 * cos + this.m23 * sin;
            dest.m20 = this.m10 * rm21 + this.m20 * cos;
            dest.m21 = this.m11 * rm21 + this.m21 * cos;
            dest.m22 = this.m12 * rm21 + this.m22 * cos;
            dest.m23 = this.m13 * rm21 + this.m23 * cos;
            dest.m10 = nm10;
            dest.m11 = nm11;
            dest.m12 = nm12;
            dest.m13 = nm13;
            dest.m00 = this.m00;
            dest.m01 = this.m01;
            dest.m02 = this.m02;
            dest.m03 = this.m03;
            dest.m30 = this.m30;
            dest.m31 = this.m31;
            dest.m32 = this.m32;
            dest.m33 = this.m33;
            dest.properties((byte)(this.properties & -14));
            return dest;
        }
    }

    public Matrix4fe rotateX(float ang) {
        return this.rotateX(ang, this);
    }

    private Matrix4fe rotateY(float ang, Matrix4fe dest) {
        if((this.properties & 4) != 0) {
            return this;//dest.rotationY(ang);
        } else {
            float sin = (float)Math.sin((double)ang);
            float cos = (float)cosFromSin((double)sin, (double)ang);
            float rm02 = -sin;
            float nm00 = this.m00 * cos + this.m20 * rm02;
            float nm01 = this.m01 * cos + this.m21 * rm02;
            float nm02 = this.m02 * cos + this.m22 * rm02;
            float nm03 = this.m03 * cos + this.m23 * rm02;
            dest.m20 = this.m00 * sin + this.m20 * cos;
            dest.m21 = this.m01 * sin + this.m21 * cos;
            dest.m22 = this.m02 * sin + this.m22 * cos;
            dest.m23 = this.m03 * sin + this.m23 * cos;
            dest.m00 = nm00;
            dest.m01 = nm01;
            dest.m02 = nm02;
            dest.m03 = nm03;
            dest.m10 = this.m10;
            dest.m11 = this.m11;
            dest.m12 = this.m12;
            dest.m13 = this.m13;
            dest.m30 = this.m30;
            dest.m31 = this.m31;
            dest.m32 = this.m32;
            dest.m33 = this.m33;
            dest.properties((byte)(this.properties & -14));
            return dest;
        }
    }

    public Matrix4fe rotateY(float ang) {
        return this.rotateY(ang, this);
    }

    private static double cosFromSin(double sin, double angle) {
        double cos = Math.sqrt(1.0D - sin * sin);
        double a = angle + 1.5707963267948966D;
        double b = a - (double)((int)(a / 6.283185307179586D)) * 6.283185307179586D;
        if(b < 0.0D) {
            b += 6.283185307179586D;
        }

        return b >= 3.141592653589793D?-cos:cos;
    }

    private final Matrix4fe identity(Matrix4fe dest) {
        dest.m00 = 1.0F;
        dest.m01 = 0.0F;
        dest.m02 = 0.0F;
        dest.m03 = 0.0F;
        dest.m10 = 0.0F;
        dest.m11 = 1.0F;
        dest.m12 = 0.0F;
        dest.m13 = 0.0F;
        dest.m20 = 0.0F;
        dest.m21 = 0.0F;
        dest.m22 = 1.0F;
        dest.m23 = 0.0F;
        dest.m30 = 0.0F;
        dest.m31 = 0.0F;
        dest.m32 = 0.0F;
        dest.m33 = 1.0F;
        return dest;
    }

    public FloatBuffer get(FloatBuffer buffer) {
        return this.get(buffer.position(), buffer);
    }

    public FloatBuffer get(int index, FloatBuffer buffer) {
        if(index == 0) {
            this.put0(this, buffer);
        } else {
            this.putN(this, index, buffer);
        }
        return buffer;
    }

    private void putN(Matrix4f m, int offset, FloatBuffer dest) {
        dest.put(offset, m.m00);
        dest.put(offset + 1, m.m01);
        dest.put(offset + 2, m.m02);
        dest.put(offset + 3, m.m03);
        dest.put(offset + 4, m.m10);
        dest.put(offset + 5, m.m11);
        dest.put(offset + 6, m.m12);
        dest.put(offset + 7, m.m13);
        dest.put(offset + 8, m.m20);
        dest.put(offset + 9, m.m21);
        dest.put(offset + 10, m.m22);
        dest.put(offset + 11, m.m23);
        dest.put(offset + 12, m.m30);
        dest.put(offset + 13, m.m31);
        dest.put(offset + 14, m.m32);
        dest.put(offset + 15, m.m33);
    }

    private void put0(Matrix4f m, FloatBuffer dest) {
        dest.put(0, m.m00);
        dest.put(1, m.m01);
        dest.put(2, m.m02);
        dest.put(3, m.m03);
        dest.put(4, m.m10);
        dest.put(5, m.m11);
        dest.put(6, m.m12);
        dest.put(7, m.m13);
        dest.put(8, m.m20);
        dest.put(9, m.m21);
        dest.put(10, m.m22);
        dest.put(11, m.m23);
        dest.put(12, m.m30);
        dest.put(13, m.m31);
        dest.put(14, m.m32);
        dest.put(15, m.m33);
    }

    public Matrix4fe translate(Vector3f offset) {
        return this.translate(offset.x, offset.y, offset.z);
    }

    private Matrix4fe translate(float x, float y, float z) {
        if((this.properties & 4) != 0) {
            return this.translation(x, y, z);
        } else {
            this.m30 = this.m00 * x + this.m10 * y + this.m20 * z + this.m30;
            this.m31 = this.m01 * x + this.m11 * y + this.m21 * z + this.m31;
            this.m32 = this.m02 * x + this.m12 * y + this.m22 * z + this.m32;
            this.m33 = this.m03 * x + this.m13 * y + this.m23 * z + this.m33;
            this.properties &= -6;
            return this;
        }
    }

    private Matrix4fe translation(float x, float y, float z) {
        identity(this);
        this.m30 = x;
        this.m31 = y;
        this.m32 = z;
        this.properties(10);
        return this;
    }

    public Matrix4fe mul(Matrix4fe right) {
        return this.mulGeneric(right, this);
    }

    private Matrix4fe mulGeneric(Matrix4fe right, Matrix4fe dest) {
        float nm00 = this.m00 * right.m00 + this.m10 * right.m01 + this.m20 * right.m02 + this.m30 * right.m03;
        float nm01 = this.m01 * right.m00 + this.m11 * right.m01 + this.m21 * right.m02 + this.m31 * right.m03;
        float nm02 = this.m02 * right.m00 + this.m12 * right.m01 + this.m22 * right.m02 + this.m32 * right.m03;
        float nm03 = this.m03 * right.m00 + this.m13 * right.m01 + this.m23 * right.m02 + this.m33 * right.m03;
        float nm10 = this.m00 * right.m10 + this.m10 * right.m11 + this.m20 * right.m12 + this.m30 * right.m13;
        float nm11 = this.m01 * right.m10 + this.m11 * right.m11 + this.m21 * right.m12 + this.m31 * right.m13;
        float nm12 = this.m02 * right.m10 + this.m12 * right.m11 + this.m22 * right.m12 + this.m32 * right.m13;
        float nm13 = this.m03 * right.m10 + this.m13 * right.m11 + this.m23 * right.m12 + this.m33 * right.m13;
        float nm20 = this.m00 * right.m20 + this.m10 * right.m21 + this.m20 * right.m22 + this.m30 * right.m23;
        float nm21 = this.m01 * right.m20 + this.m11 * right.m21 + this.m21 * right.m22 + this.m31 * right.m23;
        float nm22 = this.m02 * right.m20 + this.m12 * right.m21 + this.m22 * right.m22 + this.m32 * right.m23;
        float nm23 = this.m03 * right.m20 + this.m13 * right.m21 + this.m23 * right.m22 + this.m33 * right.m23;
        float nm30 = this.m00 * right.m30 + this.m10 * right.m31 + this.m20 * right.m32 + this.m30 * right.m33;
        float nm31 = this.m01 * right.m30 + this.m11 * right.m31 + this.m21 * right.m32 + this.m31 * right.m33;
        float nm32 = this.m02 * right.m30 + this.m12 * right.m31 + this.m22 * right.m32 + this.m32 * right.m33;
        float nm33 = this.m03 * right.m30 + this.m13 * right.m31 + this.m23 * right.m32 + this.m33 * right.m33;
        dest.m00 = nm00;
        dest.m01 = nm01;
        dest.m02 = nm02;
        dest.m03 = nm03;
        dest.m10 = nm10;
        dest.m11 = nm11;
        dest.m12 = nm12;
        dest.m13 = nm13;
        dest.m20 = nm20;
        dest.m21 = nm21;
        dest.m22 = nm22;
        dest.m23 = nm23;
        dest.m30 = nm30;
        dest.m31 = nm31;
        dest.m32 = nm32;
        dest.m33 = nm33;
        dest.properties(0);
        return dest;
    }

    public Matrix4fe translationRotateScale(float tx, float ty, float tz, float qx, float qy, float qz, float qw, float sx, float sy, float sz) {
        float dqx = qx + qx;
        float dqy = qy + qy;
        float dqz = qz + qz;

        float q00 = dqx * qx;
        float q11 = dqy * qy;
        float q22 = dqz * qz;
        float q01 = dqx * qy;
        float q02 = dqx * qz;
        float q03 = dqx * qw;
        float q12 = dqy * qz;
        float q13 = dqy * qw;
        float q23 = dqz * qw;

        this.m00 = sx - (q11 + q22) * sx;
        this.m01 = (q01 + q23) * sx;
        this.m02 = (q02 - q13) * sx;
        this.m03 = 0.0F;

        this.m10 = (q01 - q23) * sy;
        this.m11 = sy - (q22 + q00) * sy;
        this.m12 = (q12 + q03) * sy;
        this.m13 = 0.0F;

        this.m20 = (q02 + q13) * sz;
        this.m21 = (q12 - q03) * sz;
        this.m22 = sz - (q11 + q00) * sz;
        this.m23 = 0.0F;

        this.m30 = tx;
        this.m31 = ty;
        this.m32 = tz;
        this.m33 = 1.0F;
        this.properties(2);
        return this;
    }

    public Vector3f getTranslation() {
        return new Vector3f(m30, m31, m32);
    }
}
