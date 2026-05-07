package up.isgc.edu;

import java.awt.Color;

public abstract class Object3D {
    public Color color;

    public double ambient    = 0.15;
    public double diffuse    = 0.7;
    public double specular   = 0.5;
    public double shininess  = 32.0;

    public Object3D(Color color) {
        this.color = color;
    }


    public Object3D setMaterial(double ka, double kd, double ks, double n) {
        this.ambient   = ka;
        this.diffuse   = kd;
        this.specular  = ks;
        this.shininess = n;
        return this;
    }

    public abstract Intersection intersect(Ray ray);
}