package features;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import jminhep.cluster.DataHolder;
import jminhep.cluster.DataPoint;
import jminhep.cluster.Partition;
import main.ExampleGetter;
import twitter4j.Status;
import util.SysUtil;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.core.tokenizers.AlphabeticTokenizer;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import datacollection.Database;
import datacollection.UserInfo;
import features.FeatureExtractor.FTweetCluster;

/**
 * FileName: ClusterFeature.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date May 11, 2015 9:03:57 PM
 */
public class ClusterFeature {
    private static final int CLUSTERS_COUNT = 10;
    private StringToWordVector filter = null;

    public void
            setFeature (FeatureExtractor featureGetters, List<Status> tweets) {
        try {
            Instances dataFiltered = tweets2Vectors(tweets);
            // train clusterer
            EM clusterer = new EM();
            clusterer.setNumClusters(CLUSTERS_COUNT);
            // set further options for EM, if necessary...
            clusterer.buildClusterer(dataFiltered);
            for (int cid = 0; cid < CLUSTERS_COUNT; cid++) {
                featureGetters.getterListOfPreNum.add(new FTweetCluster(cid,
                        clusterer, filter));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instances tweets2Vectors (List<Status> tweets) throws Exception {
        Attribute strAttr = new Attribute("tweet content", (FastVector) null);
        FastVector attributes = new FastVector();
        attributes.addElement(strAttr);
        Instances data =
                new Instances("Test-dataset", attributes, 0);
        HashSet<String> textExisting = new HashSet<String>();
        for (Status t : tweets) {
            String s = WordFeature.getTextOfTweet(t);
            if (!textExisting.contains(s)) {
                textExisting.add(s);
                double[] values = new double[data.numAttributes()];
                values[0] = data.attribute(0).addStringValue(s);
                Instance inst = new Instance(1.0, values);
                data.add(inst);
            }
        }

        filter = new StringToWordVector(2000);
        filter.setOutputWordCounts(true);
        filter.setLowerCaseTokens(true);
        filter.setUseStoplist(true);
        filter.setMinTermFreq(2);
        filter.setStemmer(new IteratedLovinsStemmer());
        filter.setTokenizer(new AlphabeticTokenizer());
        filter.setInputFormat(data);
        Instances dataFiltered = Filter.useFilter(data, filter);
        return dataFiltered;
    }

    // For test
    void testEM (List<Status> tweets) throws Exception {
        Instances dataFiltered = tweets2Vectors(tweets);
        long time = SysUtil.getCpuTime();
        // train clusterer
        EM clusterer = new EM();
        clusterer.setNumClusters(10);
        // set further options for EM, if necessary...
        clusterer.buildClusterer(dataFiltered);

        // evaluate clusterer
        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(clusterer);
        eval.evaluateClusterer(dataFiltered);

        // print results
        System.out.println(eval.clusterResultsToString());
        for (int i = 0; i < dataFiltered.numInstances(); i++) {
            Instance inst = dataFiltered.instance(i);
            System.out.println(inst.toString() + " is cluster "
                    + Arrays.toString(clusterer.distributionForInstance(inst)));
        }
        System.out.println("time used: " + (SysUtil.getCpuTime() - time));
    }

    void testFuzzy (List<Status> tweets) throws Exception {
        Instances dataFiltered = tweets2Vectors(tweets);
        long time = SysUtil.getCpuTime();
        DataHolder data2 = new DataHolder("Example");
        for (int i = 0; i < dataFiltered.numInstances(); i++) {
            Instance inst = dataFiltered.instance(i);
            double[] a = inst.toDoubleArray();
            data2.add(new DataPoint(a));
        }

        Partition pat = new Partition(data2);

        // set No clusters, precision, fuzziness (dummy if non-applicable),
        // max number of iterations
        pat.set(3, 0.001, 1.7, 1000);

        // probability for membership (only for Fuzzy algorithm)
        pat.setProbab(0.68);

        pat.run(132);
        System.out.println("\nalgorithm: " + pat.getName());
        System.out.println("Compactness: " + pat.getCompactness());
        System.out.println("No of final clusters: " + pat.getNclusters());
        DataHolder Centers = pat.getCenters();
        System.out.println("Positions of centers: ");
        Centers.print();

        // show cluster association
        for (int m = 0; m < data2.getSize(); m++) {
            DataPoint dp = data2.getRaw(m);
            int k = dp.getClusterNumber();
            System.out.println("point=" + m + " associated with=" + k);
        }

        System.out.println("time used: " + (SysUtil.getCpuTime() - time));
    }

    // For test.
    final Database db = Database.getInstance();

    private List<Status> getAuthorTweets (long authorId, Date fromDate,
            Date toDate) {
        final List<Status> auTweets =
                db.getOriginalTweetListInTimeRange(authorId, fromDate, toDate);
        Iterator<Status> iter = auTweets.iterator();
        while (iter.hasNext()) {
            Status t = iter.next();
            if (t.getRetweetCount() == 0) {
                iter.remove();
            }
        }
        Collections.sort(auTweets, ExampleGetter.TWEET_SORTER);
        return auTweets;
    }

    public static void main (String[] args) throws Exception {
        ClusterFeature a = new ClusterFeature();
        for (long id : UserInfo.KEY_AUTHORS) {
            if (id != 16958346L) {
                // continue;
            }
            final List<Status> tweets =
                    a.getAuthorTweets(id, ExampleGetter.TRAIN_START_DATE,
                            ExampleGetter.TEST_START_DATE);
            System.out.println(UserInfo.KA_ID2SCREENNAME.get(id));
            a.testEM(tweets);

            System.out.println("****");
        }
    }
}
