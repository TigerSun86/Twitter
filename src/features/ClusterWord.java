package features;

import java.util.ArrayList;
import java.util.BitSet;
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
import weka.core.Stopwords;
import weka.core.neighboursearch.PerformanceStats;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.tokenizers.AlphabeticTokenizer;
import weka.core.tokenizers.Tokenizer;
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
    private static final int MIN_DF = 100;
    private static final boolean NEED_STEM = false;

    private List<String> wordList = null;
    private List<HashSet<String>> wordSetOfDocs = null;
    private HashMap<String, BitSet> word2DocIds = null;
    private HashMap<String, Double> probOfWord = null;

    private void clusterWords (List<List<String>> docs) throws Exception {
        long time = SysUtil.getCpuTime();
        initializeWords(docs);
        initWord2DocIds();
        filterWords();

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
        System.out.println(wordList.size() + " instances to clustered.");
        HashMap<String, Double> distanceTable = getDistanceTable();
        System.out.println("Distance table done.");
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
        List<List<String>> result = new ArrayList<List<String>>();
        for (int cl = 0; cl < 10; cl++) {
            result.add(new ArrayList<String>());
        }
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            int cl = clusterer.clusterInstance(inst);
            result.get(cl).add(inst.toString());
        }
        for (int cl = 0; cl < 10; cl++) {
            System.out.println("****");
            System.out.println("Cluster " + cl);
            System.out.println(result.get(cl).toString());
        }
        System.out.println("time used: " + (SysUtil.getCpuTime() - time));
    }

    private void test (List<Status> tweets) throws Exception {
        List<List<String>> docs = new ArrayList<List<String>>();
        for (Status t : tweets) {
            List<String> doc = WordFeature.splitIntoWords(t, true, NEED_STEM);
            docs.add(doc);
        }
        clusterWords(docs);
    }

    private void test2 () throws Exception {
        List<String> pages = DomainGetter.getWebPages();
        Tokenizer tokenizer = new AlphabeticTokenizer();
        Stemmer stemmer = new IteratedLovinsStemmer();
        List<List<String>> docs = new ArrayList<List<String>>();
        for (String page : pages) {
            List<String> doc = new ArrayList<String>();
            tokenizer.tokenize(page);
            // Iterate through tokens, perform stemming, and remove stopwords
            while (tokenizer.hasMoreElements()) {
                String word = ((String) tokenizer.nextElement()).intern();
                word = word.toLowerCase();
                if (Stopwords.isStopword(word)) {
                    continue;// Check stop word before and after stemmed.
                }
                if (NEED_STEM) {
                    word = stemmer.stem(word);
                }
                doc.add(word);
            }
            docs.add(doc);
        }
        clusterWords(docs);
    }

    private void initializeWords (List<List<String>> docs) {
        wordSetOfDocs = new ArrayList<HashSet<String>>();
        HashSet<String> wholeWordSet = new HashSet<String>();
        for (List<String> doc : docs) {
            HashSet<String> wordsOfDoc = new HashSet<String>();
            for (String str : doc) {
                wordsOfDoc.add(str);
                wholeWordSet.add(str);
            }
            wordSetOfDocs.add(wordsOfDoc);
        }
        wordList = new ArrayList<String>();
        wordList.addAll(wholeWordSet);
        Collections.sort(wordList); // Sort alphabetically.
    }

    private void initWord2DocIds () {
        word2DocIds = new HashMap<String, BitSet>();
        for (String word : wordList) {
            word2DocIds.put(word, new BitSet(wordSetOfDocs.size()));
        }
        for (int id = 0; id < wordSetOfDocs.size(); id++) {
            HashSet<String> doc = wordSetOfDocs.get(id);
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
            if (df < MIN_DF) {
                word2DocIds.remove(word);
            }
        }
        wordList = new ArrayList<String>();
        wordList.addAll(word2DocIds.keySet());
        Collections.sort(wordList);
    }

    private HashMap<String, Double> getDistanceTable () {
        probOfWord = new HashMap<String, Double>();
        HashMap<String, Double> miOfTwoWords = new HashMap<String, Double>();
        int linkcount = 0;
        HashSet<String> wordUsed = new HashSet<String>();
        for (int i = 0; i < wordList.size(); i++) {
            String w1 = wordList.get(i);
            double pw1 = probOfWord(w1);
            for (int j = i; j < wordList.size(); j++) {
                String w2 = wordList.get(j);
                if (!w1.equals(w2)) {
                    // The mutual information of a word itself should be
                    // infinite, so the distance should be 0. It will be handled
                    // in class MyWordDistance.
                    double pw2 = probOfWord(w2);
                    double pw1w2 = probOfTwoWords(w1, w2);
                    double mi = pw1w2 / (pw1 * pw2);

                    if (mi > 1.000001) {
                        // If mi <= 1, (logMi <=0), the distance should be
                        // infinite. Just don't add it so class MyWordDistance
                        // will handle it.
                        double logMi = Math.log(mi);
                        miOfTwoWords.put(getTwoWordsKey(w1, w2), logMi);
                    }

                    // Debug info.
                    if (pw1w2 > 0) {
                        linkcount++;
                        wordUsed.add(w1);
                        wordUsed.add(w2);
                    }
                }
            }
        }

        HashMap<String, Double> distanceTableOfTwoWords =
                new HashMap<String, Double>();
        for (Entry<String, Double> entry : miOfTwoWords.entrySet()) {
            distanceTableOfTwoWords.put(entry.getKey(), (1 / entry.getValue()));
        }
        // Debug info.
        int to = wordList.size();
        int total = (to * to - to) / 2;
        System.out.printf(
                "%d word-pairs are valid, among total of %d (%.2f%%)%n",
                linkcount, total, ((double) linkcount * 100.0) / total);
        System.out
                .printf("%d words have at least one link to others, among total of %d (%.2f%%)%n",
                        wordUsed.size(), wordList.size(),
                        ((double) wordUsed.size() * 100.0) / wordList.size());
        return distanceTableOfTwoWords;
    }

    public static String getTwoWordsKey (String w1, String w2) {
        if (w1.compareTo(w2) <= 0) {
            return w1 + WORD_SEPARATER + w2;
        } else {
            return w2 + WORD_SEPARATER + w1;
        }
    }

    private final double probOfTwoWords (String w1, String w2) {
        double total = (double) wordSetOfDocs.size();
        BitSet interval = (BitSet) word2DocIds.get(w1).clone();
        interval.and(word2DocIds.get(w2));
        double df = interval.cardinality();
        double prob = df / total;
        return prob;
    }

    private final double probOfWord (String w) {
        if (probOfWord.containsKey(w)) {
            return probOfWord.get(w);
        }
        double total = (double) wordSetOfDocs.size();
        double df = (double) word2DocIds.get(w).cardinality();
        double prob = df / total;
        probOfWord.put(w, prob);
        return prob;
    }

    private static class MyWordDistance extends EditDistance {
        private static final long serialVersionUID = 1L;
        private final HashMap<String, Double> distanceTableOfTwoWords;

        public MyWordDistance(HashMap<String, Double> distanceTableOfTwoWords) {
            this.distanceTableOfTwoWords = distanceTableOfTwoWords;
        }

        private double distanceOfTwoWords (String w1, String w2) {
            if (w1.equals(w2)) {
                return 0; // Same word, distance to itself is 0.
            }
            String key = getTwoWordsKey(w1, w2);
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
        new ClusterWord().test2();
        System.exit(0);
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
