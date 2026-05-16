package up.isgc.edu;

public class Camera {
    public Vector3D position;
    public double   fovDegrees;
    public int      width;
    public int      height;
    public double   nearClip;
    public double   farClip;

    private double tanHalfFov;
    private double aspectRatio;

    public Camera(Vector3D position, double fovDegrees,
                  int width, int height,
                  double nearClip, double farClip) {
        this.position    = position;
        this.fovDegrees  = fovDegrees;
        this.width       = width;
        this.height      = height;
        this.nearClip    = nearClip;
        this.farClip     = farClip;
        this.tanHalfFov  = Math.tan(Math.toRadians(fovDegrees / 2.0));
        this.aspectRatio = (double) width / height;
    }

    public Camera(Vector3D position, double fovDegrees, int width, int height) {
        this(position, fovDegrees, width, height, 0.001, Double.MAX_VALUE);
    }

    // int overload (compatibilidad con código viejo)
    public Ray generateRay(int px, int py) {
        return generateRay((double) px, (double) py);
    }

    // double overload — necesario para anti-aliasing con jitter
    public Ray generateRay(double px, double py) {
        double ndcX    = (px + 0.5) / width;
        double ndcY    = (py + 0.5) / height;
        double screenX = (2 * ndcX - 1) * aspectRatio * tanHalfFov;
        double screenY = (1 - 2 * ndcY) * tanHalfFov;
        return new Ray(position, new Vector3D(screenX, screenY, -1));
    }

    public boolean inClipRange(double t) {
        return t >= nearClip && t <= farClip;
    }
}