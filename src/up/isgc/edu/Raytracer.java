package up.isgc.edu;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class Raytracer {
    private Scene       scene;
    private Camera      camera;
    private Color       background;
    private List<Light> lights = new ArrayList<>();

    private double ambientStrength = 0.05;
    private static final double SHADOW_BIAS = 1e-4; // evita self-shadowing

    public Raytracer(Scene scene, Camera camera, Color background) {
        this.scene      = scene;
        this.camera     = camera;
        this.background = background;
    }

    public void addLight(Light light) { lights.add(light); }
    private boolean isInShadow(Vector3D point, Light light) {
        Vector3D dir          = light.directionFrom(point);
        Vector3D shadowOrigin = point.add(dir.scale(SHADOW_BIAS));
        Ray      shadowRay    = new Ray(shadowOrigin, dir);
        double   maxDist      = light.distanceFrom(point);

        Intersection hit = scene.closestIntersection(shadowRay);
        return hit != null && hit.t > SHADOW_BIAS && hit.t < maxDist;
    }
    private Color shade(Intersection hit, Ray ray) {
        Object3D obj = hit.object;
        Vector3D N   = hit.normal;
        Vector3D V   = ray.direction.scale(-1).normalize();

        Color base = obj.color;
        double br  = base.getRed()   / 255.0;
        double bg  = base.getGreen() / 255.0;
        double bb  = base.getBlue()  / 255.0;


        double r = ambientStrength * obj.ambient * br;
        double g = ambientStrength * obj.ambient * bg;
        double b = ambientStrength * obj.ambient * bb;

        for (Light light : lights) {

            if (isInShadow(hit.point, light)) continue;

            Vector3D L     = light.directionFrom(hit.point);
            double   NdotL = Math.max(0, N.dot(L));
            if (NdotL <= 0) continue;

            Vector3D R     = N.scale(2 * NdotL).subtract(L).normalize();
            double   RdotV = Math.max(0, R.dot(V));

            double diffuse  = obj.diffuse  * NdotL;
            double specular = obj.specular * Math.pow(RdotV, obj.shininess);

            double lr   = light.color.getRed()   / 255.0;
            double lg   = light.color.getGreen() / 255.0;
            double lb   = light.color.getBlue()  / 255.0;
            double li   = light.intensity;
            double att  = light.attenuation(hit.point);

            r += li * att * (diffuse * br * lr + specular * lr);
            g += li * att * (diffuse * bg * lg + specular * lg);
            b += li * att * (diffuse * bb * lb + specular * lb);
        }

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
            if (py % 50 == 0) System.out.println("Rendering row " + py + "/" + camera.height);
            for (int px = 0; px < camera.width; px++) {
                Ray ray = camera.generateRay(px, py);
                Intersection hit = scene.closestIntersection(ray, camera);
                Color color = (hit != null) ? shade(hit, ray) : background;
                image.setRGB(px, py, color.getRGB());
            }
        }

        return image;
    }

    public static void main(String[] args) throws Exception {
        int width  = 800;
        int height = 600;

        Scene scene = new Scene();

        // --- Teapot ---
        String objPath = "src/up/isgc/edu/utils/Teapot.obj";
        Color teapotColor = new Color(180, 100, 40);

        List<Triangle> teapot = OBJreader.loadOBJ(objPath, teapotColor);
        for (Triangle t : teapot) {
            t.setMaterial(0.1, 0.7, 0.6, 64);
            scene.add(t);
        }
        System.out.println("Triangles loaded: " + teapot.size());

        // --- Piso ---
        Color floorColor = new Color(60, 60, 70);
        Triangle floor1 = new Triangle(
                new Vector3D(-8, 0, -8), new Vector3D(8, 0, -8), new Vector3D(8, 0, 8), floorColor);
        Triangle floor2 = new Triangle(
                new Vector3D(-8, 0, -8), new Vector3D(8, 0, 8), new Vector3D(-8, 0, 8), floorColor);
        floor1.setMaterial(0.1, 0.9, 0.0, 1);
        floor2.setMaterial(0.1, 0.9, 0.0, 1);
        scene.add(floor1);
        scene.add(floor2);

        // --- Cámara ---
        Camera camera = new Camera(
                new Vector3D(0.2, 3.5, 10), 45, width, height, 0.1, 50.0
        );

        Raytracer rt = new Raytracer(scene, camera, new Color(20, 20, 30));

        rt.addLight(Light.directional(
                new Vector3D(0.0, -1.0, -1.0), Color.WHITE, 1.1
        ));
        rt.addLight(Light.directional(
                new Vector3D(1.0, -0.5, 0.0), new Color(100, 150, 255), 0.5
        ));
        rt.addLight(Light.point(
                new Vector3D(0.0, 5.0, 6.0), new Color(255, 220, 180), 0.9
        ));

        System.out.println("Rendering...");
        BufferedImage image = rt.render();

        File out = new File("output_v07.png");
        ImageIO.write(image, "png", out);
        System.out.println("Rendered → " + out.getAbsolutePath());
    }
}