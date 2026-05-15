package up.isgc.edu;

import java.awt.Color;

// Plano infinito con textura checkerboard procedural
public class CheckerPlane extends Object3D {
    private double y;           // altura del plano
    private Color  colorA;
    private Color  colorB;
    private double tileSize;

    public CheckerPlane(double y, Color colorA, Color colorB, double tileSize) {
        super(colorA);
        this.y        = y;
        this.colorA   = colorA;
        this.colorB   = colorB;
        this.tileSize = tileSize;
    }

    @Override
    public Color getColor(Vector3D point) {
        int ix = (int) Math.floor(point.x / tileSize);
        int iz = (int) Math.floor(point.z / tileSize);
        // Manejo correcto de negativos
        if (point.x < 0) ix--;
        if (point.z < 0) iz--;
        return ((ix + iz) % 2 == 0) ? colorA : colorB;
    }

    @Override
    public Intersection intersect(Ray ray) {
        // Plano horizontal y=this.y → t = (y - ray.origin.y) / ray.direction.y
        if (Math.abs(ray.direction.y) < 1e-8) return null;
        double t = (y - ray.origin.y) / ray.direction.y;
        if (t < 1e-4) return null;

        Vector3D point  = ray.pointAt(t);
        Vector3D normal = new Vector3D(0, 1, 0);
        if (ray.direction.y > 0) normal = new Vector3D(0, -1, 0);
        return new Intersection(t, this, point, normal);
    }
}