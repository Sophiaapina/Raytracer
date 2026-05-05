package up.isgc.edu;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.List;

public class Raytracer {
    private Scene  scene;
    private Camera camera;
    private Color  background;

    private Vector3D lightPosition   = new Vector3D(5, 10, 8);
    private double   ambientStrength = 0.15;

    public Raytracer(Scene scene, Camera camera, Color background) {
        this.scene      = scene;
        this.camera     = camera;
        this.background = background;
    }

    private Color flatShade(Intersection hit) {
        Vector3D normal  = hit.normal;
        Vector3D toLight = lightPosition.subtract(hit.point).normalize();

        double diffuse   = Math.max(0, normal.dot(toLight));
        double intensity = Math.min(ambientStrength + (1.0 - ambientStrength) * diffuse, 1.0);

        Color base = hit.object.color;
        int r = (int)(base.getRed()   * intensity);
        int g = (int)(base.getGreen() * intensity);
        int b = (int)(base.getBlue()  * intensity);
        return new Color(r, g, b);
    }

    public BufferedImage render() {
        BufferedImage image = new BufferedImage(
                camera.width, camera.height, BufferedImage.TYPE_INT_RGB
        );

        for (int py = 0; py < camera.height; py++) {
            for (int px = 0; px < camera.width; px++) {
                Ray ray = camera.generateRay(px, py);
                Intersection hit = scene.closestIntersection(ray, camera);
                Color color = (hit != null) ? flatShade(hit) : background;
                image.setRGB(px, py, color.getRGB());
            }
        }

        return image;
    }

    public static void main(String[] args) throws Exception {
        int width  = 800;
        int height = 600;

        Scene scene = new Scene();


        String objPath = "src/up/isgc/edu/utils/Teapot.obj";
        Color teapotColor = new Color(255, 105, 180);

        List<Triangle> teapot = OBJreader.loadOBJ(objPath, teapotColor);
        for (Triangle t : teapot) {
            scene.add(t);
        }
        System.out.println("Triangles loaded: " + teapot.size());

        Color floorColor = new Color(60, 60, 70);
        scene.add(new Triangle(
                new Vector3D(-8, 0, -8),
                new Vector3D( 8, 0, -8),
                new Vector3D( 8, 0,  8),
                floorColor
        ));
        scene.add(new Triangle(
                new Vector3D(-8, 0, -8),
                new Vector3D( 8, 0,  8),
                new Vector3D(-8, 0,  8),
                floorColor
        ));

        Camera camera = new Camera(
                new Vector3D(0.2, 3.5, 10),
                45,
                width, height,
                0.1,
                50.0
        );

        Raytracer rt = new Raytracer(scene, camera, new Color(20, 20, 30));
        System.out.println("Rendering...");
        BufferedImage image = rt.render();

        File out = new File("output_v04.png");
        ImageIO.write(image, "png", out);
        System.out.println("Rendered → " + out.getAbsolutePath());
    }
}