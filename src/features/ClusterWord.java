package features;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import util.SysUtil;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EditDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;
import features.SimCalculator.Mode;
import features.SimCalculator.Pair;

/**
 * FileName: ClusterWord.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date May 21, 2015 5:52:30 PM
 */
public class ClusterWord {
    public static final int MIN_DF = 0;
    public static final int NUM_OF_CL = 10;

    public static class ClusterWordSetting {
        public int minDf = MIN_DF;
        public int numOfCl = NUM_OF_CL;
        public boolean needStem = FeatureExtractor.NEED_STEM;
        public Mode mode = SimCalculator.Mode.JACCARD;
        public boolean mEstimate = true;
    }

    public ClusterWordSetting para = new ClusterWordSetting();

    private boolean debug = true;

    private List<String> wordList = null;
    private List<Set<String>> wordSetOfDocs = null;
    private HashMap<String, BitSet> word2DocIds = null;
    private Clusterer clusterer = null; // For debug.

    public ClusterWord(List<Set<String>> pages) {
        this.wordSetOfDocs = pages;
    }

    public void setWordList (List<String> wordList) {
        // Actually the content of the inputed wordList won't be changed
        // because method filterWords will renew wordList.
        this.wordList = wordList;
    }

    public HashMap<String, Integer> clusterWords () {
        long time = SysUtil.getCpuTime();

        init();

        SimCalculator simCal =
                new SimCalculator(this.para.mode, this.para.mEstimate, true,
                        wordList, wordSetOfDocs, word2DocIds, null);
        HashMap<String, Double> similarityTable = simCal.getSimilarityTable();
        SingleCutAlg clAlg = new SingleCutAlg();
        clAlg.debug = this.debug;
        clAlg.cluster(similarityTable, wordList);

        HashMap<String, Integer> word2Cl = new HashMap<String, Integer>();
        for (int cid = 0; cid < clAlg.clusters.size(); cid++) {
            Set<String> cl = clAlg.clusters.get(cid);
            for (String w : cl) {
                word2Cl.put(w, cid);
            }
        }
        // for (String w : clAlg.singletons) {
        // word2Cl.put(w, clAlg.clusters.size());
        // }
        // Important! don't forget tell upper level how many clusters have.
        para.numOfCl = clAlg.clusters.size();
        if (debug) {
            System.out.println("Time used: " + (SysUtil.getCpuTime() - time));
        }
        return word2Cl;
    }

    private static class SingleCutAlg {
        boolean debug = true;
        int maxInitClusterSize = 20;
        int minInitClusterSize = 5;
        List<Set<String>> clusters;

        private static class Node {
            String n;
            Set<String> nei;

            public Node(String n) {
                this.n = n;
                this.nei = new HashSet<String>();
            }

        }

