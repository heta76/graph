package heta.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * Класс, реализующий структуру графа (ориентированного и неориентированного).
 * @param <TVertex> Тип идентификатора вершины.
 * @param <TWeight> Тип веса ребра.
 */
public class Graph<TVertex, TWeight> implements IGraph<TVertex, TWeight>{

    // Основной список смежности: Вершина -> (Сосед -> Вес)
    private final Map<TVertex, Map<TVertex, TWeight>> adjacencyList;

    // Список входящих ребер для ориентированного графа
    private final Map<TVertex, Set<TVertex>> reverseAdjacency;

    private final boolean isDirected;

    // --- Конструкторы ---

    public Graph(boolean isDirected) {
        this.isDirected = isDirected;
        this.adjacencyList = new HashMap<>();
        this.reverseAdjacency = isDirected ? new HashMap<>() : null;
    }

    public Graph(boolean isDirected, Iterable<TVertex> vertices) {
        this(isDirected);
        if (vertices != null) {
            for (TVertex v : vertices) {
                addVertexInternal(v);
            }
        }
    }

    // Конструктор копии
    public Graph(Graph<TVertex, TWeight> other) {
        if (other == null) throw new IllegalArgumentException("other cannot be null");

        this.isDirected = other.isDirected;
        this.adjacencyList = new HashMap<>();

        // Глубокая копия списка смежности
        for (var entry : other.adjacencyList.entrySet()) {
            this.adjacencyList.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }

        // Глубокая копия обратного списка
        if (this.isDirected && other.reverseAdjacency != null) {
            this.reverseAdjacency = new HashMap<>();
            for (var entry : other.reverseAdjacency.entrySet()) {
                this.reverseAdjacency.put(entry.getKey(), new HashSet<>(entry.getValue()));
            }
        } else {
            this.reverseAdjacency = null;
        }
    }

    // Конструктор из файла
    public Graph(String filePath, Function<String, TVertex> vertexParser, Function<String, TWeight> weightParser, String separator) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        if (lines.isEmpty()) throw new IOException("Файл пуст.");

        // 1. Определение типа графа
        String type = lines.get(0).trim().toLowerCase();
        if (type.equals("directed")) {
            this.isDirected = true;
            this.reverseAdjacency = new HashMap<>();
        } else if (type.equals("undirected")) {
            this.isDirected = false;
            this.reverseAdjacency = null;
        } else {
            throw new IOException("Некорректный тип графа: " + type);
        }
        this.adjacencyList = new HashMap<>();

        // 2. Парсинг вершин (строка 2)
        if (lines.size() > 1 && !lines.get(1).isBlank()) {
            String[] verticesParts = lines.get(1).split(separator);
            for (String vStr : verticesParts) {
                if (!vStr.isBlank()) {
                    addVertexInternal(vertexParser.apply(vStr.trim()));
                }
            }
        }

