package up.isgc.edu;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Raytracer {

    private Scene scene;
    private Camera camera;
    private Color background;
    private List<Light> lights = new ArrayList<>();

    private double ambientStrength = 0.03;
    private static final double SHADOW_BIAS    = 1e-4;
    private static final int    MAX_DEPTH      = 3;  // sube a 6 en desktop
    private static final int    SHADOW_SAMPLES = 1;  // sube a 8 en desktop
    private static final int    AA_SAMPLES     = 1;  // sube a 4 en desktop

    public Raytracer(Scene scene, Camera camera, Color background) {
        this.scene = scene; this.camera = camera; this.background = background;
    }

    public void addLight(Light light) { lights.add(light); }

    private double shadowFactor(Vector3D point, Light light) {
        int visible = 0;
        Random rng = new Random();
        for (int i = 0; i < SHADOW_SAMPLES; i++) {
            Vector3D jitter = new Vector3D(
                    (rng.nextDouble() - 0.5) * 0.4,
                    (rng.nextDouble() - 0.5) * 0.4,
                    (rng.nextDouble() - 0.5) * 0.4);
            Vector3D dir    = light.directionFrom(point).add(jitter).normalize();
            Vector3D origin = point.add(dir.scale(SHADOW_BIAS));
            double   maxD   = light.distanceFrom(point);
            Intersection hit = scene.closestIntersection(new Ray(origin, dir));
            if (hit == null || hit.t < SHADOW_BIAS || hit.t > maxD
                    || hit.object.transparency > 0.5)
                visible++;
        }
        return visible / (double) SHADOW_SAMPLES;
    }

    private double[] localPhong(Intersection hit, Ray ray) {
        Object3D obj  = hit.object;
        Vector3D N    = hit.normal.normalize();
        Vector3D V    = ray.direction.scale(-1).normalize();
        Color    base = obj.getColor(hit.point);

        double br = base.getRed()   / 255.0;
        double bg = base.getGreen() / 255.0;
        double bb = base.getBlue()  / 255.0;

        double r = ambientStrength * obj.ambient * br;
        double g = ambientStrength * obj.ambient * bg;
        double b = ambientStrength * obj.ambient * bb;

        for (Light light : lights) {
            double shadow = shadowFactor(hit.point, light);
            if (shadow <= 0.001) continue;
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
            double   li    = light.intensity * light.attenuation(hit.point) * shadow;
            r += li * (diff * br * lr + spec * lr);
            g += li * (diff * bg * lg + spec * lg);
            b += li * (diff * bb * lb + spec * lb);
        }
        return new double[]{r, g, b};
    }

    private Color traceRay(Ray ray, int depth) {
        if (depth > MAX_DEPTH) return background;

        Intersection hit = scene.closestIntersection(ray, camera);
        if (hit == null) return background;

        Object3D obj = hit.object;
        Vector3D N   = hit.normal.normalize();
        double[] local = localPhong(hit, ray);
        double rr = local[0], rg = local[1], rb = local[2];

        // Reflexión + Fresnel
        if (obj.reflectivity > 0 && depth < MAX_DEPTH) {
            Vector3D I    = ray.direction.normalize();
            Vector3D refl = I.subtract(N.scale(2 * I.dot(N))).normalize();
            Color reflColor = traceRay(
                    new Ray(hit.point.add(refl.scale(SHADOW_BIAS)), refl), depth + 1);
            double fresnel = Math.pow(1.0 - Math.max(0, -I.dot(N)), 5.0);
            double rf = obj.reflectivity + (1.0 - obj.reflectivity) * fresnel;
            rr = rr * (1-rf) + (reflColor.getRed()   / 255.0) * rf;
            rg = rg * (1-rf) + (reflColor.getGreen() / 255.0) * rf;
            rb = rb * (1-rf) + (reflColor.getBlue()  / 255.0) * rf;
        }

        // Refracción
        if (obj.transparency > 0 && depth < MAX_DEPTH) {
            Vector3D I    = ray.direction.normalize();
            double cosI   = -N.dot(I);
            boolean entering = cosI > 0;
            Vector3D n    = entering ? N : N.scale(-1);
            double   ni   = entering ? 1.0 : obj.ior;
            double   nt   = entering ? obj.ior : 1.0;
            double   ratio = ni / nt;
            double   cos2t = 1 - ratio * ratio * (1 - cosI * cosI);
            if (cos2t >= 0) {
                Vector3D refractDir = I.scale(ratio)
                        .add(n.scale(ratio * Math.abs(cosI) - Math.sqrt(cos2t))).normalize();
                Color refractColor = traceRay(
                        new Ray(hit.point.add(refractDir.scale(SHADOW_BIAS)), refractDir), depth + 1);
                double tr = obj.transparency;
                rr = rr * (1-tr) + (refractColor.getRed()   / 255.0) * tr;
                rg = rg * (1-tr) + (refractColor.getGreen() / 255.0) * tr;
                rb = rb * (1-tr) + (refractColor.getBlue()  / 255.0) * tr;
            }
        }

        // Tone mapping + gamma
        rr = Math.pow(rr / (1.0 + rr), 1.0 / 2.2);
        rg = Math.pow(rg / (1.0 + rg), 1.0 / 2.2);
        rb = Math.pow(rb / (1.0 + rb), 1.0 / 2.2);

        return new Color(
                (int)(Math.min(1, Math.max(0, rr)) * 255),
                (int)(Math.min(1, Math.max(0, rg)) * 255),
                (int)(Math.min(1, Math.max(0, rb)) * 255));
    }

    public BufferedImage render() throws InterruptedException {
        BufferedImage image = new BufferedImage(
                camera.width, camera.height, BufferedImage.TYPE_INT_RGB);
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("Using " + threads + " threads");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger rowsDone = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int startY = 0; startY < camera.height; startY += 16) {
            final int y0 = startY;
            final int y1 = Math.min(startY + 16, camera.height);
            futures.add(pool.submit(() -> {
                Random rng = new Random();
                for (int py = y0; py < y1; py++) {
                    for (int px = 0; px < camera.width; px++) {
                        double r = 0, g = 0, b = 0;
                        for (int s = 0; s < AA_SAMPLES; s++) {
                            Color c = traceRay(camera.generateRay(
                                    px + rng.nextDouble(), py + rng.nextDouble()), 0);
                            r += c.getRed(); g += c.getGreen(); b += c.getBlue();
                        }
                        image.setRGB(px, py, new Color(
                                (int)(r/AA_SAMPLES), (int)(g/AA_SAMPLES), (int)(b/AA_SAMPLES)).getRGB());
                    }
                    int done = rowsDone.incrementAndGet();
                    if (done % 50 == 0 || done == camera.height)
                        System.out.printf("  %.1f%%%n", 100.0 * done / camera.height);
                }
            }));
        }
        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException e) { e.printStackTrace(); }
        }
        pool.shutdown();
        return image;
    }

    public static void main(String[] args) throws Exception {

        // ── Cambia a 3840x2160 en el desktop ──
        int width  = 800;
        int height = 450;

        Scene scene = new Scene();

        // ── PISO reflectivo, tiles grandes ────────────────
        CheckerPlane floor = new CheckerPlane(0,
                new Color(210, 210, 210), new Color(18, 18, 18), 2.5);
        floor.setMaterial(0.02, 0.7, 0.5, 128);
        floor.setReflectivity(0.75);
        scene.add(floor);

        // ── PARED TRASERA ─────────────────────────────────
        CheckerPlane backWall = new CheckerPlane(0,
                new Color(35, 35, 40), new Color(35, 35, 40), 999) {
            @Override
            public Intersection intersect(Ray ray) {
                // Plano vertical z = -8
                if (Math.abs(ray.direction.z) < 1e-8) return null;
                double t = (-8 - ray.origin.z) / ray.direction.z;
                if (t < 1e-4) return null;
                Vector3D point  = ray.pointAt(t);
                if (point.y < 0 || point.y > 12 || point.x < -12 || point.x > 12) return null;
                Vector3D normal = new Vector3D(0, 0, 1);
                return new Intersection(t, this, point, normal);
            }
        };
        backWall.setMaterial(0.05, 0.6, 0.0, 1);
        scene.add(backWall);

        // ── PARED IZQUIERDA ───────────────────────────────
        CheckerPlane leftWall = new CheckerPlane(0,
                new Color(30, 30, 35), new Color(30, 30, 35), 999) {
            @Override
            public Intersection intersect(Ray ray) {
                if (Math.abs(ray.direction.x) < 1e-8) return null;
                double t = (-10 - ray.origin.x) / ray.direction.x;
                if (t < 1e-4) return null;
                Vector3D point  = ray.pointAt(t);
                if (point.y < 0 || point.y > 12 || point.z < -8 || point.z > 15) return null;
                Vector3D normal = new Vector3D(1, 0, 0);
                return new Intersection(t, this, point, normal);
            }
        };
        leftWall.setMaterial(0.05, 0.6, 0.0, 1);
        scene.add(leftWall);

        // ── PARED DERECHA ─────────────────────────────────
        CheckerPlane rightWall = new CheckerPlane(0,
                new Color(30, 30, 35), new Color(30, 30, 35), 999) {
            @Override
            public Intersection intersect(Ray ray) {
                if (Math.abs(ray.direction.x) < 1e-8) return null;
                double t = (10 - ray.origin.x) / ray.direction.x;
                if (t < 1e-4) return null;
                Vector3D point  = ray.pointAt(t);
                if (point.y < 0 || point.y > 12 || point.z < -8 || point.z > 15) return null;
                Vector3D normal = new Vector3D(-1, 0, 0);
                return new Intersection(t, this, point, normal);
            }
        };
        rightWall.setMaterial(0.05, 0.6, 0.0, 1);
        scene.add(rightWall);

        // ── TECHO ─────────────────────────────────────────
        CheckerPlane ceiling = new CheckerPlane(0,
                new Color(28, 28, 32), new Color(28, 28, 32), 999) {
            @Override
            public Intersection intersect(Ray ray) {
                if (Math.abs(ray.direction.y) < 1e-8) return null;
                double t = (8 - ray.origin.y) / ray.direction.y;
                if (t < 1e-4) return null;
                Vector3D point  = ray.pointAt(t);
                if (point.z < -8 || point.z > 15 || point.x < -10 || point.x > 10) return null;
                Vector3D normal = new Vector3D(0, -1, 0);
                return new Intersection(t, this, point, normal);
            }
        };
        ceiling.setMaterial(0.05, 0.5, 0.0, 1);
        scene.add(ceiling);

        // ── VENTANA (quad blanco brillante en pared izq) ──
        // Simulada como una esfera aplastada muy plana y brillante
        // En realidad es una luz area — la aproximamos con un rect blanco
        CheckerPlane window = new CheckerPlane(0,
                new Color(255, 252, 240), new Color(255, 252, 240), 999) {
            @Override
            public Intersection intersect(Ray ray) {
                if (Math.abs(ray.direction.x) < 1e-8) return null;
                double t = (-9.9 - ray.origin.x) / ray.direction.x;
                if (t < 1e-4) return null;
                Vector3D point = ray.pointAt(t);
                // Ventana entre y=[2,6], z=[0,6]
                if (point.y < 2 || point.y > 6 || point.z < 0 || point.z > 6) return null;
                Vector3D normal = new Vector3D(1, 0, 0);
                return new Intersection(t, this, point, normal);
            }
        };
        window.setMaterial(1.0, 1.0, 0.0, 1); // emisiva
        scene.add(window);

        // ── PEDESTAL para la tetera ───────────────────────
        // Caja simple hecha de triángulos
        double px = 2.5, pz = -3.5, ph = 1.5, ps = 1.8;
        Color pedestalColor = new Color(15, 15, 18);
        // Pedestal top
        addQuad(scene, new Vector3D(px-ps,ph,pz-ps), new Vector3D(px+ps,ph,pz-ps),
                new Vector3D(px+ps,ph,pz+ps), new Vector3D(px-ps,ph,pz+ps),
                pedestalColor, 0.0, 0.9, 0.4, 64, 0.6);
        // Pedestal front
        addQuad(scene, new Vector3D(px-ps,0,pz+ps), new Vector3D(px+ps,0,pz+ps),
                new Vector3D(px+ps,ph,pz+ps), new Vector3D(px-ps,ph,pz+ps),
                pedestalColor, 0.0, 0.7, 0.3, 32, 0.3);
        // Pedestal right
        addQuad(scene, new Vector3D(px+ps,0,pz-ps), new Vector3D(px+ps,0,pz+ps),
                new Vector3D(px+ps,ph,pz+ps), new Vector3D(px+ps,ph,pz-ps),
                pedestalColor, 0.0, 0.7, 0.3, 32, 0.3);

        // ── ESFERA CHROME ─────────────────────────────────
        Sphere chrome = new Sphere(new Vector3D(-3.5, 1.8, 0), 1.8, new Color(220, 220, 230));
        chrome.setMaterial(0.01, 0.05, 1.0, 512);
        chrome.setReflectivity(0.98);
        scene.add(chrome);

        // ── ESFERA VIDRIO (centro) ────────────────────────
        Sphere glass = new Sphere(new Vector3D(0.3, 1.9, 1.5), 1.9, new Color(240, 245, 255));
        glass.setMaterial(0.01, 0.02, 1.0, 1024);
        glass.setReflectivity(0.06);
        glass.setTransparency(0.97, 1.52);
        scene.add(glass);

        // ── ESFERA ROJA (dentro del vidrio) ───────────────
        Sphere inner = new Sphere(new Vector3D(0.1, 1.1, 1.2), 0.6, new Color(185, 25, 25));
        inner.setMaterial(0.08, 0.92, 0.15, 32);
        scene.add(inner);

        // ── ESFERA ROJA GRANDE (derecha) ──────────────────
        Sphere redBig = new Sphere(new Vector3D(4.8, 1.6, 1.0), 1.6, new Color(185, 35, 25));
        redBig.setMaterial(0.06, 0.95, 0.08, 16);
        scene.add(redBig);

        // ── TETERA DE VIDRIO (sobre pedestal) ─────────────
        List<Triangle> teapot = OBJreader.loadOBJ(
                "src/up/isgc/edu/utils/Teapot.obj", new Color(235, 238, 245));
        for (Triangle t : teapot) {
            Vector3D v0 = new Vector3D(t.v0.x * 0.55 + 2.5, t.v0.y * 0.55 + 1.5, t.v0.z * 0.55 - 4.0);
            Vector3D v1 = new Vector3D(t.v1.x * 0.55 + 2.5, t.v1.y * 0.55 + 1.5, t.v1.z * 0.55 - 4.0);
            Vector3D v2 = new Vector3D(t.v2.x * 0.55 + 2.5, t.v2.y * 0.55 + 1.5, t.v2.z * 0.55 - 4.0);
            Triangle scaled = new Triangle(v0, v1, v2, new Color(235, 238, 245));
            scaled.setMaterial(0.01, 0.03, 1.0, 512);
            scaled.setReflectivity(0.12);
            scaled.setTransparency(0.88, 1.5);
            scene.add(scaled);
        }
        System.out.println("Triangles: " + teapot.size());

        // ── CÁMARA ────────────────────────────────────────
        Camera camera = new Camera(
                new Vector3D(0.5, 3.2, 11.5), 48, width, height, 0.1, 100.0);

        Raytracer rt = new Raytracer(scene, camera, new Color(5, 5, 8));

        // ── LUCES cinemáticas ─────────────────────────────
        // Ventana principal (izquierda, blanca, fuerte)
        rt.addLight(Light.point(
                new Vector3D(-9, 5, 3), new Color(255, 252, 235), 20.0));
        // Luz cálida lateral derecha (tipo candle/tungsten)
        rt.addLight(Light.point(
                new Vector3D(9, 5, -2), new Color(255, 180, 90), 12.0));
        // Rim light trasera azulada
        rt.addLight(Light.point(
                new Vector3D(-2, 7, -10), new Color(160, 200, 255), 6.0));
        // Fill suave desde arriba
        rt.addLight(Light.directional(
                new Vector3D(0, -1, -0.2), new Color(100, 120, 180), 0.5));

        System.out.println("Rendering " + width + "x" + height + "...");
        long start = System.currentTimeMillis();
        BufferedImage image = rt.render();
        System.out.printf("Done in %.2fs%n", (System.currentTimeMillis() - start) / 1000.0);

        File out = new File("cinematic_render.png");
        ImageIO.write(image, "png", out);
        System.out.println("Saved -> " + out.getAbsolutePath());
    }

    // ── Helper: quad = 2 triángulos ──────────────────────────
    private static void addQuad(Scene scene,
                                Vector3D a, Vector3D b, Vector3D c, Vector3D d,
                                Color color, double ka, double kd, double ks, double n, double refl) {
        Triangle t1 = new Triangle(a, b, c, color);
        Triangle t2 = new Triangle(a, c, d, color);
        t1.setMaterial(ka, kd, ks, n); t1.setReflectivity(refl);
        t2.setMaterial(ka, kd, ks, n); t2.setReflectivity(refl);
        scene.add(t1); scene.add(t2);
    }
}