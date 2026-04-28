package up.isgc.edu;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Raytracer {
    private Scene scene;
    private Camera camera;
    private Color background;

    public Raytracer(Scene scene, Camera camera, Color background) {
        this.scene      = scene;
        this.camera     = camera;
        this.background = background;
    }

    public BufferedImage render() {
        BufferedImage image = new BufferedImage(
                camera.width, camera.height, BufferedImage.TYPE_INT_RGB
        );

        for (int py = 0; py < camera.height; py++) {
            for (int px = 0; px < camera.width; px++) {
                Ray ray = camera.generateRay(px, py);

                Intersection hit = scene.closestIntersection(ray, camera);

                Color color = (hit != null) ? hit.object.color : background;
                image.setRGB(px, py, color.getRGB());
            }
        }

        return image;
    }

    public static void main(String[] args) throws Exception {
        int width  = 800;
        int height = 600;

        Scene scene = new Scene();

        scene.add(new Sphere(new Vector3D(-1.5, 0.5, 0), 0.8, Color.RED));
        scene.add(new Sphere(new Vector3D( 1.5, 0.5, 0), 0.5, Color.BLUE));


        scene.add(new Triangle(
                new Vector3D(-3.0, -0.5,  1.0),
                new Vector3D( 3.0, -0.5,  1.0),
                new Vector3D( 0.0, -0.5, -2.0),
                new Color(50, 180, 80)          // green floor
        ));

        scene.add(new Triangle(
                new Vector3D( 0.0,  1.5, -0.5),
                new Vector3D(-0.8,  0.2, -0.5),
                new Vector3D( 0.8,  0.2, -0.5),
                new Color(255, 200, 0)
        ));

        Camera camera = new Camera(
                new Vector3D(0, 0, 5),
                60,
                width, height,
                0.1,
                20.0
        );

        Raytracer rt = new Raytracer(scene, camera, Color.WHITE);
        BufferedImage image = rt.render();

        File out = new File("output_v02.png");
        ImageIO.write(image, "png", out);
        System.out.println("Rendered → " + out.getAbsolutePath());
    }
}