        public void cluster (HashMap<String, Double> simTable,
                List<String> wordList) {
            clusters = new ArrayList<Set<String>>();

            // Get initial singletons.
            Set<String> single =
                    SimClusterAlg.getSingleton(simTable, new HashSet<String>(),
                            wordList);
            if (debug) {
                System.out.println("Initial singletons: " + single.toString());
            }

            if (debug) {
                System.out.println("*****");
                System.out.println("Cutting stage.");
                System.out.printf(
                        "maxInitClusterSize is %d, minInitClusterSize is %d%n",
                        maxInitClusterSize, minInitClusterSize);
            }
            HashMap<String, Node> curGraph = getTheGraph(simTable);
            // Sort all edges.
            List<Pair> sortedEdges =
                    SimCalculator.Pair.getAscendingPairs(simTable);

            // For re-attaching, should in descending order.
            LinkedList<Pair> visitedEdges = new LinkedList<Pair>();

            // Loop through all edges lowest to highest,
            for (Pair pair : sortedEdges) {
                if (curGraph.isEmpty()) {
                    break;
                }
                String[] ns = pair.e.split(SimCalculator.WORD_SEPARATER);
                String n1 = ns[0];
                String n2 = ns[1];
                // if the edge hasn't been removed.
                if (curGraph.containsKey(n1)
                        && curGraph.get(n1).nei.contains(n2)) {
                    assert curGraph.containsKey(n2)
                            && curGraph.get(n2).nei.contains(n1);
                    // Cut one edge.
                    curGraph.get(n1).nei.remove(n2);
                    curGraph.get(n2).nei.remove(n1);
                    visitedEdges.addFirst(pair); // Will be descending.
                    if (debug) {
                        System.out.println("Cut edge: " + pair.toString());
                    }
                    // Check isolated subgraph.
                    List<Set<String>> subGraphs =
                            getIsolatedSubgraphs(curGraph, n1, n2);
                    // For all (at most 2) new small enough subgraph.
                    for (Set<String> subG : subGraphs) {
                        if (subG.size() <= maxInitClusterSize) {
                            // Add subgraph into cluster list.
                            clusters.add(subG);
                            // Remove all edges of subgraph from the whole
                            // graph.
                            for (String nodeToRemove : subG) {
                                removeNode(curGraph, nodeToRemove);
                            }
                            if (debug) {
                                System.out.println("New cluster: "
                                        + subG.toString());
                            }
                        }
                    }
                }
            }
            assert curGraph.isEmpty();
            if (debug) {
                System.out.println("*****");
                System.out.println("Clustering result:");
                for (int i = 0; i < clusters.size(); i++) {
                    System.out.println("Cluster " + i + ":");
                    System.out.println(clusters.get(i).toString());
                }
                System.out.println("Singleton: " + single.toString());
            }

            if (debug) {
                System.out.println("*****");
                System.out.println("Re-attaching stage.");
                System.out.printf(
                        "maxInitClusterSize is %d, minInitClusterSize is %d%n",
                        maxInitClusterSize, minInitClusterSize);
            }
            // Selected node -> the sub graph it belongs to.
            HashMap<String, Set<String>> selectedNodes =
                    new HashMap<String, Set<String>>();
            // Unselected node -> the sub graph it belongs to.
            HashMap<String, Set<String>> frontierNodes =
                    new HashMap<String, Set<String>>();
            List<Set<String>> selectedClusters = new ArrayList<Set<String>>();

            for (Set<String> subG : clusters) {
                if (subG.size() >= minInitClusterSize) {
                    selectedClusters.add(subG);
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
            if (debug) {
                System.out.println("*****");

                System.out.println("Selected seed clusters:");
                for (int i = 0; i < clusters.size(); i++) {
                    System.out.println("Cluster " + i + ":");
                    System.out.println(clusters.get(i).toString());
                }
            }

            for (Pair pair : visitedEdges) {
                if (frontierNodes.isEmpty()) {
                    break;
                }
                if (debug) {
                    System.out.println("Trying to attach edge: "
                            + pair.toString());
                }
                String[] ns = pair.e.split(SimCalculator.WORD_SEPARATER);
                String n1 = ns[0];
                String n2 = ns[1];
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
                    if (debug) {
                        System.out.printf(
                                "Sub graph of node %s has joined by %s%n",
                                nodeIn, subGOut.toString());
                    }
                }
            }
            assert frontierNodes.isEmpty();

            if (debug) {
                System.out.println("*****");
                System.out.println("Clustering result:");
                for (int i = 0; i < clusters.size(); i++) {
                    System.out.println("Cluster " + i + ":");
                    System.out.println(clusters.get(i).toString());
                }
                System.out.println("Singleton: " + single.toString());
            }
        }

        private static HashMap<String, Node> getTheGraph (
                HashMap<String, Double> simTable) {
            HashMap<String, Node> graph = new HashMap<String, Node>();
            for (Entry<String, Double> entry : simTable.entrySet()) {
                String[] ns =
                        entry.getKey().split(SimCalculator.WORD_SEPARATER);
                String n1 = ns[0];
                String n2 = ns[1];
                assert !n1.equals(n2);
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

        private static List<Set<String>> getIsolatedSubgraphs (
                HashMap<String, Node> graph, String n1, String n2) {
            List<Set<String>> subGs = new ArrayList<Set<String>>();

            Set<String> subG1 = getMaximumConnectedGraph(graph, n1, n2);
            if (subG1 != null) { // If == null return empty subGs.
                Set<String> subG2 = getMaximumConnectedGraph(graph, n2, n1);
                assert subG2 != null;
                subGs.add(subG1);
                subGs.add(subG2);
            }
            return subGs;
        }

        private static Set<String> getMaximumConnectedGraph (
                HashMap<String, Node> graph, String initNode, String stopNode) {
            Set<String> outQue = new HashSet<String>();
            Set<String> inQue = new HashSet<String>();
            LinkedList<String> que = new LinkedList<String>();
            inQue.add(initNode);
            que.add(initNode);

            boolean breakByStopNode = false;
            while (!que.isEmpty() && !breakByStopNode) {
                String curN = que.removeFirst();
                if (curN.equals(stopNode)) {
                    breakByStopNode = true;
                } else {
                    inQue.remove(curN);
                    outQue.add(curN);
                    for (String nei : graph.get(curN).nei) {
                        if (!outQue.contains(nei) && !inQue.contains(nei)) {
                            // Haven't visited and haven't planed to visit.
                            inQue.add(nei);
                            que.add(nei);
                        }
                    }
                }
            }
            if (breakByStopNode) {
                return null;
            } else {
                return outQue;
            }
        }
    }

    private static class SimClusterAlg {
        int maxClusterSize = 0; // Initialize dynamically when runing.
        int maxNumOfSteps = 20;
        List<Set<String>> clusters; // Output
        Set<String> singletons; // Output

        private double low;
        private double high;

        boolean debug = true;

        public void cluster (HashMap<String, Double> simTable,
                List<String> wordList) {
            int totalWordCount = wordList.size();

            if (maxClusterSize <= 1) {
                maxClusterSize = Math.max(3, totalWordCount / 5);
            }
            if (maxNumOfSteps <= 2) {
                maxNumOfSteps = 3;
            }
            singletons = new HashSet<String>();
            clusters = new ArrayList<Set<String>>();
            getBound(simTable);

            Set<String> usedWords = new HashSet<String>();
            double step = (high - low) / (maxNumOfSteps - 2);
            double threshold = low;

            if (debug) {
                System.out.printf("About to cluster %d words with %d edges.%n",
                        totalWordCount, simTable.size());
                System.out
                        .printf("Lowest similarity = %.3f, "
                                + "highest similarity = %.3f, maxInitClusterSize = %d, "
                                + "maxNumOfSteps = %d, step size = %.3f.%n",
                                low, high, maxClusterSize, maxNumOfSteps, step);
            }
            for (int i = 0; i < maxNumOfSteps; i++, threshold += step) {
                System.out.println("Threshold: " + threshold);
                HashMap<String, Double> curSimTable =
                        filterSimTable(simTable, usedWords, threshold);
                Set<String> single =
                        getSingleton(curSimTable, usedWords, wordList);
                if (debug) {
                    System.out.println("Singletons: " + single.toString());
                }
                singletons.addAll(single);
                usedWords.addAll(single);
                List<Set<String>> cls = getClusters(curSimTable);
                for (Set<String> cl : cls) {
                    if (cl.size() <= maxClusterSize) {
                        if (debug) {
                            System.out.println("Cluster: " + cl.toString());
                        }
                        clusters.add(cl);
                        usedWords.addAll(cl);
                    }
                }
                if (debug) {
                    System.out.println("Clustered words: " + usedWords.size()
                            + "/" + totalWordCount);
                }
                if (usedWords.size() >= totalWordCount) {
                    break;
                }
            }
            if (debug) {
                System.out.println("*****");
                for (int i = 0; i < clusters.size(); i++) {
                    System.out.println("Cluster " + i + ":");
                    System.out.println(clusters.get(i).toString());
                }
                System.out.println("Singleton: " + singletons.toString());
            }
        }

        private void getBound (HashMap<String, Double> simTable) {
            Iterator<Entry<String, Double>> iter =
                    simTable.entrySet().iterator();
            low = iter.next().getValue();
            high = low;
            while (iter.hasNext()) {
                double v = iter.next().getValue();
                if (v > high) {
                    high = v;
                } else if (v < low) {
                    low = v;
                }
            }
            assert high > low;
        }

        private List<Set<String>> getClusters (HashMap<String, Double> st) {
            List<Set<String>> cls = new ArrayList<Set<String>>();
            while (!st.isEmpty()) {
                Set<String> cl = oneMaximumConnectedCluster(st);
                cls.add(cl);
            }
            return cls;
        }

        private HashMap<String, Double> filterSimTable (
                HashMap<String, Double> simTable, Set<String> used, double thr) {
            HashMap<String, Double> filteredSimTable =
                    new HashMap<String, Double>();
            for (Entry<String, Double> entry : simTable.entrySet()) {
                double sim = entry.getValue();
                if (sim >= thr) {
                    // Keep only when they have strong similarity higher than
                    // threshold.
                    String[] s =
                            entry.getKey().split(SimCalculator.WORD_SEPARATER);
                    if (!used.contains(s[0])) {
                        assert !used.contains(s[1]);
                        filteredSimTable.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            return filteredSimTable;
        }

        public static Set<String> getSingleton (
                HashMap<String, Double> simTable, Set<String> used,
                List<String> wordList) {
            Set<String> wordsOfTable = new HashSet<String>();
            for (Entry<String, Double> entry : simTable.entrySet()) {
                String[] s = entry.getKey().split(SimCalculator.WORD_SEPARATER);
                wordsOfTable.add(s[0]);
                wordsOfTable.add(s[1]);
            }
            Set<String> single = new HashSet<String>();
            for (String s : wordList) {
                if (!wordsOfTable.contains(s) && !used.contains(s)) {
                    single.add(s);
                }
            }
            return single;
        }

        private Set<String> oneMaximumConnectedCluster (
                HashMap<String, Double> simTable) {
            Set<String> cl = new HashSet<String>();
            Iterator<Entry<String, Double>> iter =
                    simTable.entrySet().iterator();
            String[] str =
                    iter.next().getKey().split(SimCalculator.WORD_SEPARATER);
            cl.add(str[0]);
            cl.add(str[1]);
            iter.remove();
            int oldSize = cl.size();
            int newSize = cl.size();
            do {
                oldSize = newSize;
                iter = simTable.entrySet().iterator();
                while (iter.hasNext()) {
                    Entry<String, Double> entry = iter.next();
                    str = entry.getKey().split(SimCalculator.WORD_SEPARATER);
                    if ((cl.contains(str[0]) || cl.contains(str[1]))) {
                        cl.add(str[0]);
                        cl.add(str[1]);
                        iter.remove();
                    }
                }
                newSize = cl.size();
            } while (newSize > oldSize);

            return cl;
        }
    }

    public HashMap<String, Integer> clusterWordsByHierachicalClusteringAlg () {
        long time = SysUtil.getCpuTime();

        init();
        SimCalculator simCal =
                new SimCalculator(this.para.mode, this.para.mEstimate, true,
                        wordList, wordSetOfDocs, word2DocIds, null);
        HashMap<String, Double> similarityTable = simCal.getSimilarityTable();
        HashMap<String, Double> distanceTable =
                getDistanceTable(similarityTable);

        // System.out.println("Distance table done.");
        MyWordDistance disFun = new MyWordDistance(distanceTable);
        HashMap<String, Integer> word2Cl = null;
        try {
            Attribute strAttr = new Attribute("Word", (FastVector) null);
            FastVector attributes = new FastVector();
            attributes.addElement(strAttr);
            Instances data = new Instances("Test-dataset", attributes, 0);
            for (String word : this.wordList) {
                double[] values = new double[data.numAttributes()];
                values[0] = data.attribute(0).addStringValue(word);
                Instance inst = new Instance(1.0, values);
                data.add(inst);
            }
            // train clusterer
            HierarchicalClusterer clusterer = new HierarchicalClusterer();
            clusterer.setOptions(new String[] { "-L", "AVERAGE" });

            clusterer.setNumClusters(this.para.numOfCl);
            clusterer.setDistanceFunction(disFun);
            clusterer.buildClusterer(data);

            this.clusterer = clusterer; // For test.
            // Clustering result.
            word2Cl = new HashMap<String, Integer>();
            for (int i = 0; i < data.numInstances(); i++) {
                Instance inst = data.instance(i);
                int cl = clusterer.clusterInstance(inst);
                word2Cl.put(inst.toString(), cl);
            }
            if (debug) {
                eval();
                System.out.println("Time used: "
                        + (SysUtil.getCpuTime() - time));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return word2Cl;
    }

    private void init () {
        initializeWords();
        initWord2DocIds();
        filterWords();
    }

    private void initializeWords () {
        HashSet<String> wholeWordSet = new HashSet<String>();
        for (Set<String> page : wordSetOfDocs) {
            wholeWordSet.addAll(page);
        }
        if (wordList == null) {
            // wordList is not assigned, so use the word list of docs.
            wordList = new ArrayList<String>();
            wordList.addAll(wholeWordSet);
        }

        Collections.sort(wordList); // Sort alphabetically.
    }

    private void initWord2DocIds () {
        word2DocIds = new HashMap<String, BitSet>();
        for (String word : wordList) {
            word2DocIds.put(word, new BitSet(wordSetOfDocs.size()));
        }
        for (int id = 0; id < wordSetOfDocs.size(); id++) {
            Set<String> doc = wordSetOfDocs.get(id);
            for (String word : doc) {
                // There could be some words of doc do not existing in the
                // wordList.
                if (word2DocIds.containsKey(word)) {
                    word2DocIds.get(word).set(id);
                }
            }
        }
    }

    private void filterWords () {
        for (String word : wordList) {
            int df = word2DocIds.get(word).cardinality();
            if (df < this.para.minDf) {
                word2DocIds.remove(word);
            }
        }
        wordList = new ArrayList<String>();
        wordList.addAll(word2DocIds.keySet());
        Collections.sort(wordList);
    }

    private HashMap<String, Double> getDistanceTable (
            HashMap<String, Double> similarityTableOfTwoWords) {
        HashMap<String, Double> distanceTableOfTwoWords =
                new HashMap<String, Double>();
        for (Entry<String, Double> entry : similarityTableOfTwoWords.entrySet()) {
            double sim = entry.getValue();
            assert sim >= 0;
            if (sim != 0.0) {
                distanceTableOfTwoWords.put(entry.getKey(), 1 / sim);
            }
        }
        return distanceTableOfTwoWords;
    }

    private static class MyWordDistance extends EditDistance {
        private static final long serialVersionUID = 1L;
        private final HashMap<String, Double> distanceTableOfTwoWords;

        public MyWordDistance(HashMap<String, Double> distanceTableOfTwoWords) {
            this.distanceTableOfTwoWords = distanceTableOfTwoWords;
        }

        private double distanceOfTwoWords (String w1, String w2) {
            if (w1.equals(w2)) {
                return 0.0;
            }
            String key = SimCalculator.getTwoWordsKey(w1, w2);
            if (distanceTableOfTwoWords.containsKey(key)) {
                return distanceTableOfTwoWords.get(key);
            } else { // No such word pair, distance is infinite.
                return Double.POSITIVE_INFINITY;
            }
        }

        @Override
        public double distance (Instance first, Instance second,
                double cutOffValue, PerformanceStats stats) {
            double sqDistance = 0;
            int numAttributes = m_Data.numAttributes();

            validate();

            double diff;

            for (int i = 0; i < numAttributes; i++) {
                diff = 0;
                if (m_ActiveIndices[i]) {
                    String w1 = first.stringValue(i);
                    String w2 = second.stringValue(i);
                    diff = distanceOfTwoWords(w1, w2);
                }
                sqDistance = updateDistance(sqDistance, diff);
                if (sqDistance > (cutOffValue * cutOffValue))
                    return Double.POSITIVE_INFINITY;
            }
            double distance = Math.sqrt(sqDistance);
            return distance;
        }
    }

    // For test.
    private void eval () throws Exception {
        System.out.println("*****");
        System.out.println(wordSetOfDocs.size() + " web pages/tweets.");
        System.out.println(wordList.size() + " words to be clustered.");
        Attribute strAttr = new Attribute("Word", (FastVector) null);
        FastVector attributes = new FastVector();
        attributes.addElement(strAttr);
        Instances data = new Instances("Test-dataset", attributes, 0);
        for (String word : wordList) {
            double[] values = new double[data.numAttributes()];
            values[0] = data.attribute(0).addStringValue(word);
            Instance inst = new Instance(1.0, values);
            data.add(inst);
        }
        // evaluate clusterer
        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(clusterer);
        eval.evaluateClusterer(data);

        // print results
        System.out.println(eval.clusterResultsToString());
        List<List<String>> result = new ArrayList<List<String>>();
        for (int cl = 0; cl < this.para.numOfCl; cl++) {
            result.add(new ArrayList<String>());
        }
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            int cl = clusterer.clusterInstance(inst);
            result.get(cl).add(inst.toString());
        }
        for (int cl = 0; cl < this.para.numOfCl; cl++) {
            System.out.println("****");
            System.out.println("Cluster " + cl);
            System.out.println(result.get(cl).toString());
        }
        System.out.println("*****");
    }
}
