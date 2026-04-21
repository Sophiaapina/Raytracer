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
        // L = C - O
        Vector3D L = center.subtract(ray.origin);

        // tca = L · D
        double tca = L.dot(ray.direction);

        // If tca < 0, sphere is behind the ray
        if (tca < 0) return null;

        // d² + tca² = L²  →  d² = L·L - tca·tca
        double d2 = L.dot(L) - tca * tca;
        double r2 = radius * radius;

        // If d² > r², ray misses the sphere
        if (d2 > r2) return null;

        // thc = √(radius² - d²)
        double thc = Math.sqrt(r2 - d2);

        double t0 = tca - thc;
        double t1 = tca + thc;

        double t = (t0 > 0) ? t0 : t1;
        if (t < 0) return null;

        return new Intersection(t, this);
    }
}