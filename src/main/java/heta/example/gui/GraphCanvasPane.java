package heta.example.gui;

import heta.example.Graph;
import heta.example.IGraph;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Интерактивный холст: отрисовка графа, перетаскивание вершин,
 * клик для размещения новой вершины.
 */
public class GraphCanvasPane extends StackPane {

    // ---------- константы ----------
    private static final double NODE_RADIUS = 18;
    private static final double CURVE_OFFSET = 28;
    private static final Color EDGE_COLOR = Color.web("#555");
    private static final Color HIGHLIGHT_PATH = Color.web("#c62828");
    private static final Color HIGHLIGHT_MST = Color.web("#2e7d32");
    private static final Color HIGHLIGHT_FLOW = Color.web("#e65100");
    private static final Color HIGHLIGHT_NODE = Color.web("#1565c0");
    private static final Color NODE_FILL = Color.web("#e3f2fd");
    private static final Color NODE_STROKE = Color.web("#1565c0");

    private double panX = 0;   // смещение просмотра по X
    private double panY = 0;   // смещение просмотра по Y
    private double lastMouseX, lastMouseY;  // для перетаскивания средней кнопкой
    private boolean panning = false;        // флаг режима панорамирования

    // ---------- поля ----------
    private final Canvas canvas = new Canvas();
    private IGraph<String, Double> graph;
    private GraphLayout layout;
    private GraphHighlight highlight = new GraphHighlight();
    private String dragVertex;
    private Consumer<Point2D> pendingVertexPlacement;
    private BiConsumer<String, Point2D> onVertexMoved;

    /** Вспомогательная запись для рисования кратных рёбер */
    private record EdgeSlot(int index, int total) {}

    // ---------- конструктор ----------
    public GraphCanvasPane() {
        getChildren().add(canvas);

        // автоматический ресайз холста
        widthProperty().addListener((obs, o, n) -> {
            canvas.setWidth(n.doubleValue());
            redraw();
        });
        heightProperty().addListener((obs, o, n) -> {
            canvas.setHeight(n.doubleValue());
            redraw();
        });
        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                Point2D graphPt = toGraphCoords(e.getX(), e.getY());
                String vertex = findVertexAt(graphPt.getX(), graphPt.getY());
                if (vertex != null) {
                    // клик по вершине — перетаскивание вершины
                    dragVertex = vertex;
                } else {
                    // клик по пустому месту — панорамирование
                    panning = true;
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    canvas.setCursor(javafx.scene.Cursor.MOVE);
                }
                e.consume();
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (panning) {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;
                panX += dx;
                panY += dy;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                redraw();
                e.consume();
            } else if (dragVertex != null && layout != null) {
                Point2D graphPt = toGraphCoords(e.getX(), e.getY());
                layout.put(dragVertex, graphPt.getX(), graphPt.getY());
                if (onVertexMoved != null) {
                    onVertexMoved.accept(dragVertex, graphPt);
                }
                redraw();
                e.consume();
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (panning) {
                panning = false;
                canvas.setCursor(javafx.scene.Cursor.DEFAULT);
                e.consume();
            } else if (dragVertex != null) {
                dragVertex = null;
                e.consume();
            }
        });

        canvas.setOnMouseClicked(e -> {
            if (pendingVertexPlacement != null && e.getButton() == MouseButton.PRIMARY) {
                Point2D graphPt = toGraphCoords(e.getX(), e.getY());
                String vertex = findVertexAt(graphPt.getX(), graphPt.getY());
                if (vertex == null) {
                    // клик по пустому месту — размещаем новую вершину
                    pendingVertexPlacement.accept(graphPt);
                    pendingVertexPlacement = null;
                    setStyle(null);
                }
                e.consume();
            }
        });
    }

    // ---------- публичные методы ----------
    public void bindGraph(IGraph<String, Double> graph, GraphLayout layout) {
        this.graph = graph;
        this.layout = layout;
        if (graph != null && layout != null) {
            double cx = canvas.getWidth() > 0 ? canvas.getWidth() / 2 : 400;
            double cy = canvas.getHeight() > 0 ? canvas.getHeight() / 2 : 300;
            double r = Math.min(cx, cy) * 0.65;
            layout.ensureAllPresent(graph.getAdjacencyStructure().keySet(), cx, cy, r);
        }
        redraw();
    }

    public void setHighlight(GraphHighlight highlight) {
        this.highlight = highlight != null ? highlight : new GraphHighlight();
        redraw();
    }

    public GraphHighlight getHighlight() {
        return highlight;
    }

    public void setOnVertexMoved(BiConsumer<String, Point2D> handler) {
        this.onVertexMoved = handler;
    }

    public void awaitVertexPlacement(Consumer<Point2D> handler) {
        this.pendingVertexPlacement = handler;
        setStyle("-fx-cursor: crosshair;");
    }

    public void cancelVertexPlacement() {
        pendingVertexPlacement = null;
        setStyle(null);
    }

    // ---------- отрисовка ----------
    public void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.web("#fafafa"));
        gc.fillRect(0, 0, w, h);

