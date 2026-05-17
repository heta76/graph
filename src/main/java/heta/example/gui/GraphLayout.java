package heta.example.gui;

import javafx.geometry.Point2D;
import java.util.*;

public class GraphLayout {
    private final Map<String, Point2D> positions = new HashMap<>();

    public Map<String, Point2D> getPositions() { return positions; }
    public Point2D get(String vertex) { return positions.get(vertex); }
    public void put(String vertex, double x, double y) { positions.put(vertex, new Point2D(x, y)); }
    public void remove(String vertex) { positions.remove(vertex); }
    public void clear() { positions.clear(); }

    public void layoutCircle(Set<String> vertices, double centerX, double centerY, double radius) {
        List<String> list = new ArrayList<>(vertices);
        int n = list.size();
        if (n == 0) return;
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n - Math.PI / 2;
            String v = list.get(i);
            positions.put(v, new Point2D(
                    centerX + radius * Math.cos(angle),
                    centerY + radius * Math.sin(angle)
            ));
        }
    }

    public void ensureAllPresent(Set<String> vertices, double centerX, double centerY, double radius) {
        boolean missing = false;
        for (String v : vertices) {
            if (!positions.containsKey(v)) {
                missing = true;
                break;
            }
        }
        if (missing || positions.size() != vertices.size()) {
            layoutCircle(vertices, centerX, centerY, radius);
        }
    }
}