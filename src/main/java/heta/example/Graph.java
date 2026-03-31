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
    private final boolean isWeighted;


    private class DSU<T> {
        private final Map<T, T> parent = new HashMap<>();

        public DSU(Collection<T> elements) {
            for (T e : elements) parent.put(e, e);
        }

        public T find(T i) {
            if (parent.get(i).equals(i)) return i;
            T root = find(parent.get(i));
            parent.put(i, root); // Сжатие путей
            return root;
        }

        public boolean union(T i, T j) {
            T rootI = find(i);
            T rootJ = find(j);
            if (!rootI.equals(rootJ)) {
                parent.put(rootI, rootJ);
                return true;
            }
            return false;
        }
    }

    // --- Конструкторы ---
    public Graph(boolean isDirected, boolean isWeighted) {
        this.isDirected = isDirected;
        this.isWeighted = isWeighted;
        this.adjacencyList = new HashMap<>();
        this.reverseAdjacency = isDirected ? new HashMap<>() : null;
    }

    public Graph(boolean isDirected, Iterable<TVertex> vertices, boolean isWeighted) {
        this(isDirected, isWeighted);
        if (vertices != null) {
            for (TVertex v : vertices) {
                addVertexInternal(v);
            }
        }
    }

    // Конструктор копии
    public Graph(Graph<TVertex, TWeight> other, boolean isWeighted) {

        if (other == null) throw new IllegalArgumentException("other cannot be null");

        this.isDirected = other.isDirected;
        this.isWeighted = other.isWeighted;
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
    public Graph(String filePath, Function<String, TVertex> vertexParser, Function<String, TWeight> weightParser, TWeight defaultWeight, String separator) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        if (lines.isEmpty()) throw new IOException("Файл пуст.");

        // 1. Тип (directed/undirected) - Строка 1
        String type = lines.get(0).trim().toLowerCase();
        this.isDirected = type.equals("directed");
        this.reverseAdjacency = isDirected ? new HashMap<>() : null;
        this.adjacencyList = new HashMap<>();

        // 2. Взвешенность (weighted/unweighted) - Строка 2
        if (lines.size() < 2) throw new IOException("Неверный формат: отсутствует строка типа веса.");
        String weightType = lines.get(1).trim().toLowerCase();
        this.isWeighted = weightType.equals("weighted");

        // 3. Парсинг вершин - Строка 3
        if (lines.size() > 2 && !lines.get(2).isBlank()) {
            String[] verticesParts = lines.get(2).split(separator);
            for (String vStr : verticesParts) {
                if (!vStr.isBlank()) {
                    addVertexInternal(vertexParser.apply(vStr.trim()));
                }
            }
        }

        // 4. Парсинг ребер - Строки 4+
        for (int i = 3; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;

            String[] parts = line.split(separator);
            TVertex v1 = vertexParser.apply(parts[0].trim());
            TVertex v2 = vertexParser.apply(parts[1].trim());

            TWeight w;
            if (isWeighted) {
                if (parts.length < 3) throw new IOException("Строка " + (i + 1) + ": ожидался вес.");
                w = weightParser.apply(parts[2].trim());
            } else {
                w = defaultWeight; // Используем дефолт (например, 1.0)
            }

            addEdge(v1, v2, w);
        }
    }

    // --- Методы ---
    @Override
    public boolean isDirected() { return isDirected; }

    @Override
    public int getVertexCount() { return adjacencyList.size(); }

    @Override
    public boolean isWeighted() { return isWeighted; }

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
        // Проверка на петлю в неориентированном графе
        if (!isDirected && source.equals(destination)) {
            throw new IllegalArgumentException(
                    "Невозможно создать петлю в неориентированном графе: вершина " + source +
                            " не может быть соединена сама с собой, так как это приведёт к мультиграфу."
            );
        }

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
            writer.println(isWeighted ? "weighted" : "unweighted");

            // Вершины
            StringJoiner vJoiner = new StringJoiner(separator);
            for (TVertex v : adjacencyList.keySet()) vJoiner.add(v.toString());
            writer.println(vJoiner.toString());

            // Ребра
            Set<String> visited = new HashSet<>();
            for (var entry : adjacencyList.entrySet()) {
                TVertex u = entry.getKey();
                for (var edge : entry.getValue().entrySet()) {
                    TVertex v = edge.getKey();
                    if (!isDirected) {
                        String p1 = u + "-" + v, p2 = v + "-" + u;
                        if (visited.contains(p1) || visited.contains(p2)) continue;
                        visited.add(p1);
                    }

                    if (isWeighted) {
                        writer.println(u + separator + v + separator + edge.getValue());
                    } else {
                        writer.println(u + separator + v);
                    }
                }
            }
        }
    }


    // Сохраняет обращённый граф (transpose) в файл.
    public void saveTransposeToFile(String filePath, String separator) throws IOException {
        IGraph<TVertex, TWeight> trans = getTranspose();
        // saveToFile определён в IGraph, поэтому можно вызывать напрямую
        trans.saveToFile(filePath, separator);
    }

    @Override
    public int getOutDegree(TVertex vertex) {

        if (!isDirected) {
            throw new UnsupportedOperationException(
                    "Полустепень исхода определяется только для ориентированного графа."
            );
        }

        if (!adjacencyList.containsKey(vertex)) {
            throw new NoSuchElementException("Вершина не найдена.");
        }

        return adjacencyList.get(vertex).size();
    }

    @Override
    public Set<TVertex> getAdjacentVertices(TVertex vertex) {

        if (!adjacencyList.containsKey(vertex)) {
            throw new NoSuchElementException("Вершина не найдена.");
        }

        // Возвращаем только исходящих соседей
        return Collections.unmodifiableSet(adjacencyList.get(vertex).keySet());
    }

    @Override
    public Set<TVertex> getIncomingVertices(TVertex vertex) {
        if (!adjacencyList.containsKey(vertex)) {
            throw new NoSuchElementException("Вершина не найдена.");
        }
        return Collections.unmodifiableSet(reverseAdjacency.get(vertex));
    }

    @Override
    public Map<TVertex, java.util.Map<TVertex, TWeight>> getAdjacencyStructure() {
        // Возвращаем копию или unmodifiable view для безопасности
        return Collections.unmodifiableMap(adjacencyList);
    }


    @Override
    public IGraph<TVertex, TWeight> getTranspose() {
        // Если граф неориентированный — обращение совпадает с ним (возврат копии)
        if (!isDirected) {
            return new Graph<>(this, this.isWeighted);
        }

        // Создаём новый ориентированный граф с теми же свойствами
        Graph<TVertex, TWeight> transposed = new Graph<>(true, this.isWeighted);

        // Добавляем все вершины (используем внутренний метод, т.к. мы внутри класса)
        for (TVertex v : adjacencyList.keySet()) {
            transposed.addVertexInternal(v);
        }

        // Для каждой дуги u -> v добавляем v -> u с тем же весом
        for (var entry : adjacencyList.entrySet()) {
            TVertex u = entry.getKey();
            for (var edge : entry.getValue().entrySet()) {
                TVertex v = edge.getKey();
                TWeight w = edge.getValue();
                // Добавляем перевёрнутое ребро
                transposed.addEdge(v, u, w);
            }
        }

        return transposed;
    }

    // --- Результаты кратчайших путей ---

    public static class ShortestPathResult<V> {
        private final V source;
        private final Map<V, Double> distances;
        private final Map<V, V> predecessors;

        public ShortestPathResult(V source, Map<V, Double> distances, Map<V, V> predecessors) {
            this.source = source;
            this.distances = Collections.unmodifiableMap(new LinkedHashMap<>(distances));
            this.predecessors = Collections.unmodifiableMap(new LinkedHashMap<>(predecessors));
        }

        public V getSource() {
            return source;
        }

        public Map<V, Double> getDistances() {
            return distances;
        }

        public double getDistance(V vertex) {
            return distances.getOrDefault(vertex, Double.POSITIVE_INFINITY);
        }

        public List<V> buildPathTo(V target) {
            Double d = distances.get(target);
            if (d == null || Double.isInfinite(d)) {
                return Collections.emptyList();
            }

            LinkedList<V> path = new LinkedList<>();
            V current = target;

            while (current != null) {
                path.addFirst(current);
                if (current.equals(source)) {
                    return path;
                }
                current = predecessors.get(current);
            }

            return Collections.emptyList();
        }
    }

    public static class FloydResult<V> {
        private final List<V> vertices;
        private final Map<V, Map<V, Double>> distances;
//        private final boolean hasNegativeCycle;
        private final Map<V, Map<V, V>> next;

        public FloydResult(List<V> vertices, Map<V, Map<V, Double>> distances, Map<V, Map<V, V>> next) {
            this.vertices = vertices;
            this.distances = distances;
            this.next = next;
        }

        public List<V> getVertices() {
            return vertices;
        }

        public List<V> getPath(V from, V to) {
            if (distances.get(from).get(to) == Double.POSITIVE_INFINITY) return Collections.emptyList();
            if (distances.get(from).get(to) == Double.NEGATIVE_INFINITY) return null; // Сигнал об отр. цикле

            List<V> path = new ArrayList<>();
            V current = from;
            while (current != null) {
                path.add(current);
                if (current.equals(to)) break;
                current = next.get(current).get(to);
                if (path.size() > vertices.size()) return null; // Защита от зацикливания
            }
            return path;
        }

        public Map<V, Map<V, Double>> getDistances() {
            return distances;
        }

        public boolean hasNegativeCycle() {
            for (V v : vertices) {
                if (getDistance(v, v) < 0) {
                    return true;
                }
            }
            return false;
        }

        public double getDistance(V from, V to) {
            Map<V, Double> row = distances.get(from);
            if (row == null) return Double.POSITIVE_INFINITY;
            return row.getOrDefault(to, Double.POSITIVE_INFINITY);
        }
    }

