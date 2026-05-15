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
    private List<Light> lights      = new ArrayList<>();

    private double ambientStrength  = 0.05;
    private static final double SHADOW_BIAS = 1e-4;
    private static final int    MAX_DEPTH   = 5;  // máximo de rebotes

    public Raytracer(Scene scene, Camera camera, Color background) {
        this.scene = scene; this.camera = camera; this.background = background;
    }

    public void addLight(Light light) { lights.add(light); }

    // ── Shadow ──────────────────────────────────────────────
    private boolean isInShadow(Vector3D point, Light light) {
        Vector3D dir    = light.directionFrom(point);
        Vector3D origin = point.add(dir.scale(SHADOW_BIAS));
        Ray      sRay   = new Ray(origin, dir);
        double   maxD   = light.distanceFrom(point);
        Intersection hit = scene.closestIntersection(sRay);
        // Objetos transparentes no bloquean la luz completamente
        if (hit != null && hit.t > SHADOW_BIAS && hit.t < maxD) {
            return hit.object.transparency < 0.5;
        }
        return false;
    }

    // ── Phong local ─────────────────────────────────────────
    private double[] localPhong(Intersection hit, Ray ray) {
        Object3D obj  = hit.object;
        Vector3D N    = hit.normal;
        Vector3D V    = ray.direction.scale(-1).normalize();
        Color    base = obj.getColor(hit.point);

        double br = base.getRed()   / 255.0;
        double bg = base.getGreen() / 255.0;
        double bb = base.getBlue()  / 255.0;

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
            double   diff  = obj.diffuse  * NdotL;
            double   spec  = obj.specular * Math.pow(RdotV, obj.shininess);
            double   lr    = light.color.getRed()   / 255.0;
            double   lg    = light.color.getGreen() / 255.0;
            double   lb    = light.color.getBlue()  / 255.0;
            double   li    = light.intensity * light.attenuation(hit.point);

            r += li * (diff * br * lr + spec * lr);
            g += li * (diff * bg * lg + spec * lg);
            b += li * (diff * bb * lb + spec * lb);
        }
        return new double[]{r, g, b};
    }

    // ── Traza un rayo recursivamente ────────────────────────
    private Color traceRay(Ray ray, int depth) {
        if (depth > MAX_DEPTH) return background;

        Intersection hit = scene.closestIntersection(ray, camera);
        if (hit == null) return background;

        Object3D obj = hit.object;
        Vector3D N   = hit.normal;

        // Color local (Phong)
        double[] local = localPhong(hit, ray);
        double lr = local[0], lg = local[1], lb = local[2];

        // ── Reflexión ──────────────────────────────────────
        double rr = lr, rg = lg, rb = lb;

        if (obj.reflectivity > 0 && depth < MAX_DEPTH) {
            Vector3D I    = ray.direction.normalize();
            Vector3D refl = I.subtract(N.scale(2 * I.dot(N))).normalize();
            Vector3D orig = hit.point.add(refl.scale(SHADOW_BIAS));
            Color reflColor = traceRay(new Ray(orig, refl), depth + 1);

            rr = lr * (1 - obj.reflectivity) + (reflColor.getRed()   / 255.0) * obj.reflectivity;
            rg = lg * (1 - obj.reflectivity) + (reflColor.getGreen() / 255.0) * obj.reflectivity;
            rb = lb * (1 - obj.reflectivity) + (reflColor.getBlue()  / 255.0) * obj.reflectivity;
        }

        if (obj.transparency > 0 && depth < MAX_DEPTH) {
            Vector3D I  = ray.direction.normalize();
            double cosI = -N.dot(I);
            boolean entering = cosI > 0;
            Vector3D n  = entering ? N : N.scale(-1);
            double   ni = entering ? 1.0 : obj.ior;
            double   nt = entering ? obj.ior : 1.0;
            double   ratio = ni / nt;
            double   cos2t = 1 - ratio * ratio * (1 - cosI * cosI);

            if (cos2t >= 0) { 
                Vector3D refractDir = I.scale(ratio)
                        .add(n.scale(ratio * Math.abs(cosI) - Math.sqrt(cos2t)))
                        .normalize();
                Vector3D refractOrig = hit.point.add(refractDir.scale(SHADOW_BIAS));
                Color refractColor = traceRay(new Ray(refractOrig, refractDir), depth + 1);

                double tr = obj.transparency;
                rr = rr * (1 - tr) + (refractColor.getRed()   / 255.0) * tr;
                rg = rg * (1 - tr) + (refractColor.getGreen() / 255.0) * tr;
                rb = rb * (1 - tr) + (refractColor.getBlue()  / 255.0) * tr;
            }
        }

        int ir = (int)(Math.min(1, Math.max(0, rr)) * 255);
        int ig = (int)(Math.min(1, Math.max(0, rg)) * 255);
        int ib = (int)(Math.min(1, Math.max(0, rb)) * 255);
        return new Color(ir, ig, ib);
    }

    public BufferedImage render() {
        BufferedImage image = new BufferedImage(
                camera.width, camera.height, BufferedImage.TYPE_INT_RGB);

        for (int py = 0; py < camera.height; py++) {
            if (py % 100 == 0) System.out.printf("Rendering... %.1f%%%n",
                    100.0 * py / camera.height);
            for (int px = 0; px < camera.width; px++) {
                Ray   ray   = camera.generateRay(px, py);
                Color color = traceRay(ray, 0);
                image.setRGB(px, py, color.getRGB());
            }
        }
        return image;
    }

    public static void main(String[] args) throws Exception {
        // ── Resolución: empieza en 800x600 para probar, sube a 3840x2160 para 4K ──
        int width  = 800;
        int height = 600;

        Scene scene = new Scene();

        // ── Piso checkerboard ──────────────────────────────
        CheckerPlane floor = new CheckerPlane(0,
                new Color(240, 240, 240),
                new Color(20,  20,  20),
                1.0);
        floor.setMaterial(0.1, 0.8, 0.3, 16);
        floor.setReflectivity(0.3);  // piso ligeramente reflectivo
        scene.add(floor);

        // ── Esfera espejo ──────────────────────────────────
        Sphere mirror = new Sphere(new Vector3D(-2.5, 1.2, -1), 1.2, new Color(200, 200, 210));
        mirror.setMaterial(0.05, 0.1, 0.9, 128);
        mirror.setReflectivity(0.95);
        scene.add(mirror);

        // ── Esfera de vidrio ───────────────────────────────
        Sphere glass = new Sphere(new Vector3D(0.5, 1.2, 0), 1.2, new Color(220, 235, 255));
        glass.setMaterial(0.02, 0.05, 0.9, 256);
        glass.setReflectivity(0.1);
        glass.setTransparency(0.92, 1.5);  // IOR del vidrio
        scene.add(glass);

        // ── Esfera roja mate (fondo) ────────────────────────
        Sphere redBall = new Sphere(new Vector3D(3.5, 1.0, -0.5), 1.0, new Color(200, 30, 30));
        redBall.setMaterial(0.1, 0.9, 0.1, 8);
        scene.add(redBall);

        // ── Tetera ─────────────────────────────────────────
        String objPath = "src/up/isgc/edu/utils/Teapot.obj";
        Color teapotColor = new Color(200, 210, 220); // plateada
        List<Triangle> teapot = OBJreader.loadOBJ(objPath, teapotColor);
        for (Triangle t : teapot) {
            t.setMaterial(0.05, 0.3, 0.9, 128);
            t.setReflectivity(0.6);
            scene.add(t);
        }
        System.out.println("Triangles loaded: " + teapot.size());

        // ── Cámara ─────────────────────────────────────────
        Camera camera = new Camera(
                new Vector3D(0, 3.5, 9), 50, width, height, 0.1, 60.0);

        Raytracer rt = new Raytracer(scene, camera, new Color(15, 15, 25));

        // ── Luces ──────────────────────────────────────────
        // Ventana (area light simulada con directional)
        rt.addLight(Light.directional(
                new Vector3D(-0.3, -1.0, -0.5), new Color(255, 250, 240), 1.2));
        // Luz cálida lateral derecha
        rt.addLight(Light.point(
                new Vector3D(6, 5, 4), new Color(255, 200, 120), 1.5));
        rt.addLight(Light.directional(
                new Vector3D(1.0, -0.3, 0.5), new Color(100, 130, 200), 0.3));

        System.out.println("Rendering " + width + "x" + height + "...");
        long start = System.currentTimeMillis();
        BufferedImage image = rt.render();
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("Done in %.1fs%n", elapsed / 1000.0);

        File out = new File("output_v1.png");
        ImageIO.write(image, "png", out);
        System.out.println("Saved → " + out.getAbsolutePath());
    }
}