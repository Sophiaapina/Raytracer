package up.isgc.edu;

import java.awt.Color;

public class Light {

    public enum Type { DIRECTIONAL, POINT }

    public Type     type;
    public Vector3D vector;
    public Color    color;
    public double   intensity;

    public static Light directional(Vector3D direction, Color color, double intensity) {
        Light l  = new Light();
        l.type      = Type.DIRECTIONAL;
        l.vector    = direction.normalize();
        l.color     = color;
        l.intensity = intensity;
        return l;
    }

    public static Light point(Vector3D position, Color color, double intensity) {
        Light l  = new Light();
        l.type      = Type.POINT;
        l.vector    = position;
        l.color     = color;
        l.intensity = intensity;
        return l;
    }

    public Vector3D directionFrom(Vector3D point) {
        if (type == Type.DIRECTIONAL) {
            return vector.scale(-1).normalize();
        } else {
            return vector.subtract(point).normalize();
        }
    }

    public double distanceFrom(Vector3D point) {
        if (type == Type.DIRECTIONAL) {
            return Double.MAX_VALUE;
        } else {
            Vector3D diff = vector.subtract(point);
            return Math.sqrt(diff.dot(diff));
        }
    }

    public double attenuation(Vector3D point) {
        if (type == Type.DIRECTIONAL) return 1.0;
        double d  = distanceFrom(point);
        double kl = 0.09;   // linear
        double kq = 0.032;  // quadratic
        return 1.0 / (1.0 + kl * d + kq * d * d);
    }
}