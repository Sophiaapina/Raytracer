package up.isgc.edu;

import java.util.ArrayList;
import java.util.List;

public class Scene {
    private List<Object3D> objects = new ArrayList<>();

    public void add(Object3D obj) {
        objects.add(obj);
    }

    public Intersection closestIntersection(Ray ray) {
        Intersection closest = null;

        for (Object3D obj : objects) {
            Intersection hit = obj.intersect(ray);
            if (hit != null) {
                if (closest == null || hit.t < closest.t) {
                    closest = hit;
                }
            }
        }

        return closest;
    }
}