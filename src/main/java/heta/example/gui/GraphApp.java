package heta.example.gui;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import heta.example.gui.GraphCanvasPane;   // должно быть
import heta.example.gui.GraphGuiController;
// ... остальные импорты
/**
 * Точка входа GUI. Запуск: {@code mvn javafx:run}
 * или main-класс {@code heta.example.gui.GraphApp}.
 */
public class GraphApp extends Application {
    @Override
    public void start(Stage stage) {
        GraphCanvasPane canvas = new GraphCanvasPane();
        canvas.setMinSize(400, 300);
        TextArea output = new TextArea();
        output.setEditable(false);
        output.setWrapText(true);
        output.setMinHeight(120);
        output.setPromptText("Результаты алгоритмов и сообщения…");
        GraphGuiController ctrl = new GraphGuiController(canvas, output);
        VBox left = new VBox(6,
                section("Граф"),
                btn("Новый граф", ctrl::createNewGraph),
                btn("Загрузить из файла", ctrl::loadFromFile),
                btn("Сохранить", ctrl::saveToFile),
                btn("Сохранить раскладку", ctrl::saveLayout),
                btn("Свойства", ctrl::showStatus),
                btn("Список смежности", ctrl::showAdjacency),
                btn("Переразложить", ctrl::relayout),
                new Separator(),
                section("Редактирование"),
                btn("Добавить вершину (клик на холсте)", ctrl::addVertex),
                btn("Добавить ребро", ctrl::addEdge),
                btn("Удалить вершину", ctrl::removeVertex),
                btn("Удалить ребро", ctrl::removeEdge),
                new Separator(),
                section("Алгоритмы"),
                btn("Дейкстра", ctrl::runDijkstra),
                btn("Беллман–Форд", ctrl::runBellmanFord),
                btn("Флойд–Уоршелл", ctrl::runFloyd),
                btn("Макс. поток (Эдмондс–Карп)", ctrl::runMaxFlow),
                btn("MST (Краскал)", ctrl::runMst),
                btn("Радиус графа", ctrl::showRadius),
                btn("Вершина → дерево", ctrl::checkTreeVertex),
                btn("Полустепень исхода", ctrl::showOutDegree),
                btn("Сохранить обращение", ctrl::saveTranspose),
                new Separator(),
                btn("Очистить вывод / подсветку", ctrl::clearOutput)
        );
        left.setPadding(new Insets(10));
        left.setFillWidth(true);
        ScrollPane leftScroll = new ScrollPane(left);
        leftScroll.setFitToWidth(true);
        leftScroll.setPrefWidth(240);
        leftScroll.setMinWidth(200);
        VBox right = new VBox(6, new Label("Вывод"), output);
        VBox.setVgrow(output, Priority.ALWAYS);
        right.setPadding(new Insets(10));
        right.setPrefWidth(280);
        right.setMinWidth(180);
        SplitPane centerSplit = new SplitPane(canvas, right);
        centerSplit.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        centerSplit.setDividerPositions(0.68);
        SplitPane.setResizableWithParent(right, Boolean.TRUE);
        BorderPane root = new BorderPane();
        root.setLeft(leftScroll);
        root.setCenter(centerSplit);
        Scene scene = new Scene(root, 1100, 650);
        stage.setTitle("Визуализатор графов");
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(500);
        stage.show();
        canvas.widthProperty().addListener((o, a, b) -> {
            if (ctrl.getGraph() != null) {
                canvas.redraw();
            }
        });
    }
    private static Label section(String title) {
        Label l = new Label(title);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }
    private static Button btn(String text, Runnable action) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        return b;
    }
    public static void main(String[] args) {
        launch(args);
    }
}