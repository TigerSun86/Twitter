package features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import util.Dbg;
import features.ClusterWord.ClAlg;
import features.SimTable.Pair;

/**
 * FileName: SingleCutAlg.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 22, 2015 11:54:43 PM
 */
public class SingleCutAlg implements ClAlg {
    private static boolean DBG = false;
    private static final int maxNumOfSteps = 1000;

    int maxInitClusterSize = 20;
    int numOfCl = 10;
    boolean reattach = false;

    List<Set<String>> clusters = null;

    public SingleCutAlg(int numOfCl, boolean reattach) {
        this.numOfCl = numOfCl;
        this.reattach = reattach;
    }

    @Override
    public List<Set<String>> cluster (SimTable simTable, List<String> wordList) {
        clusters = new ArrayList<Set<String>>();

        simTable.findMaxMinValues();
        double step = (simTable.max - simTable.min) / (maxNumOfSteps - 2);
        double threshold = simTable.min;

        // Get initial singletons.
        Set<String> single = getSingletons(simTable, wordList);

        if (Dbg.dbg) {
            System.out.println("*****");
            System.out.println("Cutting stage.");
            System.out.printf("maxInitClusterSize: %d, numberOfClusters: %d, "
                    + "maxNumOfSteps: %d, maxValue: %.4f, "
                    + "minValue: %.4f, stepSize: %.5f%n", maxInitClusterSize,
                    numOfCl, maxNumOfSteps, simTable.max, simTable.min, step);
            System.out.printf("%d edges between %d words%n", simTable.size(),
                    wordList.size());
            System.out.println("Initial singletons: " + single.toString());
            if (DBG) System.out.println("Initial clusters:");
            // Remaining info printed in
            // removeSmallSubGraphsFromWholeGraph()
        }
        HashMap<String, Node> curGraph = getTheGraph(simTable);
        List<Set<String>> subGs = getMaximumConnectedGraphs(curGraph);
        removeSmallSubGraphsFromWholeGraph(curGraph, subGs);

        // Sort all edges.
        simTable.sortPairsAscendingly();
        List<Pair> sortedEdges = simTable.getPairs();

        // For re-attaching, should in descending order.
        LinkedList<Pair> visitedEdges = new LinkedList<Pair>();

        // Loop through all edges lowest to highest,
        for (Pair edge : sortedEdges) {
            if (curGraph.isEmpty()) {
                break;
            }

            String n1 = edge.w1;
            String n2 = edge.w2;
            // if the edge hasn't been removed.
            if (curGraph.containsKey(n1) && curGraph.get(n1).nei.contains(n2)) {
                assert curGraph.containsKey(n2)
                        && curGraph.get(n2).nei.contains(n1);
                // Cut one edge.
                curGraph.get(n1).nei.remove(n2);
                curGraph.get(n2).nei.remove(n1);
                visitedEdges.addFirst(edge); // Will be descending.
                if (Dbg.dbg && DBG) {
                    System.out.println("Cut edge: " + edge.toString());
                }
                if (threshold <= edge.v) {
                    threshold += step;
                    // Check isolated subgraph.
                    List<Set<String>> subGraphs =
                            getMaximumConnectedGraphs(curGraph);
                    // For all new small enough subgraph.
                    removeSmallSubGraphsFromWholeGraph(curGraph, subGraphs);
                }
            }
        }
        assert curGraph.isEmpty();

        Collections.sort(clusters, new ClusterComparator());
        if (Dbg.dbg && DBG) {
            System.out.println("*****");
            System.out.println("Clustering result:");
            for (int i = 0; i < clusters.size(); i++) {
                System.out.println("Cluster " + i + ":");
                System.out.println(clusters.get(i).size() + " "
                        + clusters.get(i).toString());
            }
        }

        List<Set<String>> selectedClusters = new ArrayList<Set<String>>();
        // Select top 10 clusters, clusters have already sorted before.
        for (int i = 0; i < Math.min(numOfCl, clusters.size()); i++) {
            selectedClusters.add(clusters.get(i));
        }

        if (reattach) {
            reattachStage(selectedClusters, visitedEdges);
        } else { // Just use selected clusters;
            clusters = selectedClusters;
        }

        if (Dbg.dbg) {
            System.out.println("*****");
            System.out.println("Final clustering result:");
            for (int i = 0; i < clusters.size(); i++) {
                System.out.println("Cluster " + i + ":");
                System.out.println(clusters.get(i).size() + " "
                        + clusters.get(i).toString());
            }
        }
        return clusters;
    }

