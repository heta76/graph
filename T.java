import heta.example.Graph;
public class T {
  public static void main(String[] a) throws Exception {
    var g = new Graph<>("A:/Java/Graphs/src/main/java/heta/example/output_undirected_weighted.txt", s->s, Double::parseDouble, 1.0, " ");
    System.out.println("V="+g.getVertexCount()+" E="+g.getEdgeList().size()+" dir="+g.isDirected());
    var c = g.findVertexToRemoveToMakeTree();
    System.out.println("candidate="+c);
  }
}
