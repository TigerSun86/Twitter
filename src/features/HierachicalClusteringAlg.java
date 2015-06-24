package features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import util.Dbg;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EditDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;
import features.ClusterWord.ClAlg;

/**
 * FileName: HierachicalClusteringAlg.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 24, 2015 12:49:32 AM
 */
public class HierachicalClusteringAlg implements ClAlg {
    int numOfCl = 10;

    public HierachicalClusteringAlg(int numOfCl) {
        this.numOfCl = numOfCl;
    }

    @Override
    public List<Set<String>> cluster (SimTable simTable, List<String> wordList) {
        SimTable disTable = simTable;
        disTable.inverseValues();

        // System.out.println("Distance table done.");
        MyWordDistance disFun = new MyWordDistance(disTable);
        List<Set<String>> clusters = null;
        try {
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
            // train clusterer
            HierarchicalClusterer clusterer = new HierarchicalClusterer();
            clusterer.setOptions(new String[] { "-L", "AVERAGE" });

            clusterer.setNumClusters(this.numOfCl);
            clusterer.setDistanceFunction(disFun);
            clusterer.buildClusterer(data);

            // Clustering result.
            clusters = new ArrayList<Set<String>>();
            for (int i = 0; i < numOfCl; i++) {
                clusters.add(new HashSet<String>());
            }
            for (int i = 0; i < data.numInstances(); i++) {
                Instance inst = data.instance(i);
                int cl = clusterer.clusterInstance(inst);
                clusters.get(cl).add(inst.toString());
            }
            if (Dbg.dbg) {
                eval(clusters, clusterer, wordList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clusters;
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
    private void eval (List<Set<String>> clusters,
            HierarchicalClusterer clusterer, List<String> wordList)
            throws Exception {
        System.out.println("*****");
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

        System.out.println("*****");
        System.out.println("Final clustering result:");
        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster " + i + ":");
            System.out.println(clusters.get(i).size() + " "
                    + clusters.get(i).toString());
        }
    }
}
