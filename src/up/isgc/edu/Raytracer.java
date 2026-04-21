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
        this.scene = scene;
        this.camera = camera;
        this.background = background;
    }

    public BufferedImage render() {
        BufferedImage image = new BufferedImage(
                camera.width, camera.height, BufferedImage.TYPE_INT_RGB
        );

        for (int py = 0; py < camera.height; py++) {
            for (int px = 0; px < camera.width; px++) {
                Ray ray = camera.generateRay(px, py);
                Intersection hit = scene.closestIntersection(ray);

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
        scene.add(new Sphere(new Vector3D(-1.0, 0.0, 0.0), 1.0, Color.RED));
        scene.add(new Sphere(new Vector3D( 1.5, 0.0, 0.0), 0.6, Color.BLUE));

        Camera camera = new Camera(new Vector3D(0, 0, 5), 60, width, height);


        Raytracer rt = new Raytracer(scene, camera, Color.WHITE);
        BufferedImage image = rt.render();


        File out = new File("output.png");
        ImageIO.write(image, "png", out);
        System.out.println("Rendered → " + out.getAbsolutePath());
    }
}