    private void reattachStage (List<Set<String>> selectedClusters,
            LinkedList<Pair> visitedEdges) {
        if (Dbg.dbg) {
            System.out.println("*****");
            System.out.println("Re-attaching stage.");
            System.out.printf(
                    "maxInitClusterSize is %d, numberOfClusters is %d%n",
                    maxInitClusterSize, numOfCl);
        }
        // Selected node -> the sub graph it belongs to.
        HashMap<String, Set<String>> selectedNodes =
                new HashMap<String, Set<String>>();
        // Unselected node -> the sub graph it belongs to.
        HashMap<String, Set<String>> frontierNodes =
                new HashMap<String, Set<String>>();

        Set<Set<String>> seedClusters =
                new HashSet<Set<String>>(selectedClusters);
        for (Set<String> subG : clusters) {
            if (seedClusters.contains(subG)) {
                for (String node : subG) {
                    selectedNodes.put(node, subG);
                }
            } else {
                for (String node : subG) {
                    frontierNodes.put(node, subG);
                }
            }
        }
        clusters = selectedClusters;

        if (Dbg.dbg) {
            System.out.println("*****");

            System.out.println("Selected seed clusters:");
            for (int i = 0; i < clusters.size(); i++) {
                System.out.println("Cluster " + i + ":");
                System.out.println(clusters.get(i).toString());
            }
        }

        for (Pair edge : visitedEdges) {
            if (frontierNodes.isEmpty()) {
                break;
            }
            if (Dbg.dbg && DBG) {
                System.out.println("Trying to attach edge: " + edge.toString());
            }
            String n1 = edge.w1;
            String n2 = edge.w2;
            // One and only one set contains the node.
            assert selectedNodes.containsKey(n1) != frontierNodes
                    .containsKey(n1);
            assert selectedNodes.containsKey(n2) != frontierNodes
                    .containsKey(n2);
            boolean isN1In = selectedNodes.containsKey(n1);
            boolean isN2In = selectedNodes.containsKey(n2);
            if (isN1In != isN2In) {
                // One node from selected, one from frontier.
                String nodeIn = (isN1In ? n1 : n2);
                String nodeOut = (isN1In ? n2 : n1);
                Set<String> subGIn = selectedNodes.get(nodeIn);
                Set<String> subGOut = frontierNodes.get(nodeOut);
                for (String nodeOfSubGOut : subGOut) {
                    // Move the nodes of the sub graph at frontier into the
                    // sub graph at selected.
                    subGIn.add(nodeOfSubGOut);
                    selectedNodes.put(nodeOfSubGOut, subGIn);
                    frontierNodes.remove(nodeOfSubGOut);
                }
                if (Dbg.dbg && DBG) {
                    System.out.printf(
                            "Sub graph of node %s has joined by %s%n", nodeIn,
                            subGOut.toString());
                }
            }
        }
        // assert frontierNodes.isEmpty();
        Set<Set<String>> unattachedSubGs = new HashSet<Set<String>>();
        for (Set<String> subG : frontierNodes.values()) {
            unattachedSubGs.add(subG);
        }
        Collections.sort(clusters, new ClusterComparator());
        if (Dbg.dbg) {
            System.out.println("*****");
            System.out.println("Unattached clusters:");
            for (Set<String> subG : unattachedSubGs) {
                System.out.println(subG.toString());
            }
        }
    }

