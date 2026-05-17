package heta.example.gui;
import heta.example.Graph;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/** Состояние подсветки для отрисовки результатов алгоритмов. */
public class GraphHighlight {

    public enum Mode {
        NONE, PATH, MST, FLOW
    }
    private Mode mode = Mode.NONE;
    private final Set<String> highlightedVertices = new HashSet<>();
    private final Set<EdgeKey> highlightedEdges = new HashSet<>();
    private final Map<EdgeKey, String> edgeLabels = new HashMap<>();
    public void clear() {
        mode = Mode.NONE;
        highlightedVertices.clear();
        highlightedEdges.clear();
        edgeLabels.clear();
    }
    public void setPath(List<String> path, boolean directed) {
        mode = Mode.PATH;
        highlightedVertices.clear();
        highlightedEdges.clear();
        if (path == null || path.isEmpty()) {
            return;
        }
        highlightedVertices.addAll(path);
        for (int i = 0; i < path.size() - 1; i++) {
            highlightedEdges.add(EdgeKey.of(path.get(i), path.get(i + 1), directed));
        }
    }
    public void addPath(List<String> path, boolean directed) {
        if (path == null || path.size() < 2) {
            return;
        }
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
            if (v.equals(source)) {
                continue;
            }
            List<String> path = result.buildPathTo(v);
            if (path.size() >= 2) {
                highlightedEdges.add(EdgeKey.of(path.get(path.size() - 2), path.get(path.size() - 1), directed));
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
    public record EdgeKey(String a, String b) {
        public static EdgeKey of(String u, String v, boolean directed) {
            if (directed) {
                return new EdgeKey(u, v);
            }
            if (u.compareTo(v) <= 0) {
                return new EdgeKey(u, v);
            }
            return new EdgeKey(v, u);
        }
    }
}