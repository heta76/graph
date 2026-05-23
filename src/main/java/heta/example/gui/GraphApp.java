package heta.example.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

/**
 * Точка входа GUI. Запуск: {@code mvn javafx:run}
 * или main-класс {@code heta.example.gui.GraphApp}.
 */
public class GraphApp extends Application {

    @Override
    public void start(Stage stage) {
        // ---------- холст ----------
        GraphCanvasPane canvas = new GraphCanvasPane();
        canvas.setMinSize(400, 300);

        // ---------- область вывода ----------
        TextArea output = new TextArea();
        output.setEditable(false);
        output.setMinHeight(120);
        output.setFont(javafx.scene.text.Font.font("Monospaced", 12));
        output.setWrapText(false);   // по умолчанию без переноса, для матриц

        // ---------- контроллер ----------
        GraphGuiController ctrl = new GraphGuiController(canvas, output);

        // ---------- элементы анимации ----------
        CheckBox animateCheck = new CheckBox("Анимация");
        animateCheck.setSelected(true);

        Slider speedSlider = new Slider(50, 1000, 200);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(200);
        speedSlider.setMinorTickCount(0);
        speedSlider.setBlockIncrement(50);

        Label speedLabel = new Label("Задержка: 200 мс");
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                speedLabel.setText("Задержка: " + newVal.intValue() + " мс"));

        Button replayBtn = btn("Повторить анимацию", ctrl::replayAnimation);

        // ---------- левая панель с кнопками ----------
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

                section("Анимация"),
                animateCheck,
                speedSlider,
                speedLabel,
                replayBtn,
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

        // ---------- правая часть (вывод) ----------
        VBox right = new VBox(6, new Label("Вывод"), output);
        VBox.setVgrow(output, Priority.ALWAYS);
        right.setPadding(new Insets(10));
        right.setPrefWidth(280);
        right.setMinWidth(180);

        // ---------- центральная область ----------
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

        // ---------- связывание анимации с контроллером ----------
        animateCheck.selectedProperty().addListener((obs, oldVal, newVal) ->
                ctrl.setAnimationEnabled(newVal));
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                ctrl.setAnimationDelay(newVal.intValue()));

        stage.show();

        // ---------- перерисовка при изменении размера холста ----------
        canvas.widthProperty().addListener((o, a, b) -> {
            if (ctrl.getGraph() != null) {
                canvas.redraw();
            }
        });
    }

    // ---------- вспомогательные методы для построения GUI ----------
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