// --- Вспомогательные методы ---

    private double weightToDouble(TWeight weight) {
        if (weight instanceof Number num) {
            return num.doubleValue();
        }
        throw new IllegalArgumentException("Веса должны быть числами (Number).");
    }

    private boolean hasNegativeEdge() {
        for (var entry : adjacencyList.entrySet()) {
            for (var edge : entry.getValue().entrySet()) {
                if (weightToDouble(edge.getValue()) < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static class PQNode<V> {
        final V vertex;
        final double distance;

        PQNode(V vertex, double distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }



    @Override
    public TVertex findVertexToRemoveToMakeTree() {
        if (isDirected) {
            throw new UnsupportedOperationException("Задача определена только для неориентированного графа.");
        }

        int V = adjacencyList.size();
        if (V <= 1) return null; // Из пустого графа или 1 вершины дерево не сделать

        // Важно: getEdgeList() для неориентированного графа возвращает каждое ребро один раз
        int E = getEdgeList().size();
        int neededDegree = E - V + 2;

        for (TVertex v : adjacencyList.keySet()) {
            int degree = adjacencyList.get(v).size();

            // Сначала быстрая проверка по количеству ребер
            if (degree == neededDegree) {
                // Затем проверка на связность оставшейся части
                if (isConnectedWithoutVertex(v)) {
                    return v; // Нашли подходящую вершину
                }
            }
        }
        return null;
    }



    private boolean isConnectedWithoutVertex(TVertex removed) {

        Set<TVertex> visited = new HashSet<>();

        TVertex start = null;

        for (TVertex v : adjacencyList.keySet()) {
            if (!v.equals(removed)) {
                start = v;
                break;
            }
        }

        if (start == null) return true;

        dfsWithoutVertex(start, removed, visited);

        return visited.size() == adjacencyList.size() - 1;
    }

    private void dfsWithoutVertex(TVertex v, TVertex removed, Set<TVertex> visited) {

        visited.add(v);

        for (TVertex neighbor : adjacencyList.get(v).keySet()) {

            if (neighbor.equals(removed)) continue;

            if (!visited.contains(neighbor)) {
                dfsWithoutVertex(neighbor, removed, visited);
            }
        }
    }


    public double getRadius() {
        // Радиус обычно ищется для связных графов.
        // Если граф несвязный, расстояние до некоторых вершин будет бесконечным.

        List<Integer> eccentricities = new ArrayList<>();

        for (TVertex v : adjacencyList.keySet()) {
            int eccentricity = calculateEccentricity(v);
            // Если вершина изолирована или не до всех можно добраться,
            // обычно возвращают бесконечность или обрабатывают отдельно.
            if (eccentricity != -1) {
                eccentricities.add(eccentricity);
            }
        }

        if (eccentricities.isEmpty()) return 0;

        return Collections.min(eccentricities);
    }

    private int calculateEccentricity(TVertex startNode) {
        Map<TVertex, Integer> distances = new HashMap<>();
        Queue<TVertex> queue = new LinkedList<>();

        queue.add(startNode);
        distances.put(startNode, 0);

        int maxDist = 0;

        while (!queue.isEmpty()) {
            TVertex current = queue.poll();
            int currentDist = distances.get(current);

            if (currentDist > maxDist) {
                maxDist = currentDist;
            }

            // Перебираем соседей
            for (TVertex neighbor : adjacencyList.get(current).keySet()) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, currentDist + 1);
                    queue.add(neighbor);
                }
            }
        }

        // Проверка на связность: если посетили меньше вершин, чем есть в графе
        if (distances.size() < adjacencyList.size()) {
            // В теории графов для несвязных графов радиус часто считается бесконечным
            return Integer.MAX_VALUE;
        }

        return maxDist;
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

    public IGraph<TVertex, TWeight> getKruskalMST() {
        if (isDirected) {
            throw new UnsupportedOperationException("Каркас минимального веса обычно ищется для неориентированных графов.");
        }

        // 1. Создаем пустой неориентированный взвешенный граф для результата
        Graph<TVertex, TWeight> mst = new Graph<>(false, true);
        for (TVertex v : adjacencyList.keySet()) {
            mst.addVertex(v);
        }

        // 2. Получаем все ребра и сортируем их по весу
        List<Edge<TVertex, TWeight>> edges = getEdgeList();
        edges.sort(Comparator.comparingDouble(e -> ((Number) e.weight).doubleValue()));

        // 3. Используем DSU для объединения компонентов
        DSU<TVertex> dsu = new DSU<>(adjacencyList.keySet());
        int edgesAdded = 0;
        double totalWeight = 0;

        for (Edge<TVertex, TWeight> edge : edges) {
            if (dsu.union(edge.source, edge.dest)) {
                mst.addEdge(edge.source, edge.dest, edge.weight);
                totalWeight += ((Number) edge.weight).doubleValue();
                edgesAdded++;

                // Если добавили V-1 ребро, каркас готов
                if (edgesAdded == getVertexCount() - 1) break;
            }
        }

        // Можно вывести вес в консоль для информации
        System.out.println("Общий вес остовного дерева: " + totalWeight);
        return mst;
    }

    public ShortestPathResult<TVertex> dijkstra(TVertex source) {
        if (!adjacencyList.containsKey(source)) {
            throw new NoSuchElementException("Вершина не найдена: " + source);
        }

        if (hasNegativeEdge()) {
            throw new UnsupportedOperationException(
                    "Алгоритм Дейкстры нельзя применять к графу с отрицательными весами."
            );
        }

        Map<TVertex, Double> dist = new LinkedHashMap<>();
        Map<TVertex, TVertex> prev = new LinkedHashMap<>();

        for (TVertex v : adjacencyList.keySet()) {
            dist.put(v, Double.POSITIVE_INFINITY);
            prev.put(v, null);
        }

        PriorityQueue<PQNode<TVertex>> pq =
                new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));

        dist.put(source, 0.0);
        pq.add(new PQNode<>(source, 0.0));

        while (!pq.isEmpty()) {
            PQNode<TVertex> cur = pq.poll();

            if (cur.distance > dist.get(cur.vertex)) {
                continue;
            }

            for (var edge : adjacencyList.get(cur.vertex).entrySet()) {
                TVertex to = edge.getKey();
                double w = weightToDouble(edge.getValue());
                double nd = cur.distance + w;

                if (nd < dist.get(to)) {
                    dist.put(to, nd);
                    prev.put(to, cur.vertex);
                    pq.add(new PQNode<>(to, nd));
                }
            }
        }

        return new ShortestPathResult<>(source, dist, prev);
    }


    public ShortestPathResult<TVertex> bellmanFord(TVertex source) {
        if (!adjacencyList.containsKey(source)) {
            throw new NoSuchElementException("Вершина не найдена: " + source);
        }

        Map<TVertex, Double> dist = new LinkedHashMap<>();
        Map<TVertex, TVertex> prev = new LinkedHashMap<>();

        for (TVertex v : adjacencyList.keySet()) {
            dist.put(v, Double.POSITIVE_INFINITY);
            prev.put(v, null);
        }

        dist.put(source, 0.0);

        int V = adjacencyList.size();

        for (int i = 1; i < V; i++) {
            boolean changed = false;

            for (var entry : adjacencyList.entrySet()) {
                TVertex u = entry.getKey();
                double du = dist.get(u);

                if (Double.isInfinite(du)) continue;

                for (var edge : entry.getValue().entrySet()) {
                    TVertex v = edge.getKey();
                    double w = weightToDouble(edge.getValue());

                    if (du + w < dist.get(v)) {
                        dist.put(v, du + w);
                        prev.put(v, u);
                        changed = true;
                    }
                }
            }

            if (!changed) break;
        }

        // Проверка на отрицательный цикл
        for (var entry : adjacencyList.entrySet()) {
            TVertex u = entry.getKey();
            double du = dist.get(u);

            if (Double.isInfinite(du)) continue;

            for (var edge : entry.getValue().entrySet()) {
                TVertex v = edge.getKey();
                double w = weightToDouble(edge.getValue());

                if (du + w < dist.get(v)) {
                    throw new IllegalStateException("В графе есть цикл отрицательного веса.");
                }
            }
        }

        return new ShortestPathResult<>(source, dist, prev);
    }



    public FloydResult<TVertex> floydWarshall() {
        if (adjacencyList.isEmpty()) throw new IllegalStateException("Граф пуст.");

        List<TVertex> vertices = new ArrayList<>(adjacencyList.keySet());
        int n = vertices.size();
        Map<TVertex, Integer> index = new HashMap<>();
        for (int i = 0; i < n; i++) index.put(vertices.get(i), i);

        double[][] dist = new double[n][n];
        Integer[][] next = new Integer[n][n]; // Для восстановления пути

        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
            dist[i][i] = 0.0;
        }

        // Инициализация ребер
        for (var entry : adjacencyList.entrySet()) {
            int i = index.get(entry.getKey());
            for (var edge : entry.getValue().entrySet()) {
                int j = index.get(edge.getKey());
                double w = weightToDouble(edge.getValue());
                if (w < dist[i][j]) {
                    dist[i][j] = w;
                    next[i][j] = j;
                }
            }
        }

        // Основной цикл O(V^3)
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (dist[i][k] != Double.POSITIVE_INFINITY && dist[k][j] != Double.POSITIVE_INFINITY) {
                        if (dist[i][k] + dist[k][j] < dist[i][j]) {
                            dist[i][j] = dist[i][k] + dist[k][j];
                            next[i][j] = next[i][k];
                        }
                    }
                }
            }
        }

        // Обработка отрицательных циклов: если путь проходит через цикл, он равен -Infinity
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int t = 0; t < n; t++) {
                    if (dist[t][t] < 0 && dist[i][t] != Double.POSITIVE_INFINITY && dist[t][j] != Double.POSITIVE_INFINITY) {
                        dist[i][j] = Double.NEGATIVE_INFINITY;
                    }
                }
            }
        }

        // Перенос результатов в FloydResult
        Map<TVertex, Map<TVertex, Double>> distMap = new LinkedHashMap<>();
        Map<TVertex, Map<TVertex, TVertex>> nextMap = new LinkedHashMap<>();

        for (int i = 0; i < n; i++) {
            Map<TVertex, Double> dRow = new LinkedHashMap<>();
            Map<TVertex, TVertex> nRow = new LinkedHashMap<>();
            for (int j = 0; j < n; j++) {
                dRow.put(vertices.get(j), dist[i][j]);
                nRow.put(vertices.get(j), next[i][j] != null ? vertices.get(next[i][j]) : null);
            }
            distMap.put(vertices.get(i), dRow);
            nextMap.put(vertices.get(i), nRow);
        }

        return new FloydResult<>(vertices, distMap, nextMap);
    }

}