        // 3. Парсинг ребер (строки 3+)
        for (int i = 2; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;

            String[] parts = line.split(separator);
            if (parts.length < 3) throw new IOException("Ошибка формата в строке " + (i + 1));

            TVertex v1 = vertexParser.apply(parts[0].trim());
            TVertex v2 = vertexParser.apply(parts[1].trim());
            TWeight w = weightParser.apply(parts[2].trim());

            addEdge(v1, v2, w);
        }
    }

    // --- Методы ---
    @Override
    public boolean isDirected() { return isDirected; }

    @Override
    public int getVertexCount() { return adjacencyList.size(); }

    private void addVertexInternal(TVertex vertex) {
        adjacencyList.putIfAbsent(vertex, new HashMap<>());
        if (isDirected) {
            reverseAdjacency.putIfAbsent(vertex, new HashSet<>());
        }
    }

    @Override
    public void addVertex(TVertex vertex) {
        if (vertex == null) throw new NullPointerException("Vertex cannot be null");
        if (adjacencyList.containsKey(vertex)) {
            throw new IllegalStateException("Вершина " + vertex + " уже существует.");
        }
        addVertexInternal(vertex);
    }

    @Override
    public void addEdge(TVertex source, TVertex destination, TWeight weight) {
        if (!adjacencyList.containsKey(source) || !adjacencyList.containsKey(destination)) {
            throw new NoSuchElementException("Одна из вершин не найдена.");
        }
        if (adjacencyList.get(source).containsKey(destination)) {
            throw new IllegalStateException("Ребро уже существует.");
        }

        adjacencyList.get(source).put(destination, weight);

        if (isDirected) {
            reverseAdjacency.get(destination).add(source);
        } else {
            // Для неориентированного — симметрия
            adjacencyList.get(destination).put(source, weight);
        }
    }

    @Override
    public void removeVertex(TVertex vertex) {
        if (!adjacencyList.containsKey(vertex)) {
            throw new NoSuchElementException("Вершина не найдена.");
        }

        if (isDirected) {
            // Удаляем исходящие ребра из reverseAdjacency соседей
            for (TVertex dest : adjacencyList.get(vertex).keySet()) {
                reverseAdjacency.get(dest).remove(vertex);
            }
            // Удаляем входящие ребра из списков смежности источников
            for (TVertex source : reverseAdjacency.get(vertex)) {
                adjacencyList.get(source).remove(vertex);
            }
            reverseAdjacency.remove(vertex);
        } else {
            // Для неориентированного: удаляем у всех соседей упоминание об этой вершине
            for (TVertex neighbor : adjacencyList.get(vertex).keySet()) {
                adjacencyList.get(neighbor).remove(vertex);
            }
        }
        adjacencyList.remove(vertex);
    }

    @Override
    public void removeEdge(TVertex source, TVertex destination) {
        // 1. Проверяем наличие исходной вершины
        if (!adjacencyList.containsKey(source)) {
            throw new NoSuchElementException("Вершина " + source + " не найдена.");
        }

        // 2. Проверяем наличие самого ребра
        if (!adjacencyList.get(source).containsKey(destination)) {
            throw new IllegalStateException("Ребро " + source + " -> " + destination + " не найдено.");
        }

        // 3. Удаляем основную запись
        adjacencyList.get(source).remove(destination);

        // 4. Обновляем дополнительные структуры в зависимости от типа графа
        if (isDirected) {
            // В ориентированном графе чистим список входящих ребер (reverseAdjacency)
            if (reverseAdjacency != null && reverseAdjacency.containsKey(destination)) {
                reverseAdjacency.get(destination).remove(source);
            }
        } else {
            // В неориентированном графе удаляем симметричное ребро
            if (adjacencyList.containsKey(destination)) {
                adjacencyList.get(destination).remove(source);
            }
        }
    }

    @Override
    public void saveToFile(String filePath, String separator) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println(isDirected ? "directed" : "undirected");

            // Запись вершин
            StringJoiner joiner = new StringJoiner(separator);
            for (TVertex v : adjacencyList.keySet()) {
                joiner.add(v.toString());
            }
            writer.println(joiner.toString());

            // Запись ребер
            Set<String> visitedEdges = new HashSet<>();
            for (var entry : adjacencyList.entrySet()) {
                TVertex u = entry.getKey();
                for (var edge : entry.getValue().entrySet()) {
                    TVertex v = edge.getKey();
                    TWeight w = edge.getValue();

                    if (!isDirected) {
                        String pair1 = u.toString() + "-" + v.toString();
                        String pair2 = v.toString() + "-" + u.toString();
                        if (visitedEdges.contains(pair1) || visitedEdges.contains(pair2)) continue;
                        visitedEdges.add(pair1);
                    }
                    writer.println(u + separator + v + separator + w);
                }
            }
        }
    }

    // Вспомогательный класс для списка ребер
    public static class Edge<TVertex, TWeight> {
        public final TVertex source;
        public final TVertex dest;
        public final TWeight weight;

        public Edge(TVertex s, TVertex d, TWeight w) {
            this.source = s; this.dest = d; this.weight = w;
        }
    }

    public List<Edge<TVertex, TWeight>> getEdgeList() {
        List<Edge<TVertex, TWeight>> edges = new ArrayList<>();
        Set<String> visitedEdges = new HashSet<>();

        for (var entry : adjacencyList.entrySet()) {
            TVertex u = entry.getKey();
            for (var edge : entry.getValue().entrySet()) {
                TVertex v = edge.getKey();
                if (!isDirected) {
                    String pair1 = u.toString() + "-" + v.toString();
                    String pair2 = v.toString() + "-" + u.toString();
                    if (visitedEdges.contains(pair1) || visitedEdges.contains(pair2)) continue;
                    visitedEdges.add(pair1);
                }
                edges.add(new Edge<>(u, v, edge.getValue()));
            }
        }
        return edges;
    }
}