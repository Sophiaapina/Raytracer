package up.isgc.edu;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OBJreader {

    public static List<Triangle> loadOBJ(String filePath, Color color) {
        List<Vector3D> vertices = new ArrayList<>();
        List<Vector3D> normals  = new ArrayList<>();
        List<Triangle> triangles = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("vn ")) {
                    String[] p = line.split("\\s+");
                    normals.add(new Vector3D(
                            Double.parseDouble(p[1]),
                            Double.parseDouble(p[2]),
                            Double.parseDouble(p[3])
                    ));
                } else if (line.startsWith("v ")) {
                    String[] p = line.split("\\s+");
                    vertices.add(new Vector3D(
                            Double.parseDouble(p[1]),
                            Double.parseDouble(p[2]),
                            Double.parseDouble(p[3])
                    ));
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    for (int i = 2; i < parts.length - 1; i++) {
                        int[] f0 = parseFace(parts[1]);
                        int[] f1 = parseFace(parts[i]);
                        int[] f2 = parseFace(parts[i + 1]);

                        Vector3D v0 = vertices.get(f0[0]);
                        Vector3D v1 = vertices.get(f1[0]);
                        Vector3D v2 = vertices.get(f2[0]);

                        Vector3D n0 = f0[2] >= 0 ? normals.get(f0[2]) : null;
                        Vector3D n1 = f1[2] >= 0 ? normals.get(f1[2]) : null;
                        Vector3D n2 = f2[2] >= 0 ? normals.get(f2[2]) : null;

                        triangles.add(new Triangle(v0, v1, v2, n0, n1, n2, color));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading OBJ file: " + e.getMessage());
        }

        return triangles;
    }

    private static int[] parseFace(String token) {
        String[] parts = token.split("/");
        int vi = Integer.parseInt(parts[0]) - 1;
        int ti = -1;
        int ni = -1;
        if (parts.length > 1 && !parts[1].isEmpty()) ti = Integer.parseInt(parts[1]) - 1;
        if (parts.length > 2 && !parts[2].isEmpty()) ni = Integer.parseInt(parts[2]) - 1;
        return new int[]{vi, ti, ni};
    }
}