    public static Set<String> getSingletons (SimTable simTable,
            List<String> wordList) {
        Set<String> wordsOfTable = new HashSet<String>();
        for (Pair p : simTable.getPairs()) {
            wordsOfTable.add(p.w1);
            wordsOfTable.add(p.w2);
        }
        Set<String> single = new HashSet<String>();
        for (String s : wordList) {
            if (!wordsOfTable.contains(s)) {
                single.add(s);
            }
        }
        return single;
    }

    private void removeSmallSubGraphsFromWholeGraph (
            HashMap<String, Node> curGraph, List<Set<String>> subGs) {
        // Check each isolated subgraph.
        for (Set<String> subG : subGs) {
            if (subG.size() <= maxInitClusterSize) {
                // Add subgraph into cluster list.
                clusters.add(subG);
                // Remove all edges of subgraph from the whole
                // graph.
                for (String nodeToBeRemoved : subG) {
                    removeNode(curGraph, nodeToBeRemoved);
                }
                if (Dbg.dbg && DBG) {
                    System.out.println("New cluster: " + subG.toString());
                }
            }
        }
    }

    private static HashMap<String, Node> getTheGraph (SimTable simTable) {
        HashMap<String, Node> graph = new HashMap<String, Node>();
        for (Pair e : simTable.getPairs()) {
            String n1 = e.w1;
            String n2 = e.w2;
            Node node1 = graph.get(n1);
            if (node1 == null) {
                node1 = new Node(n1);
                graph.put(n1, node1);
            }
            node1.nei.add(n2);
            Node node2 = graph.get(n2);
            if (node2 == null) {
                node2 = new Node(n2);
                graph.put(n2, node2);
            }
            node2.nei.add(n1);
        }
        return graph;
    }

    private static void removeNode (HashMap<String, Node> graph, String n) {
        graph.remove(n);
        for (Entry<String, Node> entry : graph.entrySet()) {
            entry.getValue().nei.remove(n);
        }
    }

    private static Set<String> getMaximumConnectedGraphOfInitNode (
            HashMap<String, Node> graph, String initNode) {
        Set<String> outQue = new HashSet<String>();
        Set<String> inQue = new HashSet<String>();
        LinkedList<String> que = new LinkedList<String>();
        inQue.add(initNode);
        que.add(initNode);

        while (!que.isEmpty()) {
            String curN = que.removeFirst();
            inQue.remove(curN);
            outQue.add(curN);
            for (String nei : graph.get(curN).nei) {
                if (!outQue.contains(nei) && !inQue.contains(nei)) {
                    // Haven't visited and haven't planed to visit.
                    inQue.add(nei);
                    que.add(nei);
                }
            }
            // assert que.size() <= wordCount;
        }
        return outQue;
    }

    private static List<Set<String>> getMaximumConnectedGraphs (
            HashMap<String, Node> graph) {
        List<Set<String>> subGs = new ArrayList<Set<String>>();
        Set<String> nodeVisited = new HashSet<String>();
        for (String n : graph.keySet()) {
            if (!nodeVisited.contains(n)) {
                Set<String> subG = getMaximumConnectedGraphOfInitNode(graph, n);
                nodeVisited.addAll(subG);
                subGs.add(subG);
            }
        }

        return subGs;
    }

    private static class Node {
        String n;
        Set<String> nei;

        public Node(String n) {
            this.n = n;
            this.nei = new HashSet<String>();
        }

        @Override
        public String toString () {
            return String.format("%s: %d neighbors", n, nei.size());
        }

    }

    private static class ClusterComparator implements Comparator<Set<String>> {
        @Override
        public int compare (Set<String> o1, Set<String> o2) {
            return o2.size() - o1.size(); // Largest to smallest.
        }
    }
}
