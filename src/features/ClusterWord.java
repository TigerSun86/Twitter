package features;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
    public static final int MIN_DF = 0;
    public static final int NUM_OF_CL = 10;
    public static final int MODE_LIFT = 0;
    public static final int MODE_JACCARD = 1;

    public static class ClusterWordSetting {
        public int minDf = MIN_DF;
        public int numOfCl = NUM_OF_CL;
        public boolean needStem = false;
        public int mode = MODE_LIFT;
        public boolean mEstimate = true;
    }

    public ClusterWordSetting para = new ClusterWordSetting();

    private static final String WORD_SEPARATER = ",";

    private boolean debug = false;
    private int countOfInvalidPair = 0; // For debug.

    private List<String> wordList = null;
    private List<String> pages = null;
    private List<HashSet<String>> wordSetOfDocs = null;
    private HashMap<String, BitSet> word2DocIds = null;
    private Clusterer clusterer = null; // For debug.

    public ClusterWord(List<String> pages) {
        this.pages = pages;
    }

    public void setWordList (List<String> wordList) {
        // Actually the content of the inputed wordList won't be changed
        // because method filterWords will renew wordList.
        this.wordList = wordList;
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
            if (df < this.para.minDf) {
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
                if (this.para.needStem) {
                    word = stemmer.stem(word);
                }
                doc.add(word);
            }
            docs.add(doc);
        }
        return docs;
    }

    /**
     * Lift(a,b) = p(a,b)/(p(a)p(b)) = N*df(a,b)/(df(a)*df(b))
     * In m-estimate Lift(a,b) = (N+1)* (df(a,b)+m)/((df(a)+m )*(df(b)+m))
     * Distance = 1/Lift(a,b)
     */
    private double distanceOfLift (String w1, String w2) {
        BitSet seta = word2DocIds.get(w1);
        BitSet setb = word2DocIds.get(w2);
        BitSet intersection = (BitSet) seta.clone();
        intersection.and(setb);
        double dfab = intersection.cardinality();
        if (dfab == 0) {
            countOfInvalidPair++;
        }
        double dfa = seta.cardinality();
        double dfb = setb.cardinality();
        double dist;
        double n = (double) wordSetOfDocs.size();
        if (this.para.mEstimate) {
            double m = 1 / n;
            double lift = (n + 1) * (dfab + m) / ((dfa + m) * (dfb + m));
            dist = 1 / lift;
        } else {
            if (dfab == 0) {
                dist = Double.POSITIVE_INFINITY;
            } else {
                double lift = n * dfab / (dfa * dfb);
                dist = 1 / lift;
            }
        }
        return dist;
    }

    /**
     * Jaccard(a,b) = df(a and b)/ df(a or b)
     * In m-estimate Jaccard(a,b) = (df(a and b) + m)/ (df(a or b)+1)
     * Distance = 1/Jaccard(a,b)
     */
    private double distanceOfJaccard (String w1, String w2) {
        BitSet seta = word2DocIds.get(w1);
        BitSet setb = word2DocIds.get(w2);
        BitSet intersection = (BitSet) seta.clone();
        intersection.and(setb);
        double dfaandb = intersection.cardinality();
        if (dfaandb == 0) {
            countOfInvalidPair++;
        }
        BitSet union = (BitSet) seta.clone();
        union.or(setb);
        double dfaorb = union.cardinality();

        double dist;
        if (this.para.mEstimate) {
            double n = (double) wordSetOfDocs.size();
            double m = 1 / n;
            double jaccard = (dfaandb + m) / (dfaorb + 1);
            dist = 1 / jaccard;
        } else {
            if (dfaandb == 0) {
                dist = Double.POSITIVE_INFINITY;
            } else {
                double jaccard = dfaandb / dfaorb;
                dist = 1 / jaccard;
            }
        }
        return dist;
    }

    private HashMap<String, Double> getDistanceTable () {
        HashMap<String, Double> distanceTableOfTwoWords =
                new HashMap<String, Double>();
        countOfInvalidPair = 0;// For debug.
        for (int i = 0; i < wordList.size(); i++) {
            String w1 = wordList.get(i);
            for (int j = i; j < wordList.size(); j++) {
                String w2 = wordList.get(j);
                if (w1.equals(w2)) {
                    // The distance of a word itself should be 0.
                    distanceTableOfTwoWords.put(getTwoWordsKey(w1, w2), 0.0);
                } else {
                    double dist;
                    if (this.para.mode == MODE_LIFT) {
                        dist = distanceOfLift(w1, w2);
                    } else {
                        dist = distanceOfJaccard(w1, w2);
                    }
                    assert !Double.isNaN(dist);
                    distanceTableOfTwoWords.put(getTwoWordsKey(w1, w2), dist);
                }
            }
        }
        if (debug) {
            // Debug info.
            int to = wordList.size();
            int total = (to * to - to) / 2;
            System.out.printf("%d word-pairs are invalid, among total of %d, "
                    + "the sparsity of upper-triangle is %.2f%%%n",
                    countOfInvalidPair, total,
                    ((double) countOfInvalidPair * 100.0) / total);
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

    private static class MyWordDistance extends EditDistance {
        private static final long serialVersionUID = 1L;
        private final HashMap<String, Double> distanceTableOfTwoWords;

        public MyWordDistance(HashMap<String, Double> distanceTableOfTwoWords) {
            this.distanceTableOfTwoWords = distanceTableOfTwoWords;
        }

        private double distanceOfTwoWords (String w1, String w2) {
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
        System.out.println(wordList.size() + " words to clustered.");
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
    }

    private static void test (List<Status> tweets) throws Exception {
        ClusterWordSetting para = new ClusterWordSetting();
        para.minDf = 500;

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
        pages.addAll(ClusterWordFeatureFactory.getTweetPages(tweets,
                para.needStem));
        System.out.println(pages.size() + " web pages.");

        List<String> wlist =
                ClusterWordFeatureFactory.getTweetWordList(tweets,
                        para.needStem);

        ClusterWord cw = new ClusterWord(pages);
        cw.setWordList(wlist);
        cw.para = para;
        cw.debug = true;
        long time = SysUtil.getCpuTime();
        cw.clusterWords();
        System.out.println("Time used: " + (SysUtil.getCpuTime() - time));
        cw.eval();
    }

    public static void main (String[] args) throws Exception {
        for (long id : UserInfo.KEY_AUTHORS) {
            if (id != 16958346L) {
                continue;
            }
            final List<Status> tweets =
                    Database.getInstance().getAuthorTweets(id,
                            ExampleGetter.TRAIN_START_DATE,
                            ExampleGetter.TEST_END_DATE);
            System.out.println(UserInfo.KA_ID2SCREENNAME.get(id));
            ClusterWord.test(tweets);
            System.out.println("****");
        }
    }

}
