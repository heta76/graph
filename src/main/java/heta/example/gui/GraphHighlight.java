package heta.example.gui;

import heta.example.Graph;
import java.util.*;

/** Состояние подсветки для отрисовки результатов алгоритмов. */
public class GraphHighlight {

    public enum Mode {
        NONE, PATH, MST, FLOW
    }

    private Mode mode = Mode.NONE;
    private final Set<String> highlightedVertices = new HashSet<>();
    private final Set<EdgeKey> highlightedEdges = new HashSet<>();
    private final Map<EdgeKey, String> edgeLabels = new HashMap<>();
    private final Set<EdgeKey> path1Edges = new HashSet<>();
    private final Set<EdgeKey> path2Edges = new HashSet<>();
    // ---------- конструкторы ----------
    public GraphHighlight() {}

    /** Конструктор копирования */
    public GraphHighlight(GraphHighlight other) {
        this.mode = other.mode;
        this.highlightedVertices.addAll(other.highlightedVertices);
        this.highlightedEdges.addAll(other.highlightedEdges);
        this.edgeLabels.putAll(other.edgeLabels);
        this.path1Edges.addAll(other.path1Edges);   // ←
        this.path2Edges.addAll(other.path2Edges);   // ←
    }

    // ---------- очистка ----------
    public void clear() {
        mode = Mode.NONE;
        highlightedVertices.clear();
        highlightedEdges.clear();
        edgeLabels.clear();
        path1Edges.clear();   // ←
        path2Edges.clear();   // ←
    }

    // ---------- установка подсветки ----------
    public void setPath(List<String> path, boolean directed) {
        mode = Mode.PATH;
        highlightedVertices.clear();
        highlightedEdges.clear();
        if (path == null || path.isEmpty()) return;
        highlightedVertices.addAll(path);
        for (int i = 0; i < path.size() - 1; i++) {
            highlightedEdges.add(EdgeKey.of(path.get(i), path.get(i + 1), directed));
        }
    }

    public void addPath(List<String> path, boolean directed) {
        if (path == null || path.size() < 2) return;
        mode = Mode.PATH;
        highlightedVertices.addAll(path);
        for (int i = 0; i < path.size() - 1; i++) {
            highlightedEdges.add(EdgeKey.of(path.get(i), path.get(i + 1), directed));
        }
    }

    public void setShortestPathTree(Graph.ShortestPathResult<String> result, boolean directed) {
        clear();
        mode = Mode.PATH;
        String source = result.getSource();
        highlightedVertices.add(source);
        for (String v : result.getDistances().keySet()) {
            if (v.equals(source)) continue;
            List<String> path = result.buildPathTo(v);
            if (path.size() >= 2) {
                highlightedEdges.add(EdgeKey.of(path.get(path.size() - 2), path.get(path.size() - 1), directed));
                highlightedVertices.addAll(path);               // <-- ДОБАВИТЬ
            }
        }
    }

    public void setEdges(Set<EdgeKey> edges) {
        mode = Mode.MST;
        highlightedEdges.clear();
        highlightedEdges.addAll(edges);
    }

    public void setFlowEdges(Map<String, Map<String, Double>> flows, boolean directed) {
        highlightedEdges.clear();
        mode = Mode.FLOW;
        for (var entry : flows.entrySet()) {
            String u = entry.getKey();
            for (var e : entry.getValue().entrySet()) {
                if (Math.abs(e.getValue()) > 1e-12) {
                    highlightedEdges.add(EdgeKey.of(u, e.getKey(), directed));
                }
            }
        }
    }

    public void setEdgeLabels(Map<EdgeKey, String> labels) {
        edgeLabels.clear();
        edgeLabels.putAll(labels);
        if (!labels.isEmpty() && mode == Mode.NONE) {
            mode = Mode.FLOW;
        }
    }

    // ---------- методы для анимации (модификация) ----------
    public void addHighlightedVertex(String v) {
        highlightedVertices.add(v);
    }

    public void addHighlightedEdge(EdgeKey key) {
        highlightedEdges.add(key);
    }

    public void addHighlightedVertices(Set<String> vertices) {
        highlightedVertices.addAll(vertices);
    }

    public void addHighlightedEdges(Set<EdgeKey> edges) {
        highlightedEdges.addAll(edges);
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void addPath1Edge(EdgeKey key) {
        path1Edges.add(key);
    }

    public void addPath2Edge(EdgeKey key) {
        path2Edges.add(key);
    }

    public boolean isPath1Edge(EdgeKey key) {
        return path1Edges.contains(key);
    }

    public boolean isPath2Edge(EdgeKey key) {
        return path2Edges.contains(key);
    }
// в класс GraphHighlight

    public Set<EdgeKey> getPath1Edges() {
        return Collections.unmodifiableSet(path1Edges);
    }

    public Set<EdgeKey> getPath2Edges() {
        return Collections.unmodifiableSet(path2Edges);
    }

    public void addPath1Edges(Set<EdgeKey> edges) {
        path1Edges.addAll(edges);
    }

    public void addPath2Edges(Set<EdgeKey> edges) {
        path2Edges.addAll(edges);
    }
    // ---------- геттеры ----------
    public Mode getMode() {
        return mode;
    }

    public Set<String> getHighlightedVertices() {
        return Collections.unmodifiableSet(highlightedVertices);
    }

    public Set<EdgeKey> getHighlightedEdges() {
        return Collections.unmodifiableSet(highlightedEdges);
    }

    public Map<EdgeKey, String> getEdgeLabels() {
        return Collections.unmodifiableMap(edgeLabels);
    }

    // ---------- EdgeKey ----------
    public record EdgeKey(String a, String b) {
        public static EdgeKey of(String u, String v, boolean directed) {
            if (directed) return new EdgeKey(u, v);
            if (u.compareTo(v) <= 0) return new EdgeKey(u, v);
            return new EdgeKey(v, u);
        }
    }
}