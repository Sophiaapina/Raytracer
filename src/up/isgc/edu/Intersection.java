package up.isgc.edu;

public class Intersection {
    public double t;
    public Object3D object;
    public Vector3D point;
    public Vector3D normal;

    public Intersection(double t, Object3D object, Vector3D point, Vector3D normal) {
        this.t      = t;
        this.object = object;
        this.point  = point;
        this.normal = normal.normalize();
    }
}