package heta.example;

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
                    case "14" -> buildMST();
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

        // Если метод добавлен в интерфейс, приведение типа не нужно
        String vertexName = ((Graph<String, Double>) graph).findVertexToRemoveToMakeTree();

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
        if (!graph.isDirected() && graph.isWeighted()) {
            System.out.println("14. Найти каркас минимального веса ");
        }
        System.out.println("0. Выход");
        System.out.print("> ");
    }
}