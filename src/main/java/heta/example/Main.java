package heta.example;

import java.util.Scanner;
import java.io.IOException;



public class Main {
    private static IGraph<String, Double> graph;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("Добро пожаловать в Graph UI!");
        initGraph();

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
                    case "6" -> showDirection();
                    case "7" -> saveToFile();
                    case "8" -> loadFromFile();
                    case "0" -> exit = true;
                    default -> System.out.println("Неверный ввод.");
                }
            } catch (Exception e) {
                System.err.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private static void initGraph() {
        System.out.print("Создать ориентированный граф? (y/n): ");
        boolean isDirected = scanner.nextLine().equalsIgnoreCase("y");
        graph = new Graph<>(isDirected);
    }

    private static void addVertex() {
        System.out.print("Введите имя вершины: ");
        String v = scanner.nextLine();
        graph.addVertex(v);
        System.out.println("Вершина добавлена.");
    }

    private static void showDirection() {
        if (graph.isDirected()){
            System.out.println("directed");
        }
        else {
            System.out.println("undirected");
        }
    }

    private static void addEdge() {
        System.out.print("Откуда (вершина): ");
        String u = scanner.nextLine();
        System.out.print("Куда (вершина): ");
        String v = scanner.nextLine();
        System.out.print("Вес: ");
        double w = Double.parseDouble(scanner.nextLine());
        graph.addEdge(u, v, w);
        System.out.println("Ребро добавлено.");
    }

    private static void removeVertex() {
        System.out.print("Имя вершины для удаления: ");
        String v = scanner.nextLine();
        graph.removeVertex(v);
        System.out.println("Вершина удалена.");
    }

    private static void removeEdge() {
        System.out.print("Удалить ребро из: ");
        String u = scanner.nextLine();
        System.out.print("Удалить ребро в: ");
        String v = scanner.nextLine();
        graph.removeEdge(u, v);
        System.out.println("Ребро удалено.");
    }

    private static void showAdjacencyList() {
        System.out.println("\n--- Список смежности ---");
        // В реальном проекте здесь можно добавить метод в IGraph для получения структуры,
        // но мы используем getEdgeList для демонстрации
        var edges = graph.getEdgeList();
        if (edges.isEmpty() && graph.getVertexCount() > 0) {
            System.out.println("В графе только изолированные вершины.");
        }
        for (var edge : edges) {
            System.out.println(edge.source + " --(" + edge.weight + ")--> " + edge.dest);
        }
    }

    private static void saveToFile() {
        System.out.print("Введите имя файла для сохранения: ");
        String path = scanner.nextLine();
        try {
            graph.saveToFile(path, " ");
            System.out.println("Сохранено.");
        } catch (Exception e) {
            System.out.println("Ошибка записи: " + e.getMessage());
        }
    }

    private static void loadFromFile() {
        System.out.print("Введите путь к файлу (например, directed_weighted.txt): ");
        String path = scanner.nextLine();
        try {
            // Используем String для вершин и Double для весов.
            // s -> s — это лямбда, которая оставляет строку как есть (имя вершины).
            // Double::parseDouble — парсит строку веса в число.
            graph = new Graph<>(path, s -> s, Double::parseDouble, " ");
            System.out.println("Граф успешно загружен из файла!");
            System.out.println("Загружено вершин: " + graph.getVertexCount());
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Ошибка формата данных: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\nМеню:");
        System.out.println("1. Добавить вершину");
        System.out.println("2. Добавить ребро");
        System.out.println("3. Удалить вершину");
        System.out.println("4. Удалить ребро");
        System.out.println("5. Показать список ребер");
        System.out.println("6. Вывести тип графа");
        System.out.println("7. Сохранить в файл");
        System.out.println("8. Загрузить из файла (пересоздать)");
        System.out.println("0. Выход");
        System.out.print("> ");
    }
}