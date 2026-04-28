package up.isgc.edu;

import java.awt.Color;


public abstract class Object3D {
    public Color color;

    public Object3D(Color color) {
        this.color = color;
    }

    public abstract Intersection intersect(Ray ray);
}