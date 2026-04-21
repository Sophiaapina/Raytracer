package up.isgc.edu;

public class Camera {
    public Vector3D position;
    public double fovDegrees;
    public int width;
    public int height;

    private double tanHalfFov;
    private double aspectRatio;

    public Camera(Vector3D position, double fovDegrees, int width, int height) {
        this.position = position;
        this.fovDegrees = fovDegrees;
        this.width = width;
        this.height = height;
        this.tanHalfFov = Math.tan(Math.toRadians(fovDegrees / 2.0));
        this.aspectRatio = (double) width / height;
    }

    public Ray generateRay(int px, int py) {
        double ndcX = (px + 0.5) / width;
        double ndcY = (py + 0.5) / height;

        double screenX = (2 * ndcX - 1) * aspectRatio * tanHalfFov;
        double screenY = (1 - 2 * ndcY) * tanHalfFov;

        Vector3D direction = new Vector3D(screenX, screenY, -1);
        return new Ray(position, direction);
    }
}