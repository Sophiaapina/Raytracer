package up.isgc.edu;

import java.awt.Color;

public class Sphere extends Object3D {
    public Vector3D center;
    public double radius;

    public Sphere(Vector3D center, double radius, Color color) {
        super(color);
        this.center = center;
        this.radius = radius;
    }

    @Override
    public Intersection intersect(Ray ray) {
        Vector3D L = center.subtract(ray.origin);
        double tca = L.dot(ray.direction);
        if (tca < 0) return null;

        double d2 = L.dot(L) - tca * tca;
        double r2 = radius * radius;
        if (d2 > r2) return null;

        double thc = Math.sqrt(r2 - d2);
        double t0 = tca - thc;
        double t1 = tca + thc;
        double t = (t0 > 0) ? t0 : t1;
        if (t < 0) return null;

        Vector3D point  = ray.pointAt(t);
        Vector3D normal = point.subtract(center).normalize();
        return new Intersection(t, this, point, normal);
    }
}