package heta.example.gui;
import javafx.geometry.Point2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
/** Сохранение и загрузка координат вершин рядом с файлом графа (*.layout). */
public final class GraphLayoutStorage {
    private static final String SUFFIX = ".layout";
    private GraphLayoutStorage() {
    }
    public static Path layoutPathFor(String graphFilePath) {
        return Path.of(graphFilePath + SUFFIX);
    }
    public static void save(String graphFilePath, GraphLayout layout) throws IOException {
        if (graphFilePath == null || graphFilePath.isBlank()) {
            return;
        }
        StringBuilder sb = new StringBuilder("# layout coordinates (vertex x y)\n");
        for (Map.Entry<String, Point2D> e : layout.getPositions().entrySet()) {
            Point2D p = e.getValue();
            sb.append(e.getKey()).append(' ')
                    .append(format(p.getX())).append(' ')
                    .append(format(p.getY())).append('\n');
        }
        Files.writeString(layoutPathFor(graphFilePath), sb.toString());
    }
    public static boolean load(String graphFilePath, GraphLayout layout, Set<String> vertices) throws IOException {
        Path path = layoutPathFor(graphFilePath);
        if (!Files.exists(path)) {
            return false;
        }
        layout.clear();
        boolean any = false;
        for (String line : Files.readAllLines(path)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 3) {
                continue;
            }
            String name = parts[0];
            if (vertices != null && !vertices.contains(name)) {
                continue;
            }
            try {
                double x = Double.parseDouble(parts[1].replace(',', '.'));
                double y = Double.parseDouble(parts[2].replace(',', '.'));
                layout.put(name, x, y);
                any = true;
            } catch (NumberFormatException ignored) {
                // пропускаем битую строку
            }
        }
        return any;
    }
    private static String format(double v) {
        if (v == (long) v) {
            return String.valueOf((long) v);
        }
        return String.format("%.2f", v);
    }
}