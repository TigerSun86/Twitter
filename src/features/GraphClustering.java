package features;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import bridgeCut.Evaluator;
import bridgeCut.Graph;
import bridgeCut.Node;

/**
 * FileName: GraphClustering.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 5, 2015 4:53:03 PM
 */
public class GraphClustering {
    private static final boolean WEIGHT = true;

    private Graph distanceTable2Graph (HashMap<String, Double> distanceTable) {
        Graph g = new Graph(false);
        for (Entry<String, Double> entry : distanceTable.entrySet()) {
            String[] pair = entry.getKey().split(SimTable.WORD_SEPARATER);
            String w1 = pair[0];
            String w2 = pair[1];
            Double dist = entry.getValue();
            if (!w1.equals(w2) && !Double.isInfinite(dist)) {
                Node n1 = g.get(w1);
                if (n1 == null) {
                    n1 = new Node(w1);
                    g.put(w1, n1);
                }

                Node n2 = g.get(w2);
                if (n2 == null) {
                    n2 = new Node(w2);
                    g.put(w2, n2);
                }
                if (WEIGHT) {
                    n1.addNeighbor(w2, dist); // Edge n1 to n2
                    n2.addNeighbor(w1, dist); // Edge n2 to n1
                } else {
                    n1.addNeighbor(w2, 1); // Edge n1 to n2
                    n2.addNeighbor(w1, 1); // Edge n2 to n1
                }
            }
        }
        return g;
    }

    public void test (HashMap<String, Double> distanceTable) {

        Graph g = distanceTable2Graph(distanceTable);
        System.out.println("Init graph is:");
        System.out.println(g.toString());
        final boolean isBC = true;
        List<Graph> glist = g.bridgeCut(0.8, false, isBC);
        System.out.println("Cluster list is:");
        System.out.println(Graph.graphListToString(glist));
        System.out.println("Davis Bouldin Index is "
                + Evaluator.davisBouldinIndex(glist, g));
        System.out.println("Silhouette Coefficient is "
                + Evaluator.silhouetteCoefficient(glist, g));

    }

}