// Если граф отсутствует – сообщение рисуем без смещения
        if (graph == null || layout == null) {
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font(14));
            gc.fillText("Создайте или загрузите граф", 20, 30);
            return;
        }

// Включаем смещение для всего, что относится к графу
        gc.save();
        gc.translate(panX, panY);

        boolean directed = graph.isDirected();
        boolean weighted = graph.isWeighted();
        Map<String, EdgeSlot> slotInfo = buildEdgeSlots(directed);

        // рёбра
        // Перед циклом по рёбрам вычислим нормали по каноническому направлению для каждой пары
        Map<String, Point2D> normals = new HashMap<>();
        for (Graph.Edge<String, Double> edge : graph.getEdgeList()) {
            String key = canonicalUndirected(edge.source, edge.dest);
            if (!normals.containsKey(key)) {
                // направление от меньшей вершины к большей
                String a = edge.source.compareTo(edge.dest) < 0 ? edge.source : edge.dest;
                String b = a.equals(edge.source) ? edge.dest : edge.source;
                Point2D pa = layout.get(a);
                Point2D pb = layout.get(b);
                if (pa != null && pb != null) {
                    normals.put(key, computeNormal(pa, pb));
                }
            }
        }

        for (Graph.Edge<String, Double> edge : graph.getEdgeList()) {
            Point2D from = layout.get(edge.source);
            Point2D to = layout.get(edge.dest);
            if (from == null || to == null) continue;

            GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(edge.source, edge.dest, directed);
            boolean hl = highlight.getHighlightedEdges().contains(key);
            Color hlColor = switch (highlight.getMode()) {
                case MST -> HIGHLIGHT_MST;
                case FLOW -> HIGHLIGHT_FLOW;
                default -> HIGHLIGHT_PATH;
            };
            gc.setStroke(hl ? hlColor : EDGE_COLOR);
            gc.setLineWidth(hl ? 3 : 1.5);

            EdgeSlot slot = slotInfo.getOrDefault(edge.source + "->" + edge.dest, new EdgeSlot(0, 1));
            Point2D baseNormal = null;
            if (!edge.source.equals(edge.dest)) {
                baseNormal = normals.get(canonicalUndirected(edge.source, edge.dest));
                if (baseNormal == null) baseNormal = new Point2D(0, 0);
            }

            if (edge.source.equals(edge.dest)) {
                drawSelfLoop(gc, from, directed);
            } else {
                drawEdge(gc, from, to, directed, slot.index(), slot.total(), baseNormal);
            }

            String extra = highlight.getEdgeLabels().get(key);
            String label = extra != null ? extra : (weighted ? formatWeight(edge.weight) : null);


            if (label != null) {
                EdgeGeometry geom = null;
                if (!edge.source.equals(edge.dest)) {
                    geom = computeEdgeGeometry(from, to, baseNormal, slot.index(), slot.total());
                }
                Point2D labelPos = labelPosition(from, to, edge.source.equals(edge.dest),
                        slot.index(), slot.total(), baseNormal, geom);
                if (edge.source.equals(edge.dest)) {
                    double tw = textWidth(label);
                    labelPos = new Point2D(labelPos.getX() - tw / 2, labelPos.getY());
                }
                gc.setFill(Color.web("#333"));
                gc.setFont(Font.font(11));
                gc.fillText(label, labelPos.getX(), labelPos.getY());
            }
        }

        // вершины
        for (String v : graph.getAdjacencyStructure().keySet()) {
            Point2D p = layout.get(v);
            if (p == null) continue;

            boolean hl = highlight.getHighlightedVertices().contains(v);
            gc.setFill(hl ? HIGHLIGHT_NODE : NODE_FILL);
            gc.setStroke(hl ? HIGHLIGHT_NODE : NODE_STROKE);
            gc.setLineWidth(hl ? 2.5 : 1.5);
            gc.fillOval(p.getX() - NODE_RADIUS, p.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);
            gc.strokeOval(p.getX() - NODE_RADIUS, p.getY() - NODE_RADIUS, NODE_RADIUS * 2, NODE_RADIUS * 2);

            gc.setFill(Color.web("#111"));
            gc.setFont(Font.font(12));
            double tw = textWidth(v);
            gc.fillText(v, p.getX() - tw / 2, p.getY() + 4);
        }
        gc.restore();                            // восстанавливаем состояние
    }

    // ---------- методы рисования ----------
    private void drawSelfLoop(GraphicsContext gc, Point2D center, boolean directed) {
        double loopR = 14;
        // центр дуги строго над вершиной
        double arcCenterX = center.getX();
        double arcCenterY = center.getY() - NODE_RADIUS;

        double x = arcCenterX - loopR;
        double y = arcCenterY - loopR;

        // дуга от 60° до 60° (против часовой), открытая снизу
        // 60 + 240 = 300° → разрыв между 300° и 60° = снизу
        gc.strokeArc(x, y, loopR * 2, loopR * 2, -30, 240, ArcType.OPEN);

        if (directed) {
            // конечный угол = -30 + 240 = 210°
            double endAngle = Math.toRadians(20);
            double ax = arcCenterX + loopR * Math.cos(endAngle);
            double ay = arcCenterY + loopR * Math.sin(endAngle);

            // точка раньше по дуге (уменьшаем угол, т.к. дуга против часовой)
            double beforeEndAngle = endAngle - Math.toRadians(15);
            double bx = arcCenterX + loopR * Math.cos(beforeEndAngle);
            double by = arcCenterY + loopR * Math.sin(beforeEndAngle);

            drawArrowHead(gc, bx, by, ax, ay);
        }
    }

    private void drawEdge(GraphicsContext gc, Point2D from, Point2D to,
                          boolean directed, int index, int total,
                          Point2D baseNormal) {  // нормаль для канонической пары
        double x1 = from.getX(), y1 = from.getY();
        double x2 = to.getX(), y2 = to.getY();
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return;

        double ux = dx / len, uy = dy / len;
        double shrink = NODE_RADIUS + 2;
        double sx = x1 + ux * shrink, sy = y1 + uy * shrink;
        double ex = x2 - ux * shrink, ey = y2 - uy * shrink;

        // Берём фиксированную нормаль (от канонического направления)
        double nx = baseNormal.getX();
        double ny = baseNormal.getY();

        double offsetFactor = total <= 1 ? 0 : (index - (total - 1) / 2.0);
        double offset = offsetFactor * CURVE_OFFSET;

        double mx = (sx + ex) / 2 + nx * offset;
        double my = (sy + ey) / 2 + ny * offset;

        if (Math.abs(offset) < 1e-6) {
            gc.strokeLine(sx, sy, ex, ey);
            if (directed) {
                drawArrowHead(gc, sx, sy, ex, ey);
            }
        } else {
            gc.beginPath();
            gc.moveTo(sx, sy);
            gc.quadraticCurveTo(mx, my, ex, ey);
            gc.stroke();
            if (directed) {
                double ax = ex - (ex - mx) * 0.35;
                double ay = ey - (ey - my) * 0.35;
                drawArrowHead(gc, ax, ay, ex, ey);
            }
        }
    }

    private Point2D computeNormal(Point2D from, Point2D to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return new Point2D(0, 0);
        // левая нормаль (поворот на +90°): (-dy, dx)
        return new Point2D(-dy / len, dx / len);
    }

    private static void drawArrowHead(GraphicsContext gc, double fromX, double fromY, double toX, double toY) {
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double head = 10;
        double a1 = angle + Math.PI * 0.85;
        double a2 = angle - Math.PI * 0.85;
        gc.strokeLine(toX, toY, toX + head * Math.cos(a1), toY + head * Math.sin(a1));
        gc.strokeLine(toX, toY, toX + head * Math.cos(a2), toY + head * Math.sin(a2));
    }

    private Point2D labelPosition(Point2D from, Point2D to, boolean selfLoop,
                                  int index, int total, Point2D baseNormal,
                                  EdgeGeometry geom) {
        if (selfLoop) {
            double loopR = 14;
            return new Point2D(from.getX(), from.getY() - NODE_RADIUS - loopR * 2 - 6);
        }

        // Если есть геометрия дуги (кривая), базируемся на контрольной точке
        if (geom != null && geom.offset() != 0) {
            // Смещаем контрольную точку чуть дальше по нормали, чтобы подпись была над дугой
            double nx = geom.nx();
            double ny = geom.ny();
            // Подпись: немного правее-выше от контрольной точки
            return new Point2D(geom.mx() + nx * 4 + 4, geom.my() + ny * 4 - 4);
        }

        // Для прямых рёбер (offset == 0) — старая логика
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return from;

        double nx, ny;
        if (baseNormal != null && total > 1) {
            nx = baseNormal.getX();
            ny = baseNormal.getY();
        } else {
            nx = -dy / len;
            ny = dx / len;
        }

        double offsetFactor = total <= 1 ? 0 : (index - (total - 1) / 2.0);
        double offset = offsetFactor * CURVE_OFFSET;

        return new Point2D(
                (from.getX() + to.getX()) / 2 + nx * offset + 4,
                (from.getY() + to.getY()) / 2 + ny * offset - 4
        );
    }

    // ---------- поиск вершины по координатам ----------
    private String findVertexAt(double x, double y) {
        if (graph == null) return null;
        for (String v : graph.getAdjacencyStructure().keySet()) {
            Point2D p = layout.get(v);
            if (p != null && p.distance(x, y) <= NODE_RADIUS + 4) {
                return v;
            }
        }
        return null;
    }
    private Point2D toGraphCoords(double x, double y) {
        return new Point2D(x - panX, y - panY);
    }
