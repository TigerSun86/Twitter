package features;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import main.ExampleGetter;
import twitter4j.Status;
import twitter4j.URLEntity;
import util.SysUtil;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
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
    public static final int MIN_DF = 100;
    public static final int NUM_OF_CL = 10;
    public static final boolean NEED_STEM = false;

    private static final String WORD_SEPARATER = ",";

    private int minDf = MIN_DF;
    private int numOfCl = NUM_OF_CL;
    private boolean needStem = NEED_STEM;
    private boolean debug = false;

    private List<String> wordList = null;
    private List<String> pages = null;
    private List<HashSet<String>> wordSetOfDocs = null;
    private HashMap<String, BitSet> word2DocIds = null;
    private HashMap<String, Double> probOfWord = null;
    private Clusterer clusterer = null; // For test.

    public ClusterWord(List<String> pages) {
        this.pages = pages;
    }

    public void setWordList (List<String> wordList) {
        // Actually the content of the inputed wordList won't be changed
        // because method filterWords will renew wordList.
        this.wordList = wordList;
    }

    public void setMinDf (int minDf) {
        this.minDf = minDf;
    }

    public void setNumOfCl (int numOfCl) {
        this.numOfCl = numOfCl;
    }

    public void setNeedStem (boolean needStem) {
        this.needStem = needStem;
    }

    public HashMap<String, Integer> clusterWords () {
        init();

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

        HashMap<String, Double> distanceTable = getDistanceTable();
        // System.out.println("Distance table done.");
        MyWordDistance disFun = new MyWordDistance(distanceTable);

        HashMap<String, Integer> word2Cl = null;
        try {
            // train clusterer
            HierarchicalClusterer clusterer = new HierarchicalClusterer();
            clusterer.setOptions(new String[] { "-L", "AVERAGE" });

            clusterer.setNumClusters(this.numOfCl);
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
        wordSetOfDocs = new ArrayList<HashSet<String>>();
        HashSet<String> wholeWordSet = new HashSet<String>();

        List<List<String>> docs = webPage2Doc(pages);
        for (List<String> doc : docs) {
            HashSet<String> wordsOfDoc = new HashSet<String>();
            for (String str : doc) {
                wordsOfDoc.add(str);
                wholeWordSet.add(str);
            }
            wordSetOfDocs.add(wordsOfDoc);
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
            if (df < minDf) {
                word2DocIds.remove(word);
            }
        }
        wordList = new ArrayList<String>();
        wordList.addAll(word2DocIds.keySet());
        Collections.sort(wordList);
    }

    private List<List<String>> webPage2Doc (List<String> pages) {
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
                if (needStem) {
                    word = stemmer.stem(word);
                }
                doc.add(word);
            }
            docs.add(doc);
        }
        return docs;
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
        if (debug) {
            // Debug info.
            int to = wordList.size();
            int total = (to * to - to) / 2;
            System.out.printf(
                    "%d word-pairs are valid, among total of %d (%.2f%%)%n",
                    linkcount, total, ((double) linkcount * 100.0) / total);
            System.out
                    .printf("%d words have at least one link to others, among total of %d (%.2f%%)%n",
                            wordUsed.size(),
                            wordList.size(),
                            ((double) wordUsed.size() * 100.0)
                                    / wordList.size());
        }

        return distanceTableOfTwoWords;
    }

    private static String getTwoWordsKey (String w1, String w2) {
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
    private void eval () throws Exception {
        System.out.println(wordList.size() + " instances to clustered.");
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
        for (int cl = 0; cl < numOfCl; cl++) {
            result.add(new ArrayList<String>());
        }
        for (int i = 0; i < data.numInstances(); i++) {
            Instance inst = data.instance(i);
            int cl = clusterer.clusterInstance(inst);
            result.get(cl).add(inst.toString());
        }
        for (int cl = 0; cl < numOfCl; cl++) {
            System.out.println("****");
            System.out.println("Cluster " + cl);
            System.out.println(result.get(cl).toString());
        }
    }

    private static void test3 (List<Status> tweets) throws Exception {
        DomainGetter domainGetter = DomainGetter.getInstance();
        List<String> pages = new ArrayList<String>();
        for (Status t : tweets) {
            for (URLEntity url : t.getURLEntities()) {
                String p = domainGetter.getWebPage(url.getText());
                if (!p.isEmpty()) {
                    pages.add(p);
                }
            }
        }
        System.out.println(pages.size() + " web pages.");
        ClusterWord cw = new ClusterWord(pages);
        cw.setWordList(ClusterWordFeature.getTweetWordList(tweets, NEED_STEM));
        cw.setMinDf(10);
        cw.debug = true;
        long time = SysUtil.getCpuTime();
        cw.clusterWords();
        System.out.println("Time used: " + (SysUtil.getCpuTime() - time));
        cw.eval();
    }

    public static void main (String[] args) throws Exception {
        // ClusterWord.test2();
        // System.exit(0);
        for (long id : UserInfo.KEY_AUTHORS) {
            if (id != 16958346L) {
                continue;
            }
            final List<Status> tweets =
                    Database.getInstance().getAuthorTweets(id,
                            ExampleGetter.TRAIN_START_DATE,
                            ExampleGetter.TEST_END_DATE);
            System.out.println(UserInfo.KA_ID2SCREENNAME.get(id));
            ClusterWord.test3(tweets);
            System.out.println("****");
        }
    }

}
