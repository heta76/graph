package heta.example;


import java.io.File;
import java.io.IOException;
import java.util.List;

public class GraphTest1 {
    public static void main(String[] args) {
        // Папка с тестовыми файлами (измените, если файлы лежат в другом месте)
        String testFolder = "C:\\Users\\user\\Downloads\\demo\\graph\\src\\main\\java\\heta\\example";

        // Массив тестовых файлов
        String[] testFiles = {
                "undirected_unweighted.txt",
                "directed_weighted.txt",
                "undirected_weighted.txt",
                "directed_unweighted.txt"
        };

        System.out.println("=== НАЧАЛО ТЕСТИРОВАНИЯ КЛАССА Graph ===\n");

        for (String fileName : testFiles) {
            String filePath = testFolder + File.separator + fileName;
            System.out.println(">>> ТЕСТИРУЕМ ФАЙЛ: " + fileName + " <<<\n");

            try {
                // 1. Загрузка графа из файла
                Graph<String, Double> graph = new Graph<>(
                        filePath,
                        s -> s,               // парсер вершин: строку оставляем как есть
                        Double::parseDouble,   // парсер весов: преобразуем в Double
                        " "                    // разделитель (пробел)
                );

                // Вывод информации о загруженном графе
                System.out.println("Граф успешно загружен:");
                System.out.println("  Тип: " + (graph.isDirected() ? "ориентированный" : "неориентированный"));
                System.out.println("  Количество вершин: " + graph.getVertexCount());

                List<Graph.Edge<String, Double>> edges = graph.getEdgeList();
                System.out.println("  Количество рёбер: " + edges.size());
                System.out.println("  Рёбра (source -> dest = weight):");
                for (Graph.Edge<String, Double> e : edges) {
                    System.out.println("    " + e.source + " -> " + e.dest + " = " + e.weight);
                }

                // 2. Операции с графом
                System.out.println("\n--- Операции ---");

                // Добавление новой вершины
                String newVertex = "NEW_VERTEX";
                System.out.print("Добавляем вершину " + newVertex + "... ");
                graph.addVertex(newVertex);
                System.out.println("OK (теперь вершин: " + graph.getVertexCount() + ")");

                // Добавление ребра (если есть вершины, чтобы добавить)
                if (edges.size() > 0) {
                    Graph.Edge<String, Double> firstEdge = edges.get(0);
                    String source = firstEdge.source;
                    String dest = firstEdge.dest;
                    Double weight = firstEdge.weight;

                    // Пытаемся добавить уже существующее ребро — должно выбросить исключение
                    System.out.print("Пытаемся добавить уже существующее ребро " + source + " -> " + dest + "... ");
                    try {
                        graph.addEdge(source, dest, weight);
                        System.out.println("ОШИБКА: исключение не выброшено!");
                    } catch (IllegalStateException e) {
                        System.out.println("OK (получено ожидаемое исключение: " + e.getMessage() + ")");
                    }

                    // Добавляем ребро от новой вершины к существующей
                    System.out.print("Добавляем ребро " + newVertex + " -> " + source + " с весом 42.0... ");
                    graph.addEdge(newVertex, source, 42.0);
                    System.out.println("OK (теперь рёбер: " + graph.getEdgeList().size() + ")");
                }

                // Удаление ребра
                if (edges.size() > 0) {
                    Graph.Edge<String, Double> someEdge = edges.get(0);
                    System.out.print("Удаляем ребро " + someEdge.source + " -> " + someEdge.dest + "... ");
                    graph.removeEdge(someEdge.source, someEdge.dest);
                    System.out.println("OK (теперь рёбер: " + graph.getEdgeList().size() + ")");
                }

                // Удаление вершины
                System.out.print("Удаляем вершину " + newVertex + "... ");
                graph.removeVertex(newVertex);
                System.out.println("OK (теперь вершин: " + graph.getVertexCount() + ")");

                // 3. Сохранение в новый файл
                String outputFile = testFolder + File.separator + "output_" + fileName;
                System.out.print("Сохраняем граф в файл " + outputFile + "... ");
                graph.saveToFile(outputFile, " ");
                System.out.println("OK");

                // 4. Загрузка сохранённого файла для проверки целостности
                System.out.print("Загружаем сохранённый граф обратно... ");
                Graph<String, Double> reloadedGraph = new Graph<>(
                        outputFile,
                        s -> s,
                        Double::parseDouble,
                        " "
                );
                System.out.println("OK");
                System.out.println("  Загружено вершин: " + reloadedGraph.getVertexCount() +
                        ", рёбер: " + reloadedGraph.getEdgeList().size());

            } catch (IOException e) {
                System.err.println("!!! ОШИБКА ВВОДА/ВЫВОДА: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("!!! НЕПРЕДВИДЕННАЯ ОШИБКА: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("\n----------------------------------------\n");
        }

        System.out.println("=== ТЕСТИРОВАНИЕ ЗАВЕРШЕНО ===");
    }
}
