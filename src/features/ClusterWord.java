package features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import main.ExampleGetter;
import twitter4j.Status;
import util.SysUtil;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EditDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;
import datacollection.Database;
import datacollection.UserInfo;

/**
 * FileName: ClusterWord.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date May 21, 2015 5:52:30 PM
 */
public class ClusterWord {
    private static final String WORD_SEPARATER = ",";

    String[] wordList = null;
    private List<HashMap<String, Integer>> wordsCounterOfTweetList = null;
    private HashMap<String, Double> probOfWord = null;

    private void test (List<Status> tweets) throws Exception {
        long time = SysUtil.getCpuTime();
        initializeWords(tweets);

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

        HashMap<String, Double> distanceTable = getDistanceTable();
        MyWordDistance disFun = new MyWordDistance(distanceTable);
        // train clusterer
        HierarchicalClusterer clusterer = new HierarchicalClusterer();
        clusterer.setOptions(new String[] { "-L", "AVERAGE" });
        clusterer.setNumClusters(10);
        clusterer.setDistanceFunction(disFun);
        clusterer.buildClusterer(data);

        // evaluate clusterer
        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(clusterer);
        eval.evaluateClusterer(data);

        // print results
        System.out.println(eval.clusterResultsToString());
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            // System.out.println(inst.toString() + " is cluster "
            // + Arrays.toString(clusterer.distributionForInstance(inst)));
        }
        System.out.println("time used: " + (SysUtil.getCpuTime() - time));
    }

    private void initializeWords (List<Status> tweets) {
        HashSet<String> allWords = new HashSet<String>();
        wordsCounterOfTweetList = new ArrayList<HashMap<String, Integer>>();
        for (Status t : tweets) {
            HashMap<String, Integer> wordsCounter =
                    new HashMap<String, Integer>();
            for (String str : WordFeature.splitIntoWords(t, true, true)) {
                allWords.add(str);
                if (wordsCounter.containsKey(str)) {
                    wordsCounter.put(str, wordsCounter.get(str) + 1);
                } else {
                    wordsCounter.put(str, 1);
                }
            }
            wordsCounterOfTweetList.add(wordsCounter);
        }

        wordList = allWords.toArray(new String[0]);
        Arrays.sort(wordList); // Sort alphabetically.
    }

    private HashMap<String, Double> getDistanceTable () {
        probOfWord = new HashMap<String, Double>();
        HashMap<String, Double> miOfTwoWords = new HashMap<String, Double>();
        int linkcount = 0;
        HashSet<String> wordUsed = new HashSet<String>();
        for (int i = 0; i < wordList.length; i++) {
            String w1 = wordList[i];
            double pw1 = probOfWord(w1);
            for (int j = i; j < wordList.length; j++) {
                String w2 = wordList[j];
                double pw2 = probOfWord(w2);
                double pw1w2 = probOfTwoWords(w1, w2);
                if (pw1w2 >= 0.5 / (wordsCounterOfTweetList.size() + 1)) {
                    linkcount++;
                    wordUsed.add(w1);
                    wordUsed.add(w2);
                }
                double mi = pw1w2 / (pw1 * pw2);
                miOfTwoWords.put(getTwoWordsKey(w1, w2), mi);
            }
        }
        HashMap<String, Double> distanceTableOfTwoWords =
                new HashMap<String, Double>();
        for (Entry<String, Double> entry : miOfTwoWords.entrySet()) {
            distanceTableOfTwoWords.put(entry.getKey(), (1 / entry.getValue()));
        }
        int to = wordList.length;
        int total = (to * to - to) / 2 + to;
        System.out.printf(
                "%d word-pairs are valid, among total of %d (%.2f%%)%n",
                linkcount, total, ((double) linkcount * 100.0) / total);
        System.out
                .printf("%d words have at least one link to others, among total of %d (%.2f%%)%n",
                        wordUsed.size(), wordList.length,
                        ((double) wordUsed.size() * 100.0) / wordList.length);
        return distanceTableOfTwoWords;
    }

    public static String getTwoWordsKey (String w1, String w2) {
        if (w1.compareTo(w2) <= 0) {
            return w1 + WORD_SEPARATER + w2;
        } else {
            return w2 + WORD_SEPARATER + w1;
        }
    }

    private double probOfTwoWords (String w1, String w2) {
        double total = (double) wordsCounterOfTweetList.size();
        double df = 0;
        for (HashMap<String, Integer> wordsCounter : wordsCounterOfTweetList) {
            if (w1.equals(w2)) {
                // That's the same word, so the df should be the times that it
                // appears twice in a tweet.
                if (wordsCounter.containsKey(w1) && wordsCounter.get(w1) >= 2) {
                    df++;
                }
            } else {
                // They are different words, so it's the times that they appear
                // in the same tweet.
                if (wordsCounter.containsKey(w1)
                        && wordsCounter.containsKey(w2)) {
                    df++;
                }
            }
        }
        double prob = (df + (1 / total)) / (total + 1); // m-estimate.
        return prob;
    }

    private double probOfWord (String w) {
        if (probOfWord.containsKey(w)) {
            return probOfWord.get(w);
        }
        double total = (double) wordsCounterOfTweetList.size();
        double df = 0;
        for (HashMap<String, Integer> wordsCounter : wordsCounterOfTweetList) {
            if (wordsCounter.containsKey(w)) {
                df++;
            }
        }
        double prob = (df + (1 / total)) / (total + 1); // m-estimate.
        probOfWord.put(w, prob);
        return prob;
    }

    private static class MyWordDistance extends EditDistance {
        private final HashMap<String, Double> distanceTableOfTwoWords;

        public MyWordDistance(HashMap<String, Double> distanceTableOfTwoWords) {
            this.distanceTableOfTwoWords = distanceTableOfTwoWords;
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
                    diff = distanceTableOfTwoWords.get(getTwoWordsKey(w1, w2));
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
        ClusterWord a = new ClusterWord();
        for (long id : UserInfo.KEY_AUTHORS) {
            if (id != 28657802L) {
                // continue;
            }
            final List<Status> tweets =
                    a.getAuthorTweets(id, ExampleGetter.TRAIN_START_DATE,
                            ExampleGetter.TEST_END_DATE);
            System.out.println(UserInfo.KA_ID2SCREENNAME.get(id));
            a.test(tweets);

            System.out.println("****");
        }
    }
}
