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
        List<Triangle> triangles = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");

                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);

                    vertices.add(new Vector3D(x, y, z));
                }

                else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");

                    for (int i = 2; i < parts.length - 1; i++) {
                        int index0 = getVertexIndex(parts[1]);
                        int index1 = getVertexIndex(parts[i]);
                        int index2 = getVertexIndex(parts[i + 1]);

                        Vector3D v0 = vertices.get(index0);
                        Vector3D v1 = vertices.get(index1);
                        Vector3D v2 = vertices.get(index2);

                        triangles.add(new Triangle(v0, v1, v2, color));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading OBJ file: " + e.getMessage());
        }

        return triangles;
    }

    private static int getVertexIndex(String faceData) {
        String[] data = faceData.split("/");
        return Integer.parseInt(data[0]) - 1;
    }
}
