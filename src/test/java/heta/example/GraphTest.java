package heta.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class GraphTest {

    @TempDir
    Path tempDir;

    private Path testFilesDir;

    @BeforeEach
    void setUp() {
        // Получаем текущую рабочую директорию
        String userDir = System.getProperty("user.dir");
        System.out.println("user.dir = " + userDir);

        // Строим путь к тестовым файлам
        testFilesDir = Path.of(userDir, "src", "main", "java", "heta", "example");
        System.out.println("Тестовые файлы ищем в: " + testFilesDir);
    }

    @Test
    void testLoadUndirectedUnweighted() throws IOException {
        // Given
        Path filePath = testFilesDir.resolve("undirected_unweighted.txt");
        assertTrue(Files.exists(filePath), "Файл не найден: " + filePath);

        // When
        Graph<String, Double> graph = new Graph<>(
                filePath.toString(),
                s -> s,
                Double::parseDouble,
                " "
        );

        // Then
        assertFalse(graph.isDirected());
        assertEquals(8, graph.getVertexCount());

        List<Graph.Edge<String, Double>> edges = graph.getEdgeList();
        assertEquals(9, edges.size());

        // Проверяем наличие конкретных рёбер
        assertTrue(hasEdge(edges, "node4", "node3", 1.0));
        assertTrue(hasEdge(edges, "node5", "node1", 1.0));
        assertTrue(hasEdge(edges, "node2", "node1", 1.0));
    }

    @Test
    void testLoadDirectedWeighted() throws IOException {
        // Given
        Path filePath = testFilesDir.resolve("directed_weighted.txt");
        assertTrue(Files.exists(filePath), "Файл не найден: " + filePath);

        // When
        Graph<String, Double> graph = new Graph<>(
                filePath.toString(),
                s -> s,
                Double::parseDouble,
                " "
        );

        // Then
        assertTrue(graph.isDirected());
        assertEquals(8, graph.getVertexCount());

        List<Graph.Edge<String, Double>> edges = graph.getEdgeList();
        assertEquals(8, edges.size());

        // Проверяем наличие конкретных рёбер
        assertTrue(hasEdge(edges, "1", "2", 10.5));
        assertTrue(hasEdge(edges, "3", "3", 1.0));
        assertTrue(hasEdge(edges, "4", "5", 7.2));
    }

    @Test
    void testLoadUndirectedWeighted() throws IOException {
        // Given
        Path filePath = testFilesDir.resolve("undirected_weighted.txt");
        assertTrue(Files.exists(filePath), "Файл не найден: " + filePath);

        // When
        Graph<String, Double> graph = new Graph<>(
                filePath.toString(),
                s -> s,
                Double::parseDouble,
                " "
        );

        // Then
        assertFalse(graph.isDirected());
        assertEquals(10, graph.getVertexCount());

        List<Graph.Edge<String, Double>> edges = graph.getEdgeList();
        assertEquals(9, edges.size());

        // Проверяем наличие конкретных рёбер
        assertTrue(hasEdge(edges, "A", "B", 1.0));
        assertTrue(hasEdge(edges, "A", "G", 10.0));
        assertTrue(hasEdge(edges, "G", "G", 0.5));
        assertTrue(hasEdge(edges, "H", "I", 1.1));
    }

    @Test
    void testLoadDirectedUnweighted() throws IOException {
        // Given
        Path filePath = testFilesDir.resolve("directed_unweighted.txt");
        assertTrue(Files.exists(filePath), "Файл не найден: " + filePath);

        // When
        Graph<String, Double> graph = new Graph<>(
                filePath.toString(),
                s -> s,
                Double::parseDouble,
                " "
        );

        // Then
        assertTrue(graph.isDirected());
        assertEquals(7, graph.getVertexCount());

        List<Graph.Edge<String, Double>> edges = graph.getEdgeList();
        assertEquals(7, edges.size());

        // Проверяем наличие конкретных рёбер
        assertTrue(hasEdge(edges, "V1", "V1", 1.0));
        assertTrue(hasEdge(edges, "V6", "V1", 1.0));
        assertTrue(hasEdge(edges, "V1", "V2", 1.0));
    }

    @Test
    void testAddVertex() throws IOException {
        // Given
        Graph<String, Double> graph = createEmptyGraph(false);
        assertEquals(0, graph.getVertexCount());

        // When
        graph.addVertex("A");
        graph.addVertex("B");

        // Then
        assertEquals(2, graph.getVertexCount());

        // Проверяем, что нельзя добавить существующую вершину
        assertThrows(IllegalStateException.class, () -> graph.addVertex("A"));
    }

    @Test
    void testAddEdge() throws IOException {
        // Given
        Graph<String, Double> graph = createEmptyGraph(false);
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        assertEquals(0, graph.getEdgeList().size());

        // When
        graph.addEdge("A", "B", 5.0);
        graph.addEdge("B", "C", 3.0);

        // Then
        assertEquals(2, graph.getEdgeList().size());

        // Проверяем, что нельзя добавить существующее ребро
        assertThrows(IllegalStateException.class, () -> graph.addEdge("A", "B", 10.0));

        // Проверяем, что нельзя добавить ребро с несуществующей вершиной
        assertThrows(NoSuchElementException.class, () -> graph.addEdge("A", "X", 1.0));
    }

    @Test
    void testRemoveVertex() throws IOException {
        // Given
        Graph<String, Double> graph = createEmptyGraph(false);
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addEdge("A", "B", 1.0);
        graph.addEdge("B", "C", 2.0);
        assertEquals(3, graph.getVertexCount());
        assertEquals(2, graph.getEdgeList().size());

        // When
        graph.removeVertex("B");

        // Then
        assertEquals(2, graph.getVertexCount());
        assertEquals(0, graph.getEdgeList().size()); // Все рёбра с B должны исчезнуть

        // Проверяем, что нельзя удалить несуществующую вершину
        assertThrows(NoSuchElementException.class, () -> graph.removeVertex("X"));
    }

    @Test
    void testRemoveEdge() throws IOException {
        // Given
        Graph<String, Double> graph = createEmptyGraph(false);
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addEdge("A", "B", 1.0);
        assertEquals(1, graph.getEdgeList().size());

        // When
        graph.removeEdge("A", "B");

        // Then
        assertEquals(0, graph.getEdgeList().size());

        // Проверяем, что нельзя удалить несуществующее ребро
        assertThrows(IllegalStateException.class, () -> graph.removeEdge("A", "B"));
    }

    @Test
    void testSaveAndLoad() throws IOException {
        // Given
        Graph<String, Double> originalGraph = createEmptyGraph(true);
        originalGraph.addVertex("1");
        originalGraph.addVertex("2");
        originalGraph.addVertex("3");
        originalGraph.addEdge("1", "2", 10.5);
        originalGraph.addEdge("2", "3", 5.0);
        originalGraph.addEdge("3", "1", 2.0);

        Path tempFile = tempDir.resolve("test_graph.txt");

        // When
        originalGraph.saveToFile(tempFile.toString(), " ");
        Graph<String, Double> loadedGraph = new Graph<>(
                tempFile.toString(),
                s -> s,
                Double::parseDouble,
                " "
        );

        // Then
        assertEquals(originalGraph.isDirected(), loadedGraph.isDirected());
        assertEquals(originalGraph.getVertexCount(), loadedGraph.getVertexCount());
        assertEquals(originalGraph.getEdgeList().size(), loadedGraph.getEdgeList().size());

        // Проверяем наличие всех рёбер
        List<Graph.Edge<String, Double>> originalEdges = originalGraph.getEdgeList();
        List<Graph.Edge<String, Double>> loadedEdges = loadedGraph.getEdgeList();

        for (Graph.Edge<String, Double> edge : originalEdges) {
            assertTrue(hasEdge(loadedEdges, edge.source, edge.dest, edge.weight));
        }
    }

    @Test
    void testDirectedVsUndirected() throws IOException {
        // Given
        Graph<String, Double> directedGraph = createEmptyGraph(true);
        directedGraph.addVertex("A");
        directedGraph.addVertex("B");
        directedGraph.addEdge("A", "B", 1.0);

        Graph<String, Double> undirectedGraph = createEmptyGraph(false);
        undirectedGraph.addVertex("A");
        undirectedGraph.addVertex("B");
        undirectedGraph.addEdge("A", "B", 1.0);

        // When
        List<Graph.Edge<String, Double>> directedEdges = directedGraph.getEdgeList();
        List<Graph.Edge<String, Double>> undirectedEdges = undirectedGraph.getEdgeList();

        // Then
        assertEquals(1, directedEdges.size()); // В ориентированном только A->B
        assertEquals(1, undirectedEdges.size()); // В неориентированном одно ребро, но оно двунаправленное

        // Проверяем, что в неориентированном графе ребро есть в обе стороны
        assertTrue(hasEdge(undirectedEdges, "A", "B", 1.0));
    }

    @Test
    void testInvalidFileFormat() {
        // Given
        Path invalidFile = tempDir.resolve("invalid.txt");
        String content = "invalid\nA B\nA B 1.0";

        try {
            Files.writeString(invalidFile, content);
        } catch (IOException e) {
            fail("Не удалось создать тестовый файл");
        }

        // Then
        assertThrows(IOException.class, () -> new Graph<>(
                invalidFile.toString(),
                s -> s,
                Double::parseDouble,
                " "
        ));
    }

    // Вспомогательные методы

    private Graph<String, Double> createEmptyGraph(boolean directed) {
        return new Graph<>(directed);
    }

    private boolean hasEdge(List<Graph.Edge<String, Double>> edges, String source, String dest, double weight) {
        return edges.stream().anyMatch(e ->
                e.source.equals(source) &&
                        e.dest.equals(dest) &&
                        Math.abs(e.weight - weight) < 0.0001
        );
    }
}