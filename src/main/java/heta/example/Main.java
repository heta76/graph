package heta.example;

import java.util.List;
import java.util.Scanner;
import java.io.IOException;
import java.util.StringJoiner;

public class Main {
    private static IGraph<String, Double> graph;
    private static boolean isDirty = false; // Были ли изменения с момента последнего сохранения
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Программа для работы с графами ===");

        // 1. Начальный выбор: создать или загрузить
        while (graph == null) {
            startupMenu();
        }

        // 2. Основной цикл программы
        runMainMenu();
    }

    private static void startupMenu() {
        // Если граф существует и есть несохраненные изменения
        if (graph != null && isDirty) {
            System.out.print("У вас есть несохраненные изменения. Уверены, что хотите выйти в меню инициализации? (y/n): ");
            String confirm = scanner.nextLine().toLowerCase();
            if (!confirm.equals("y")) {
                return; // Возвращаемся в главное меню, ничего не удаляя
            }
        }
        while (true) {
            System.out.println("1. Создать новый пустой граф");
            System.out.println("2. Загрузить граф из файла");
            System.out.print("> ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> {
                    createNewGraph();
                    isDirty = false;
                    return;
                }
                case "2" -> {
                    loadFromFile();
                    if (graph != null) {
                        isDirty = false;
                        return;
                    }
                }
                default -> System.out.println(">>> Ошибка: Введите 1 или 2.");
            }
        }
    }

    private static void createNewGraph() {
        System.out.print("Граф ориентированный? (y/n): ");
        boolean isDirected = scanner.nextLine().equalsIgnoreCase("y");

        System.out.print("Граф взвешенный? (y/n): ");
        boolean isWeighted = scanner.nextLine().equalsIgnoreCase("y");

        graph = new Graph<>(isDirected, isWeighted);
        System.out.println("Создан новый " +
                (isDirected ? "ориентированный " : "неориентированный ") +
                (isWeighted ? "взвешенный " : "невзвешенный ") + "граф.");
    }

    private static void runMainMenu() {
        boolean exit = false;
        while (!exit) {
            printMenu();
            String choice = scanner.nextLine();
            try {
                switch (choice) {
                    case "1" -> addVertex();
                    case "2" -> addEdge();
                    case "3" -> removeVertex();
                    case "4" -> removeEdge();
                    case "5" -> showAdjacencyList();
                    case "6" -> showStatus();
                    case "7" -> saveToFile();
                    case "8" -> startupMenu(); // Позволяет пересоздать/перезагрузить
                    case "9" -> showOutDegree();
                    case "10" -> showAllAdjacentVertices();
                    case "11" -> saveTransposeToFile();
                    case "12" -> checkTreeAfterRemovingVertex();
                    case "13" -> showGraphRadius();
                    case "14" -> showDijkstraPaths();
                    case "15" -> showBellmanFordPaths();
                    case "16" -> showFloydWarshall();
                    case "17" -> showMaxFlow();
                    case "18" -> buildMST();
                    case "0" -> exit = true;
                    default -> System.out.println("Неверный ввод.");
                }
            } catch (Exception e) {
                System.err.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void addEdge() {
        System.out.print("Откуда (вершина): ");
        String u = scanner.nextLine();
        System.out.print("Куда (вершина): ");
        String v = scanner.nextLine();

        double w = 1.0;
        if (graph.isWeighted()) {
            System.out.print("Введите вес ребра: ");
            w = Double.parseDouble(scanner.nextLine());
        }

        graph.addEdge(u, v, w);
        isDirty = true;
        System.out.println("Ребро добавлено.");
    }

    private static void showStatus() {
        System.out.println("Тип: " + (graph.isDirected() ? "Directed" : "Undirected"));
        System.out.println("Режим: " + (graph.isWeighted() ? "Weighted" : "Unweighted (default weight 1.0)"));
        System.out.println("Количество вершин: " + graph.getVertexCount());
    }

    // --- Остальные методы (без изменений или с минимальной правкой текста) ---

    private static void addVertex() {
        System.out.print("Введите имя вершины: ");
        String v = scanner.nextLine();
        graph.addVertex(v);
        isDirty = true;
        System.out.println("Вершина добавлена.");
    }

    private static void removeVertex() {
        System.out.print("Имя вершины для удаления: ");
        String v = scanner.nextLine();
        graph.removeVertex(v);
        isDirty = true;
        System.out.println("Вершина удалена.");
    }

    private static void removeEdge() {
        System.out.print("Удалить ребро из: ");
        String u = scanner.nextLine();
        System.out.print("Удалить ребро в: ");
        String v = scanner.nextLine();
        graph.removeEdge(u, v);
        isDirty = true;
        System.out.println("Ребро удалено.");
    }

    private static void showAdjacencyList() {
        System.out.println("\n--- Список смежности ---");

        var structure = graph.getAdjacencyStructure();

        if (structure.isEmpty()) {
            System.out.println("Граф пуст.");
            return;
        }

        for (var entry : structure.entrySet()) {
            String vertex = entry.getKey();
            java.util.Map<String, Double> neighbors = entry.getValue();

            System.out.print(vertex + ": ");

            if (neighbors.isEmpty()) {
                System.out.println("(нет соседей)");
                continue;
            }

            StringJoiner sj = new StringJoiner(", ");
            for (var neighbor : neighbors.entrySet()) {
                String weightInfo = graph.isWeighted() ? "(" + neighbor.getValue() + ")" : "";
                sj.add(neighbor.getKey() + weightInfo);
            }
            System.out.println(sj.toString());
        }
    }

    private static void saveToFile() {
        System.out.print("Введите имя файла: ");
        String path = scanner.nextLine();
        try {
            graph.saveToFile(path, " ");
            isDirty = false;
            System.out.println("Успешно сохранено.");
        } catch (IOException e) {
            System.err.println("Ошибка записи: " + e.getMessage());
        }
    }

    private static void loadFromFile() {
        System.out.print("Путь к файлу: ");
        String path = scanner.nextLine();
        try {
            graph = new Graph<>(path, s -> s, Double::parseDouble, 1.0, " ");
            System.out.println("Граф загружен.");
        } catch (IOException e) {
            System.out.println("Файл не найден.");
            graph = null;
        } catch (Exception e) {
            System.out.println("Ошибка формата.");
            graph = null;
        }
    }

    private static void showOutDegree() {

        if (!graph.isDirected()) {
            System.out.println("Граф не является ориентированным.");
            return;
        }

        System.out.print("Введите вершину: ");
        String v = scanner.nextLine();

        try {
            int degree = graph.getOutDegree(v);
            System.out.println("Полустепень исхода вершины " + v + " = " + degree);
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void showAdjacentVertices(String v) {
        try {
            var neighbors = graph.getAdjacentVertices(v);

            if (neighbors.isEmpty()) {
                System.out.println("У вершины нет исходящих рёбер.");
                return;
            }

            System.out.println("Смежные исходящие вершины: " + String.join(", ", neighbors));

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void showIncomingVertices(String v) {
        try {
            var neighbors = graph.getIncomingVertices(v);

            if (neighbors.isEmpty()) {
                System.out.println("У вершины нет входящих рёбер.");
                return;
            }

            System.out.println("Смежные входящие вершины: " + String.join(", ", neighbors));

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void showAllAdjacentVertices() {
        if (!graph.isDirected()) {
            System.out.println("Граф не является ориентированным.");
            return;
        }

        System.out.print("Введите вершину: ");
        String v = scanner.nextLine();

        showAdjacentVertices(v);
        showIncomingVertices(v);
    }

    private static void saveTransposeToFile() {
        if (!graph.isDirected()) {
            System.out.println("Граф не является ориентированным — обращение совпадает с исходным (сохраните обычный граф).");
            return;
        }

        System.out.print("Путь для сохранения обращённого графа: ");
        String out = scanner.nextLine();
        try {

            IGraph<String, Double> trans = graph.getTranspose();
            trans.saveToFile(out, " ");

            System.out.println("Обращённый граф сохранён в " + out);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    private static void checkTreeAfterRemovingVertex() {
        if (graph.isDirected()) {
            System.out.println("Задание определено только для неориентированного графа.");
            return;
        }

        String vertexName = graph.findVertexToRemoveToMakeTree();

        if (vertexName != null) {
            System.out.println("Можно удалить вершину так, что получится дерево.");
            System.out.println("Кандидат для удаления: " + vertexName);
        } else {
            System.out.println("Невозможно получить дерево удалением одной вершины.");
        }
    }

    private static void buildMST() {
        if (graph.isDirected()) {
            System.out.println("Ошибка: Алгоритм Краскала предназначен для неориентированных графов.");
            return;
        }

        try {
            // Вызываем метод
            IGraph<String, Double> mst = ((Graph<String, Double>) graph).getKruskalMST();

            System.out.println("Каркас минимального веса построен.");
            System.out.println("Хотите заменить текущий граф на полученный остов? (y/n)");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                graph = mst;
                isDirty = true;
                System.out.println("Текущий граф заменен на MST.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }



    private static void showGraphRadius() {
        try {
            double radius = ((Graph<String, Double>) graph).getRadius();

            if (radius == Integer.MAX_VALUE) {
                System.out.println("Граф несвязный, радиус определить невозможно (равен бесконечности).");
            } else {
                System.out.println("Радиус графа: " + (int)radius);
            }
        } catch (Exception e) {
            System.out.println("Ошибка при расчете: " + e.getMessage());
        }
    }

    private static void showDijkstraPaths() {
        if (!graph.isWeighted()) {
            System.out.println("Задача требует взвешенный граф.");
            return;
        }

        System.out.print("Введите вершину u: ");
        String u = scanner.nextLine();

        try {
            var g = graph;
            var res = graph.dijkstra(u);

            for (String v : graph.getAdjacencyStructure().keySet()) {
                double d = res.getDistance(v);
                if (Double.isInfinite(d)) {
                    System.out.println(u + " -> " + v + ": пути нет");
                } else {
                    System.out.println(
                            u + " -> " + v + " = " + d +
                                    " | путь: " + String.join(" -> ", res.buildPathTo(v))
                    );
                }
            }
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void showBellmanFordPaths() {
        if (!graph.isWeighted()) {
            System.out.println("Задача требует взвешенный граф.");
            return;
        }

        System.out.print("Введите вершину u1: ");
        String u1 = scanner.nextLine();

        System.out.print("Введите вершину u2: ");
        String u2 = scanner.nextLine();

        System.out.print("Введите вершину v: ");
        String v = scanner.nextLine();

        try {
            Graph<String, Double> g = (Graph<String, Double>) graph;

            var r1 = g.bellmanFord(u1);
            double d1 = r1.getDistance(v);
            if (Double.isInfinite(d1)) {
                System.out.println("Из " + u1 + " до " + v + " пути нет.");
            } else {
                System.out.println(
                        u1 + " -> " + v + " = " + d1 +
                                " | путь: " + String.join(" -> ", r1.buildPathTo(v))
                );
            }

            var r2 = g.bellmanFord(u2);
            double d2 = r2.getDistance(v);
            if (Double.isInfinite(d2)) {
                System.out.println("Из " + u2 + " до " + v + " пути нет.");
            } else {
                System.out.println(
                        u2 + " -> " + v + " = " + d2 +
                                " | путь: " + String.join(" -> ", r2.buildPathTo(v))
                );
            }

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void showFloydWarshall() {
        if (!graph.isWeighted()) {
            System.out.println("Задача требует взвешенный граф.");
            return;
        }

        try {
            var res = graph.floydWarshall();
            var vertices = res.getVertices();

            // 1. Отрисовка таблицы
            int colWidth = 10;
            System.out.print(" ".repeat(6));
            for (String v : vertices) System.out.printf("%" + colWidth + "s", v);
            System.out.println();

            for (String from : vertices) {
                System.out.printf("%6s", from);
                for (String to : vertices) {
                    double d = res.getDistance(from, to);
                    String cell = formatDistance(d);
                    System.out.printf("%" + colWidth + "s", cell);
                }
                System.out.println();
            }

            if (res.hasNegativeCycle()) {
                System.out.println("\n[!] ВНИМАНИЕ: В графе обнаружены отрицательные циклы.");
                System.out.println("Расстояния до некоторых вершин бесконечно уменьшаются (-∞).");
            }

            // 2. Интерактивный поиск пути
            System.out.println("\n--- Детализация пути ---");
            while (true) {
                System.out.print("Введите начальную и конечную вершины через пробел (или 'exit' для выхода): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) break;

                String[] parts = input.split("\\s+");
                if (parts.length != 2) {
                    System.out.println("Нужно ввести ровно две вершины.");
                    continue;
                }

                String start = parts[0];
                String end = parts[1];

                if (!vertices.contains(start) || !vertices.contains(end)) {
                    System.out.println("Одна или обе вершины не найдены в графе.");
                    continue;
                }

                double dist = res.getDistance(start, end);
                List<String> path = res.getPath(start, end);

                System.out.println("Расстояние: " + formatDistance(dist));
                if (path == null) {
                    System.out.println("Путь: Не определен (проходит через отрицательный цикл)");
                } else if (path.isEmpty()) {
                    System.out.println("Путь: Не существует");
                } else {
                    System.out.println("Путь: " + String.join(" -> ", path));
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void showMaxFlow() {
        if (!graph.isDirected()) {
            System.out.println("Ошибка: максимальный поток задаётся для ориентированного графа.");
            return;
        }

        if (!graph.isWeighted()) {
            System.out.println("Ошибка: для максимального потока нужны веса как пропускные способности.");
            return;
        }

        System.out.print("Источник (s): ");
        String s = scanner.nextLine();

        System.out.print("Сток (t): ");
        String t = scanner.nextLine();

        try {
            Graph.MaxFlowResult<String> result = graph.edmondsKarp(s, t);

            System.out.println("Максимальный поток = " + formatDistance(result.getMaxFlow()));

            System.out.println("\nПути увеличения:");
            List<List<String>> paths = result.getAugmentingPaths();
            List<Double> bottlenecks = result.getBottlenecks();

            for (int i = 0; i < paths.size(); i++) {
                System.out.println(
                        (i + 1) + ") " + String.join(" -> ", paths.get(i)) +
                                " | приращение = " + formatDistance(bottlenecks.get(i))
                );
            }

            System.out.println("\nПотоки по рёбрам:");
            var flows = result.getFlows();
            for (var entry : flows.entrySet()) {
                String u = entry.getKey();
                for (var e : entry.getValue().entrySet()) {
                    double f = e.getValue();
                    if (Math.abs(f) > 1e-12) {
                        System.out.println(u + " -> " + e.getKey() + " : " + formatDistance(f));
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    // Вспомогательный метод для красивого вывода чисел
    private static String formatDistance(double d) {
        if (d == Double.POSITIVE_INFINITY) return "∞";
        if (d == Double.NEGATIVE_INFINITY) return "-∞";
        if (d == (long) d) return String.format("%d", (long) d);
        return String.format("%.2f", d);
    }

    private static void printMenu() {
        System.out.println("\n--- Управление графом ---");
        System.out.println("1. Добавить вершину");
        System.out.println("2. Добавить ребро");
        System.out.println("3. Удалить вершину");
        System.out.println("4. Удалить ребро");
        System.out.println("5. Список смежности");
        System.out.println("6. Свойства (тип/вес)");
        System.out.println("7. Сохранить");
        System.out.println("8. Пересоздать / Загрузить другой");
        System.out.println("9. Полустепень исхода вершины");
        System.out.println("10. Смежные вершины");
        System.out.println("11. Сохранить обращённый орграф в файл");
        System.out.println("12. Проверить, можно ли из графа удалить какую-либо вершину так, чтобы получилось дерево.");
        System.out.println("13. Найти радиус графа — минимальный из эксцентриситетов его вершин.");
        System.out.println("14. Дейкстра: кратчайшие пути из вершины u");
        System.out.println("15. Беллман-Форд: кратчайшие пути из u1 и u2 до v");
        System.out.println("16. Флойд: длины кратчайших путей для всех пар");
        System.out.println("17. Максимальный поток (Эдмондс–Карп)");
        if (!graph.isDirected() && graph.isWeighted()) {
            System.out.println("18. Найти каркас минимального веса ");
        }
        System.out.println("0. Выход");
        System.out.print("> ");
    }
}