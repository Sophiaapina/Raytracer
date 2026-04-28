package up.isgc.edu;

import java.awt.Color;

public class Triangle extends Object3D {
    public Vector3D v0, v1, v2;
    private static final double EPSILON = 1e-8;

    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Color color) {
        super(color);
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    public Intersection intersect(Ray ray) {
        Vector3D v2v0 = v2.subtract(v0);
        Vector3D v1v0 = v1.subtract(v0);


        Vector3D P = ray.direction.cross(v1v0);
        double determinant = v2v0.dot(P);
        if (Math.abs(determinant) < EPSILON) return null;

        double invDet = 1.0 / determinant;

        Vector3D T = ray.origin.subtract(v0);

        double u = invDet * T.dot(P);
        if (u < 0 || u > 1) return null;

        Vector3D Q = T.cross(v2v0);

        double v = invDet * ray.direction.dot(Q);
        if (v < 0 || (u + v) > (1.0 + EPSILON)) return null;  // outside triangle

        double t = invDet * Q.dot(v1v0);
        if (t < 0) return null;

        return new Intersection(t, this);
    }
}