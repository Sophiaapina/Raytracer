package up.isgc.edu;

import java.awt.Color;

public class Triangle extends Object3D {
    public Vector3D v0, v1, v2;

    private Vector3D n0, n1, n2;
    private Vector3D flatNormal;

    private static final double EPSILON = 1e-8;

    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2,
                    Vector3D n0, Vector3D n1, Vector3D n2, Color color) {
        super(color);
        this.v0 = v0; this.v1 = v1; this.v2 = v2;
        this.n0 = n0; this.n1 = n1; this.n2 = n2;
        this.flatNormal = v1.subtract(v0).cross(v2.subtract(v0)).normalize();
    }

    public Triangle(Vector3D v0, Vector3D v1, Vector3D v2, Color color) {
        this(v0, v1, v2, null, null, null, color);
    }

    @Override
    public Intersection intersect(Ray ray) {
        Vector3D edge1 = v1.subtract(v0);
        Vector3D edge2 = v2.subtract(v0);

        Vector3D P   = ray.direction.cross(edge2);
        double   det = edge1.dot(P);
        if (Math.abs(det) < EPSILON) return null;

        double   invDet = 1.0 / det;
        Vector3D T      = ray.origin.subtract(v0);

        double u = invDet * T.dot(P);
        if (u < 0 || u > 1) return null;

        Vector3D Q = T.cross(edge1);
        double   v = invDet * ray.direction.dot(Q);
        if (v < 0 || (u + v) > (1.0 + EPSILON)) return null;

        double t = invDet * edge2.dot(Q);
        if (t < EPSILON) return null;

        Vector3D point = ray.pointAt(t);

        Vector3D normal;
        if (n0 != null && n1 != null && n2 != null) {
            double w = 1.0 - u - v;
            normal = n0.scale(w).add(n1.scale(u)).add(n2.scale(v)).normalize();
        } else {
            normal = flatNormal;
        }
        if (normal.dot(ray.direction) > 0) normal = normal.scale(-1);

        return new Intersection(t, this, point, normal);
    }
}