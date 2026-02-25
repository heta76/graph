package heta.example;

import java.io.IOException;
import java.util.List;

public interface IGraph<TVertex, TWeight> {
    // Основные свойства
    boolean isDirected();
    int getVertexCount();
    boolean isWeighted();

    // Модификация
    void addVertex(TVertex vertex);
    void addEdge(TVertex source, TVertex destination, TWeight weight);
    void removeVertex(TVertex vertex);
    void removeEdge(TVertex source, TVertex destination);

    // Работа с данными
    List<Graph.Edge<TVertex, TWeight>> getEdgeList();
    void saveToFile(String filePath, String separator) throws IOException;
    java.util.Map<TVertex, java.util.Map<TVertex, TWeight>> getAdjacencyStructure();
}
