package up.isgc.edu;

import java.awt.Color;

public abstract class Object3D {
    public Color  color;

    // Phong
    public double ambient   = 0.15;
    public double diffuse   = 0.7;
    public double specular  = 0.5;
    public double shininess = 32.0;

    // Reflexión / Refracción
    public double reflectivity = 0.0;  // 0=mate, 1=espejo perfecto
    public double transparency = 0.0;  // 0=opaco, 1=totalmente transparente
    public double ior          = 1.5;  // índice de refracción (vidrio=1.5, agua=1.33)

    public Object3D(Color color) { this.color = color; }

    public Object3D setMaterial(double ka, double kd, double ks, double n) {
        this.ambient = ka; this.diffuse = kd;
        this.specular = ks; this.shininess = n;
        return this;
    }

    public Object3D setReflectivity(double r) { this.reflectivity = r; return this; }
    public Object3D setTransparency(double t, double ior) {
        this.transparency = t; this.ior = ior; return this;
    }

    // Las subclases pueden sobreescribir para texturas procedurales
    public Color getColor(Vector3D point) { return color; }

    public abstract Intersection intersect(Ray ray);
}