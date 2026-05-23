package heta.example.gui;
import heta.example.Graph;
import heta.example.IGraph;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;
import java.io.File;
import java.util.StringJoiner;
/** Связывает кнопки GUI с методами {@link IGraph} и {@link Graph}. */
public class GraphGuiController {
    private final GraphCanvasPane canvas;
    private final GraphLayout layout = new GraphLayout();
    private final GraphHighlight highlight = new GraphHighlight();
    private final TextArea output;
    private IGraph<String, Double> graph;
    private String currentGraphPath;
    private boolean dirty;
    private boolean layoutDirty;
    private static File lastDirectory = null;
    private int animationId = 0;   // идентификатор текущей анимации


    // --- АНИМАЦИЯ ---
    private List<GraphHighlight> lastAnimationSteps; // сохранённые шаги для повтора
    private int animationDelay = 200;                 // задержка в мс (по умолчанию)
    private boolean animationEnabled = true;          // включена ли анимация
    private volatile boolean animationPaused = false;
    private final Object pauseLock = new Object();

    public GraphGuiController(GraphCanvasPane canvas, TextArea output) {
        this.canvas = canvas;
        this.output = output;
        canvas.setHighlight(highlight);
        canvas.setOnVertexMoved((v, p) -> {
            layoutDirty = true;
            markDirty();
        });
    }
    public IGraph<String, Double> getGraph() {
        return graph;
    }
    public boolean isDirty() {
        return dirty;
    }
    private Window window() {
        return canvas.getScene() != null ? canvas.getScene().getWindow() : null;
    }
    private void markDirty() {
        dirty = true;
    }



