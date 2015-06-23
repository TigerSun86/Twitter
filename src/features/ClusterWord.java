package features;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
        public Mode mode = SimCalculator.Mode.AEMI;
        public boolean mEstimate = true;
        public boolean reattach = true;
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
        assert !wordSetOfDocs.isEmpty();
        long time = SysUtil.getCpuTime();

        init();

        SimCalculator simCal =
                new SimCalculator(this.para.mode, this.para.mEstimate, true,
                        false, wordList, wordSetOfDocs, word2DocIds, null);
        SimTable simTable = simCal.getSimTable();
        SingleCutAlg clAlg = new SingleCutAlg();
        clAlg.numOfCl = para.numOfCl;
        clAlg.reattach = para.reattach;
        clAlg.cluster(simTable, wordList);

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
                        false, wordList, wordSetOfDocs, word2DocIds, null);
        SimTable disTable = simCal.getSimTable();
        disTable.inverseValues();

        // System.out.println("Distance table done.");
        MyWordDistance disFun = new MyWordDistance(disTable);
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

    private static class MyWordDistance extends EditDistance {
        private static final long serialVersionUID = 1L;
        private final SimTable disTable;

        public MyWordDistance(SimTable disTable) {
            this.disTable = disTable;
        }

        private double distanceOfTwoWords (String w1, String w2) {
            if (w1.equals(w2)) {
                return 0.0;
            }
            if (disTable.contains(w1, w2)) {
                return disTable.getValue(w1, w2);
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
