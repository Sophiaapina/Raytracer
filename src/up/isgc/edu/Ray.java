package up.isgc.edu;

public class Ray {
    public Vector3D origin;
    public Vector3D direction;

    public Ray(Vector3D origin, Vector3D direction) {
        this.origin = origin;
        this.direction = direction.normalize();
    }

    public Vector3D pointAt(double t) {
        return origin.add(direction.scale(t));
    }
}