    private void saveLayoutIfPossible() {
        if (currentGraphPath == null || !layoutDirty) {
            return;
        }
        try {
            GraphLayoutStorage.save(currentGraphPath, layout);
            layoutDirty = false;
        } catch (IOException e) {
            appendOutput("Не удалось сохранить раскладку: " + e.getMessage());
        }
    }
    private void loadLayoutForGraph(String graphPath) {
        try {
            if (graph != null && GraphLayoutStorage.load(graphPath, layout,
                    graph.getAdjacencyStructure().keySet())) {
                layoutDirty = false;
                appendOutput("Загружена сохранённая раскладка вершин.");
            }
        } catch (IOException e) {
            appendOutput("Не удалось загрузить раскладку: " + e.getMessage());
        }
    }
    private void refreshCanvas() {
        if (graph != null) {
            double cx = canvas.getWidth() > 0 ? canvas.getWidth() / 2 : 400;
            double cy = canvas.getHeight() > 0 ? canvas.getHeight() / 2 : 300;
            double r = Math.min(cx, cy) * 0.65;
            layout.ensureAllPresent(graph.getAdjacencyStructure().keySet(), cx, cy, r);
        }
        canvas.bindGraph(graph, layout);
    }
    private void clearHighlight() {
        highlight.clear();
        canvas.setHighlight(highlight);
        canvas.redraw();
    }
    private void appendOutput(String text) {
        output.appendText(text);
        if (!text.endsWith("\n")) {
            output.appendText("\n");
        }
    }
    private void showError(String message) {
        appendOutput("Ошибка: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
    private Optional<String> askString(String title, String prompt, String defaultValue) {
        TextInputDialog d = new TextInputDialog(defaultValue);
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(prompt);
        return d.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }
    private Optional<String> pickVertex(String title, String prompt) {
        if (graph == null || graph.getAdjacencyStructure().isEmpty()) {
            showError("Граф пуст.");
            return Optional.empty();
        }
        List<String> vertices = new ArrayList<>(graph.getAdjacencyStructure().keySet());
        ChoiceDialog<String> d = new ChoiceDialog<>(vertices.getFirst(), vertices);
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(prompt);
        return d.showAndWait();
    }
    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
    private Graph<String, Double> asGraph() {
        return (Graph<String, Double>) graph;
    }
    private void requireGraph() {
        if (graph == null) {
            throw new IllegalStateException("Сначала создайте или загрузите граф.");
        }
    }
    // --- Инициализация ---
    public void createNewGraph() {
        if (graph != null && dirty && !confirm("Есть несохранённые изменения. Создать новый граф?")) {
            return;
        }
        boolean directed = confirm("Ориентированный граф? (Да = directed, Нет = undirected)");
        boolean weighted = confirm("Взвешенный граф? (Да = weighted)");
        graph = new Graph<>(directed, weighted);
        currentGraphPath = null;
        dirty = false;
        layoutDirty = false;
        layout.clear();
        clearHighlight();
        output.clear();
        appendOutput("Создан " + (directed ? "ориентированный " : "неориентированный ")
                + (weighted ? "взвешенный" : "невзвешенный") + " граф.");
        refreshCanvas();
    }

    public void setAnimationEnabled(boolean enabled) {
        this.animationEnabled = enabled;
    }

    public void setAnimationDelay(int delay) {
        this.animationDelay = delay;
    }

    public void replayAnimation() {
        if (lastAnimationSteps != null && !lastAnimationSteps.isEmpty()) {
            highlight.clear();               // <-- полная очистка текущей подсветки
            canvas.setHighlight(highlight);
            canvas.redraw();
            animationId++; // новое воспроизведение
            int currentId = animationId;
            animationPaused = false;
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
            playSteps(new ArrayList<>(lastAnimationSteps), 0, currentId, null);
        } else {
            appendOutput("Нет сохранённой анимации для повтора.");
        }
    }
    public void togglePause() {
        animationPaused = !animationPaused;
        if (!animationPaused) {
            synchronized (pauseLock) {
                pauseLock.notifyAll();  // пробуждаем ожидающий поток анимации
            }
            appendOutput(animationPaused ? "Анимация приостановлена." : "Анимация продолжена.");
        }
    }

    public void stopAnimation() {
        animationId++;                         // прерываем текущую анимацию
//        lastAnimationSteps = null;
        animationPaused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();             // на случай, если поток ждал паузы
        }
        highlight.clear();
        canvas.setHighlight(highlight);
        canvas.redraw();
        appendOutput("Анимация остановлена.");
    }

    public void loadFromFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Загрузить граф");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"));

        // Устанавливаем начальную директорию
        if (lastDirectory != null && lastDirectory.exists()) {
            fc.setInitialDirectory(lastDirectory);
        }

        File file = fc.showOpenDialog(window());
        if (file == null) return;

        // Запоминаем папку
        lastDirectory = file.getParentFile();
        if (graph != null && dirty && !confirm("Есть несохранённые изменения. Загрузить другой файл?")) {
            return;
        }
        try {
            currentGraphPath = file.getAbsolutePath();
            graph = new Graph<>(currentGraphPath, s -> s, NumberParsing::parseDouble, 1.0, " ");
            dirty = false;
            layout.clear();
            clearHighlight();
            output.clear();
            appendOutput("Загружен: " + file.getName());
            appendOutput("Вершин: " + graph.getVertexCount() + ", рёбер: " + graph.getEdgeList().size());
            loadLayoutForGraph(currentGraphPath);
            refreshCanvas();
        } catch (IOException e) {
            showError("Не удалось прочитать файл: " + e.getMessage());
        } catch (Exception e) {
            showError("Ошибка формата: " + e.getMessage());
        }
    }
    public void saveToFile() {
        requireGraph();
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить граф");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"));

        // Устанавливаем начальную директорию
        if (lastDirectory != null && lastDirectory.exists()) {
            fc.setInitialDirectory(lastDirectory);
        }

        // Если есть текущий путь, предлагаем его как имя по умолчанию
        if (currentGraphPath != null) {
            File currentFile = new File(currentGraphPath);
            fc.setInitialFileName(currentFile.getName());
        }

        File file = fc.showSaveDialog(window());
        if (file == null) return;

        // Запоминаем папку
        lastDirectory = file.getParentFile();
        try {
            String path = file.getAbsolutePath();
            graph.saveToFile(path, " ");
            currentGraphPath = path;
            GraphLayoutStorage.save(path, layout);
            dirty = false;
            layoutDirty = false;
            appendOutput("Сохранено: " + path);
            appendOutput("Раскладка вершин: " + GraphLayoutStorage.layoutPathFor(path));
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }
    // --- Модификация ---
    public void addVertex() {
        requireGraph();
        Optional<String> name = askString("Новая вершина", "Имя вершины:", "");
        if (name.isEmpty()) {
            return;
        }
        String v = name.get();
        if (graph.getAdjacencyStructure().containsKey(v)) {
            showError("Вершина уже существует.");
            return;
        }
        appendOutput("Кликните на холсте, чтобы разместить вершину «" + v + "».");
        canvas.awaitVertexPlacement(p -> {
            try {
                graph.addVertex(v);
                layout.put(v, p.getX(), p.getY());
                markDirty();
                layoutDirty = true;
                appendOutput("Вершина «" + v + "» добавлена.");
                canvas.redraw();
                saveLayoutIfPossible();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });
    }
    public void addEdge() {
        requireGraph();
        Optional<String> u = pickVertex("Ребро", "Вершина-источник:");
        if (u.isEmpty()) {
            return;
        }
        Optional<String> v = pickVertex("Ребро", "Вершина-назначение:");
        if (v.isEmpty()) {
            return;
        }
        double w = 1.0;
        if (graph.isWeighted()) {
            Optional<String> ws = askString("Вес", "Вес ребра:", "1");
            if (ws.isEmpty()) {
                return;
            }
            try {
                w = NumberParsing.parseDouble(ws.get());
            } catch (NumberFormatException e) {
                showError("Некорректный вес.");
                return;
            }
        }
        try {
            graph.addEdge(u.get(), v.get(), w);
            markDirty();
            appendOutput("Ребро " + u.get() + " → " + v.get() + " добавлено.");
            refreshCanvas();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }
    public void removeVertex() {
        requireGraph();
        Optional<String> v = pickVertex("Удаление", "Вершина:");
        if (v.isEmpty()) {
            return;
        }
        try {
            graph.removeVertex(v.get());
            layout.remove(v.get());
            markDirty();
            layoutDirty = true;
            appendOutput("Вершина «" + v.get() + "» удалена.");
            saveLayoutIfPossible();
            refreshCanvas();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }
    public void removeEdge() {
        requireGraph();
        Optional<String> u = pickVertex("Удаление ребра", "Из:");
        if (u.isEmpty()) {
            return;
        }
        Optional<String> v = pickVertex("Удаление ребра", "В:");
        if (v.isEmpty()) {
            return;
        }
        try {
            graph.removeEdge(u.get(), v.get());
            markDirty();
            appendOutput("Ребро удалено.");
            refreshCanvas();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    public void showAdjacency() {
        requireGraph();
        output.clear();
        var structure = graph.getAdjacencyStructure();
        if (structure.isEmpty()) {
            appendOutput("Граф пуст.");
            return;
        }
        for (var entry : structure.entrySet()) {
            StringJoiner sj = new StringJoiner(", ");
            for (var n : entry.getValue().entrySet()) {
                String w = graph.isWeighted() ? "(" + n.getValue() + ")" : "";
                sj.add(n.getKey() + w);
            }
            appendOutput(entry.getKey() + ": " + (sj.length() == 0 ? "(нет соседей)" : sj));
        }
    }
    public void showStatus() {
        requireGraph();
        appendOutput("Тип: " + (graph.isDirected() ? "ориентированный" : "неориентированный"));
        appendOutput("Режим: " + (graph.isWeighted() ? "взвешенный" : "невзвешенный (вес 1.0)"));
        appendOutput("Вершин: " + graph.getVertexCount() + ", рёбер: " + graph.getEdgeList().size());
    }
    public void relayout() {
        requireGraph();
        layout.clear();
        refreshCanvas();
        appendOutput("Раскладка пересчитана.");
    }
    // --- Алгоритмы ---

    public void runDijkstra() {
        requireGraph();
        animationId++;
        if (!graph.isWeighted()) { showError("Дейкстра требует взвешенный граф."); return; }
        Optional<String> source = pickVertex("Дейкстра", "Источник:");
        if (source.isEmpty()) return;
        try {
            var res = graph.dijkstra(source.get());
            output.clear();
            appendOutput("Кратчайшие пути из «" + source.get() + "»:");
            for (String v : graph.getAdjacencyStructure().keySet()) {
                double d = res.getDistance(v);
                if (Double.isInfinite(d)) appendOutput("  → " + v + ": нет пути");
                else appendOutput("  → " + v + " = " + formatDistance(d)
                        + " | " + String.join(" → ", res.buildPathTo(v)));
            }
            boolean directed = graph.isDirected();

            if (animationEnabled) {
                // Строим шаги: BFS по дереву кратчайших путей
                Map<String, Integer> depth = new HashMap<>();
                Queue<String> q = new LinkedList<>();
                q.add(source.get());
                depth.put(source.get(), 0);
                Map<Integer, List<GraphHighlight.EdgeKey>> edgesByDepth = new HashMap<>();

                while (!q.isEmpty()) {
                    String u = q.poll();
                    int du = depth.get(u);
                    for (Map.Entry<String, Double> e : graph.getAdjacencyStructure().get(u).entrySet()) {
                        String v = e.getKey();
                        List<String> path = res.buildPathTo(v);
                        if (path.size() >= 2 && path.get(path.size()-2).equals(u)) {
                            if (!depth.containsKey(v)) {
                                depth.put(v, du + 1);
                                q.add(v);
                            }
                            GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(u, v, directed);
                            edgesByDepth.computeIfAbsent(du, k -> new ArrayList<>()).add(key);
                        }
                    }
                }

                List<GraphHighlight> steps = new ArrayList<>();
                GraphHighlight current = new GraphHighlight();
                current.setMode(GraphHighlight.Mode.PATH);
                current.addHighlightedVertex(source.get());
                steps.add(new GraphHighlight(current)); // шаг 0 – только источник

                for (int d = 0; edgesByDepth.containsKey(d); d++) {
                    List<GraphHighlight.EdgeKey> levelEdges = edgesByDepth.get(d);
                    for (GraphHighlight.EdgeKey ek : levelEdges) {
                        current.addHighlightedEdge(ek);
                        current.addHighlightedVertex(ek.a());
                        current.addHighlightedVertex(ek.b());
                    }
                    steps.add(new GraphHighlight(current));
                }

                // Финальный шаг – полное дерево
                GraphHighlight finalHl = new GraphHighlight();
                finalHl.setShortestPathTree(res, directed);
                steps.add(finalHl);

                runAnimation(steps, null);
            } else {
                highlight.setShortestPathTree(res, directed);
                canvas.setHighlight(highlight);
                canvas.redraw();
            }
            appendOutput("(На холсте подсвечено дерево кратчайших путей)");
        } catch (Exception e) { showError(e.getMessage()); }
    }

    public void runBellmanFord() {
        requireGraph();
        animationId++;
        lastAnimationSteps = null;
        if (!graph.isWeighted()) {
            showError("Беллман–Форд требует взвешенный граф.");
            return;
        }
        Optional<String> u1 = askString("Беллман–Форд", "Вершина u1:", "");
        Optional<String> u2 = askString("Беллман–Форд", "Вершина u2:", "");
        Optional<String> v = askString("Беллман–Форд", "Целевая v:", "");
        if (u1.isEmpty() || u2.isEmpty() || v.isEmpty()) return;

        try {
            output.clear();
            var r1 = asGraph().bellmanFord(u1.get());
            var r2 = asGraph().bellmanFord(u2.get());
            printPathResult(u1.get(), v.get(), r1);
            printPathResult(u2.get(), v.get(), r2);

            boolean directed = graph.isDirected();

            List<String> path1 = r1.buildPathTo(v.get());
            List<String> path2 = r2.buildPathTo(v.get());

            if (animationEnabled) {
                List<GraphHighlight> steps = new ArrayList<>();
                GraphHighlight current = new GraphHighlight();
                current.setMode(GraphHighlight.Mode.PATH);
                Map<GraphHighlight.EdgeKey, String> labels = new HashMap<>();

                // Путь u1 -> v
                if (path1.size() >= 2) {
                    current.addHighlightedVertex(path1.get(0));
                    steps.add(copyOf(current, labels));
                    for (int i = 0; i < path1.size() - 1; i++) {
                        GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(path1.get(i), path1.get(i+1), directed);
                        current.addHighlightedEdge(key);
                        current.addPath1Edge(key);                    // ← красим в красный
                        current.addHighlightedVertex(path1.get(i+1));
                        labels.put(key, "u1");
                        steps.add(copyOf(current, labels));
                    }
                }

                // Путь u2 -> v
                if (path2.size() >= 2) {
                    for (int i = 0; i < path2.size() - 1; i++) {
                        GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(path2.get(i), path2.get(i+1), directed);
                        current.addHighlightedEdge(key);
                        current.addPath2Edge(key);                    // ← красим в синий
                        current.addHighlightedVertex(path2.get(i+1));
                        labels.put(key, "u2");
                        steps.add(copyOf(current, labels));
                    }
                }

                // Финальный шаг
                steps.add(copyOf(current, labels));

                runAnimation(steps, null);
            } else {
                highlight.clear();
                highlight.setPath(path1, directed);
                highlight.addPath(path2, directed);
                canvas.setHighlight(highlight);
                canvas.redraw();
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    // Утилитарный метод – создаёт копию GraphHighlight и устанавливает метки
    private GraphHighlight copyOf(GraphHighlight src, Map<GraphHighlight.EdgeKey, String> labels) {
        GraphHighlight copy = new GraphHighlight(src);
        copy.setEdgeLabels(new HashMap<>(labels));
        return copy;
    }

    private void printPathResult(String from, String to, Graph.ShortestPathResult<String> res) {
        double d = res.getDistance(to);
        if (Double.isInfinite(d)) {
            appendOutput(from + " → " + to + ": нет пути");
        } else {
            appendOutput(from + " → " + to + " = " + formatDistance(d)
                    + " | " + String.join(" → ", res.buildPathTo(to)));
        }
    }
    public void runFloyd() {
        requireGraph();
        animationId++;
        lastAnimationSteps = null;
        if (!graph.isWeighted()) {
            showError("Флойд требует взвешенный граф.");
            return;
        }
        try {
            var res = graph.floydWarshall();
            var vertices = res.getVertices();

            boolean oldWrap = output.isWrapText();
            output.setWrapText(false);
            // Иногда JavaFX нужно «толкнуть», чтобы пересчитала scrollbar
            output.applyCss();
            output.layout();

            try {
                output.clear();

                // --- подбираем ширину колонки ---
                int maxLen = 0;
                for (String v : vertices) {
                    maxLen = Math.max(maxLen, v.length());
                }
                for (String from : vertices) {
                    for (String to : vertices) {
                        String d = formatDistance(res.getDistance(from, to));
                        maxLen = Math.max(maxLen, d.length());
                    }
                }
                int col = maxLen + 2;
                String colFmt = "%" + col + "s";

                StringBuilder header = new StringBuilder(String.format(colFmt, ""));
                for (String v : vertices) {
                    header.append(String.format(colFmt, v));
                }
                appendOutput(header.toString());

                for (String from : vertices) {
                    StringBuilder row = new StringBuilder(String.format(colFmt, from));
                    for (String to : vertices) {
                        row.append(String.format(colFmt, formatDistance(res.getDistance(from, to))));
                    }
                    appendOutput(row.toString());
                }

            } finally {
                // Гарантированно восстанавливаем
                output.setWrapText(oldWrap);
                output.applyCss();
                output.layout();
            }

            if (res.hasNegativeCycle()) {
                appendOutput("[!] Обнаружен отрицательный цикл.");
            }

            // --- запрос пути (без изменений) ---
            Optional<String> fromV = pickVertex("Флойд", "Путь: от");
            if (fromV.isEmpty()) return;
            Optional<String> toV = pickVertex("Флойд", "Путь: до");
            if (toV.isEmpty()) return;

            List<String> path = res.getPath(fromV.get(), toV.get());
            appendOutput("Путь " + fromV.get() + " → " + toV.get() + ": "
                    + (path == null ? "через отриц. цикл" : path.isEmpty() ? "нет" : String.join(" → ", path)));
            if (path != null && !path.isEmpty()) {
                boolean directed = graph.isDirected();
                if (animationEnabled) {
                    List<GraphHighlight> steps = new ArrayList<>();
                    GraphHighlight current = new GraphHighlight();
                    current.setMode(GraphHighlight.Mode.PATH);
                    // Шаг 0: только начальная вершина
                    current.addHighlightedVertex(path.get(0));
                    steps.add(new GraphHighlight(current));
                    // Далее добавляем по одному ребру и вершине
                    for (int i = 0; i < path.size() - 1; i++) {
                        GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(path.get(i), path.get(i+1), directed);
                        current.addHighlightedEdge(key);
                        current.addHighlightedVertex(path.get(i+1));
                        steps.add(new GraphHighlight(current));
                    }
                    // Финальный шаг можно не дублировать, но оставим для надёжности
                    steps.add(new GraphHighlight(current));
                    runAnimation(steps, null);
                } else {
                    highlight.setPath(path, directed);
                    canvas.setHighlight(highlight);
                    canvas.redraw();
                }
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }
    public void runMaxFlow() {
        requireGraph();
        animationId++;
        lastAnimationSteps = null;
        if (!graph.isDirected() || !graph.isWeighted()) {
            showError("Поток: ориентированный взвешенный граф (веса = пропускные способности).");
            return;
        }
        Optional<String> s = pickVertex("Поток", "Источник s:");
        Optional<String> t = pickVertex("Поток", "Сток t:");
        if (s.isEmpty() || t.isEmpty()) return;
        try {
            Graph.MaxFlowResult<String> result = graph.edmondsKarp(s.get(), t.get());
            output.clear();
            appendOutput("Макс. поток = " + formatDistance(result.getMaxFlow()));

            Map<GraphHighlight.EdgeKey, String> labels = new HashMap<>();
            var flows = result.getFlows();
            for (var e : graph.getEdgeList()) {
                double cap = e.weight;
                double f = flows.getOrDefault(e.source, Map.of()).getOrDefault(e.dest, 0.0);
                if (Math.abs(f) > 1e-12) {
                    GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(e.source, e.dest, true);
                    labels.put(key, formatDistance(f) + "/" + formatDistance(cap));
                }
            }

            if (animationEnabled && !result.getAugmentingPaths().isEmpty()) {
                List<GraphHighlight> steps = new ArrayList<>();
                GraphHighlight accum = new GraphHighlight();
                accum.setMode(GraphHighlight.Mode.FLOW);
                Map<GraphHighlight.EdgeKey, String> accumLabels = new HashMap<>();
                for (int i = 0; i < result.getAugmentingPaths().size(); i++) {
                    List<String> path = result.getAugmentingPaths().get(i);
                    for (int j = 0; j < path.size() - 1; j++) {
                        GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(path.get(j), path.get(j+1), true);
                        accum.addHighlightedEdge(key);
                        accum.addHighlightedVertex(path.get(j));
                        accum.addHighlightedVertex(path.get(j+1));
                        if (labels.containsKey(key)) accumLabels.put(key, labels.get(key));
                    }
                    accum.setEdgeLabels(new HashMap<>(accumLabels));
                    steps.add(new GraphHighlight(accum));
                }
                // Финальный шаг – все рёбра с потоками
                // Финальный шаг – накопленные рёбра и вершины + метки
                accum.setEdgeLabels(new HashMap<>(labels));
                steps.add(new GraphHighlight(accum));

                runAnimation(steps, null);
            } else {
                highlight.clear();
                highlight.setFlowEdges(flows, true);
                highlight.setEdgeLabels(labels);
                for (List<String> path : result.getAugmentingPaths()) {
                    highlight.addPath(path, true);
                }
                canvas.setHighlight(highlight);
                canvas.redraw();
            }

            for (var entry : flows.entrySet()) {
                for (var edge : entry.getValue().entrySet()) {
                    if (Math.abs(edge.getValue()) > 1e-12)
                        appendOutput(entry.getKey() + " → " + edge.getKey() + " : " + formatDistance(edge.getValue()));
                }
            }
        } catch (Exception e) { showError(e.getMessage()); }
    }

    public void runMst() {
        requireGraph();
        animationId++;
        if (graph.isDirected() || !graph.isWeighted()) {
            showError("Краскал: неориентированный взвешенный граф.");
            return;
        }
        try {
            IGraph<String, Double> mst = asGraph().getKruskalMST();
            List<Graph.Edge<String, Double>> mstEdges = mst.getEdgeList();
            Set<GraphHighlight.EdgeKey> edgeKeys = new HashSet<>();
            for (Graph.Edge<String, Double> e : mstEdges)
                edgeKeys.add(GraphHighlight.EdgeKey.of(e.source, e.dest, false));

            if (animationEnabled) {
                List<GraphHighlight> steps = new ArrayList<>();
                GraphHighlight accum = new GraphHighlight();
                accum.setMode(GraphHighlight.Mode.MST);
                for (Graph.Edge<String, Double> e : mstEdges) {
                    GraphHighlight.EdgeKey key = GraphHighlight.EdgeKey.of(e.source, e.dest, false);
                    accum.addHighlightedEdge(key);
                    accum.addHighlightedVertex(e.source);
                    accum.addHighlightedVertex(e.dest);
                    steps.add(new GraphHighlight(accum));
                }
                // Финальный шаг – все рёбра
                steps.add(new GraphHighlight(accum));
                runAnimation(steps, null);
            } else {
                highlight.clear();
                highlight.setEdges(edgeKeys);
                canvas.setHighlight(highlight);
                canvas.redraw();
            }

            appendOutput("MST: " + mstEdges.size() + " рёбер (зелёная подсветка на холсте).");
            if (confirm("Заменить текущий граф на MST?")) {
                graph = mst;
                layout.clear();
                markDirty();
                refreshCanvas();
            }
        } catch (Exception e) { showError(e.getMessage()); }
    }

    public void showRadius() {
        requireGraph();
        try {
            double radius = asGraph().getRadius();
            if (radius == Integer.MAX_VALUE) {
                appendOutput("Граф несвязный — радиус бесконечен.");
            } else {
                appendOutput("Радиус графа: " + (int) radius);
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }
    public void checkTreeVertex() {
        requireGraph();
        animationId++;
        lastAnimationSteps = null;
        if (graph.isDirected()) {
            showError("Только для неориентированного графа.");
            return;
        }
        String candidate = graph.findVertexToRemoveToMakeTree();
        if (candidate != null) {
            appendOutput("Можно удалить вершину «" + candidate + "» и получить дерево.");
            highlight.setPath(List.of(candidate), false);
            canvas.setHighlight(highlight);
            canvas.redraw();
        } else {
            appendOutput("Нельзя получить дерево удалением одной вершины.");
        }
    }
    public void saveTranspose() {
        requireGraph();
        if (!graph.isDirected()) {
            showError("Обращение только для ориентированного графа.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Сохранить обращённый граф");

        if (lastDirectory != null && lastDirectory.exists()) {
            fc.setInitialDirectory(lastDirectory);
        }

        File file = fc.showSaveDialog(window());
        if (file == null) return;

        lastDirectory = file.getParentFile();

        try {
            graph.getTranspose().saveToFile(file.getAbsolutePath(), " ");
            appendOutput("Обращение сохранено: " + file.getAbsolutePath());
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }
    public void showOutDegree() {
        requireGraph();
        if (!graph.isDirected()) {
            showError("Полустепень исхода — только для орграфа.");
            return;
        }
        pickVertex("Полустепень", "Вершина:").ifPresent(v -> {
            try {
                appendOutput("out-degree(" + v + ") = " + graph.getOutDegree(v));
            } catch (Exception e) {
                showError(e.getMessage());
            }
        });
    }

    public void saveLayout() {
        if (currentGraphPath == null) {
            showError("Сначала сохраните граф в файл.");
            return;
        }
        try {
            GraphLayoutStorage.save(currentGraphPath, layout);
            layoutDirty = false;
            appendOutput("Раскладка сохранена.");
        } catch (IOException e) {
            showError("Не удалось сохранить раскладку: " + e.getMessage());
        }
    }


    private void runAnimation(List<GraphHighlight> steps, Runnable onFinished) {
        if (steps == null || steps.isEmpty()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        lastAnimationSteps = new ArrayList<>(steps);
        highlight.clear();               // полный сброс текущей подсветки
        canvas.setHighlight(highlight);
        canvas.redraw();
        animationId++;                        // новый идентификатор
        animationPaused = false;
        synchronized (pauseLock) {
            pauseLock.notifyAll();  // на всякий случай пробуждаем
        }
        int currentId = animationId;

        playSteps(steps, 0, currentId, onFinished);
    }

    private void playSteps(List<GraphHighlight> steps, int index, int id, Runnable onFinished) {
        if (id != animationId) return;        // прерывание, если запущена более новая анимация
        if (index >= steps.size()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        GraphHighlight step = steps.get(index);
        highlight.clear();
        highlight.setMode(step.getMode());
        highlight.addHighlightedVertices(step.getHighlightedVertices());
        highlight.addHighlightedEdges(step.getHighlightedEdges());
        highlight.setEdgeLabels(new HashMap<>(step.getEdgeLabels()));
        highlight.addPath1Edges(step.getPath1Edges());
        highlight.addPath2Edges(step.getPath2Edges());
        canvas.setHighlight(highlight);
        canvas.redraw();

        new Thread(() -> {
            try { Thread.sleep(animationDelay); } catch (InterruptedException ignored) {}

            // Ожидание, если анимация на паузе и не была прервана
            synchronized (pauseLock) {
                while (animationPaused && id == animationId) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException ignored) {}
                }
            }

            // Если за время паузы анимация была остановлена, не продолжаем
            if (id != animationId) return;

            javafx.application.Platform.runLater(() -> playSteps(steps, index + 1, id, onFinished));
        }).start();
    }

    public void clearOutput() {
        output.clear();
        clearHighlight();
    }
    private static String formatDistance(double d) {
        if (d == Double.POSITIVE_INFINITY) return "∞";
        if (d == Double.NEGATIVE_INFINITY) return "-∞";
        if (d == (long) d) return String.valueOf((long) d);

        // Форматируем с двумя знаками после запятой (системный разделитель)
        String s = String.format("%.2f", d);

        // Убираем лишние нули в дробной части
        if (s.contains(",")) {
            s = s.replaceAll("0+$", "");      // удаляем нули в конце
            if (s.endsWith(",")) {
                s = s.substring(0, s.length() - 1); // убираем запятую, если дробной части не осталось
            }
        } else if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }

    public boolean isAnimationPaused() {
        return animationPaused;
    }

}