//    // ---------- обработчики мыши ----------
//    private void handlePress(MouseEvent e) {
//        if (graph == null || e.getButton() != MouseButton.PRIMARY) return;
//        Point2D graphPt = toGraphCoords(e.getX(), e.getY());
//        dragVertex = findVertexAt(graphPt.getX(), graphPt.getY());
//    }
//
//    private void handleDrag(MouseEvent e) {
//        if (dragVertex != null && layout != null) {
//            Point2D graphPt = toGraphCoords(e.getX(), e.getY());
//            layout.put(dragVertex, graphPt.getX(), graphPt.getY());
//            if (onVertexMoved != null) {
//                onVertexMoved.accept(dragVertex, graphPt); // тоже правильнее передать мировые координаты
//            }
//            redraw();
//        }
//    }
//
//    private void handleRelease(MouseEvent e) {
//        dragVertex = null;
//    }

    private void handleClick(MouseEvent e) {
        if (pendingVertexPlacement != null && e.getButton() == MouseButton.PRIMARY) {
            Point2D graphPt = toGraphCoords(e.getX(), e.getY());
            pendingVertexPlacement.accept(graphPt);
            pendingVertexPlacement = null;
            setStyle(null);
        }
    }

    private EdgeGeometry computeEdgeGeometry(Point2D from, Point2D to, Point2D baseNormal,
                                             int index, int total) {
        double x1 = from.getX(), y1 = from.getY();
        double x2 = to.getX(), y2 = to.getY();
        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return null;

        double ux = dx / len, uy = dy / len;
        double shrink = NODE_RADIUS + 2;
        double sx = x1 + ux * shrink, sy = y1 + uy * shrink;
        double ex = x2 - ux * shrink, ey = y2 - uy * shrink;

        double offsetFactor = total <= 1 ? 0 : (index - (total - 1) / 2.0);
        double offset = offsetFactor * CURVE_OFFSET;

        double nx = baseNormal.getX();
        double ny = baseNormal.getY();
        double mx = (sx + ex) / 2 + nx * offset;
        double my = (sy + ey) / 2 + ny * offset;

        return new EdgeGeometry(sx, sy, ex, ey, mx, my, offset, nx, ny);
    }

    // Вспомогательный record (добавьте в класс)
    private record EdgeGeometry(double sx, double sy, double ex, double ey,
                                double mx, double my, double offset,
                                double nx, double ny) {}

    // ---------- утилиты ----------
    private static String formatWeight(Double w) {
        if (w == null) return "";
        if (w == w.longValue()) return String.valueOf(w.longValue());

        String s = String.format("%.2f", w);

        if (s.contains(",")) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(",")) {
                s = s.substring(0, s.length() - 1);
            }
        } else if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    private static double textWidth(String text) {
        return new Text(text).getLayoutBounds().getWidth();
    }

    /** Канонический ключ для неориентированных рёбер */
    private static String canonicalUndirected(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /**
     * Подсчитывает для каждого мультиребра его индекс и общее количество,
     * чтобы аккуратно разнести кривые.
     */
    private Map<String, EdgeSlot> buildEdgeSlots(boolean directed) {
        Map<String, Integer> totalMap = new HashMap<>();      // сколько всего рёбер между парой
        Map<String, Integer> indexCounter = new HashMap<>();  // текущий индекс для пары
        Map<String, EdgeSlot> slots = new HashMap<>();

        // 1-й проход: считаем общее количество для каждой неупорядоченной пары
        for (Graph.Edge<String, Double> e : graph.getEdgeList()) {
            String undirectedKey = canonicalUndirected(e.source, e.dest);
            totalMap.merge(undirectedKey, 1, Integer::sum);
        }

        // 2-й проход: назначаем индексы конкретным направленным рёбрам
        for (Graph.Edge<String, Double> e : graph.getEdgeList()) {
            String undirectedKey = canonicalUndirected(e.source, e.dest);
            int total = totalMap.get(undirectedKey);
            int idx = indexCounter.getOrDefault(undirectedKey, 0);
            slots.put(e.source + "->" + e.dest, new EdgeSlot(idx, total));
            indexCounter.put(undirectedKey, idx + 1);
        }
        return slots;
    }
}