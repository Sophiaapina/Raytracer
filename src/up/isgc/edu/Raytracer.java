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

    private Vector3D lightPosition = new Vector3D(5, 10, 8);
    private Color    lightColor    = Color.WHITE;

    public Raytracer(Scene scene, Camera camera, Color background) {
        this.scene      = scene;
        this.camera     = camera;
        this.background = background;
    }

    private Color phongShade(Intersection hit, Ray ray) {
        Object3D obj = hit.object;
        Vector3D N   = hit.normal;
        Vector3D L   = lightPosition.subtract(hit.point).normalize();
        Vector3D V   = ray.direction.scale(-1).normalize();


        double NdotL = Math.max(0, N.dot(L));
        Vector3D R   = N.scale(2 * NdotL).subtract(L).normalize();

        double RdotV = Math.max(0, R.dot(V));

        double ambientI  = obj.ambient;
        double diffuseI  = obj.diffuse  * NdotL;
        double specularI = obj.specular * Math.pow(RdotV, obj.shininess);

        Color base = obj.color;
        double br = base.getRed()   / 255.0;
        double bg = base.getGreen() / 255.0;
        double bb = base.getBlue()  / 255.0;

        double lr = lightColor.getRed()   / 255.0;
        double lg = lightColor.getGreen() / 255.0;
        double lb = lightColor.getBlue()  / 255.0;

        double r = br * (ambientI + diffuseI) + lr * specularI;
        double g = bg * (ambientI + diffuseI) + lg * specularI;
        double b = bb * (ambientI + diffuseI) + lb * specularI;

        r = Math.min(1.0, Math.max(0, r));
        g = Math.min(1.0, Math.max(0, g));
        b = Math.min(1.0, Math.max(0, b));

        return new Color((int)(r * 255), (int)(g * 255), (int)(b * 255));
    }

    public BufferedImage render() {
        BufferedImage image = new BufferedImage(
                camera.width, camera.height, BufferedImage.TYPE_INT_RGB
        );

        for (int py = 0; py < camera.height; py++) {
            for (int px = 0; px < camera.width; px++) {
                Ray ray = camera.generateRay(px, py);
                Intersection hit = scene.closestIntersection(ray, camera);
                Color color = (hit != null) ? phongShade(hit, ray) : background;
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
        Color teapotColor = new Color(180, 100, 40);

        List<Triangle> teapot = OBJreader.loadOBJ(objPath, teapotColor);
        for (Triangle t : teapot) {
            t.setMaterial(0.15, 0.6, 0.8, 64);
            scene.add(t);
        }
        System.out.println("Triangles loaded: " + teapot.size());

        Color floorColor = new Color(60, 60, 70);
        Triangle floor1 = new Triangle(
                new Vector3D(-8, 0, -8),
                new Vector3D( 8, 0, -8),
                new Vector3D( 8, 0,  8),
                floorColor
        );
        Triangle floor2 = new Triangle(
                new Vector3D(-8, 0, -8),
                new Vector3D( 8, 0,  8),
                new Vector3D(-8, 0,  8),
                floorColor
        );

        floor1.setMaterial(0.1, 0.8, 0.0, 1);
        floor2.setMaterial(0.1, 0.8, 0.0, 1);
        scene.add(floor1);
        scene.add(floor2);

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

        File out = new File("output_v05.png");
        ImageIO.write(image, "png", out);
        System.out.println("Rendered → " + out.getAbsolutePath());
    }
}