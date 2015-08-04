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

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;
import util.MyMath;

import com.google.common.primitives.Doubles;

import features.WordFeature.WordSelectingMode;

/**
 * FileName: WordStatisDoc.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 23, 2015 3:58:59 PM
 */
public class WordStatisDoc {
    private static final int LEAST_FREQUENCY = 2;
    private static final boolean DISCARD_USELESS_WORDS_FROM_DOCUMENTS = true;

    public enum EntityType {
        ALLTYPE, WORD, HASHTAG, MENTION, DOMAIN
    }

    public static class WordStatisDocSetting {
        public boolean withOt = true;
        public boolean withRt = false;
        public boolean withWeb = false;
        public EntityType entityType = EntityType.ALLTYPE;
        public int numOfWords = -1;
        // Work when numOfWords > 0
        public WordSelectingMode selectingMode = WordSelectingMode.SUM;

        @Override
        public String toString () {
            return String.format("WordStatisDocSetting: "
                    + "withOt: %b, withRt: %b, withWeb: %b, "
                    + "entityType: %s, numOfWords: %d, selectingMode: %s",
                    withOt, withRt, withWeb, entityType, numOfWords,
                    selectingMode);
        }
    }

    public WordStatisDocSetting para = new WordStatisDocSetting();

    // Results. This part will be initiated every time after call init(tweets).
    public List<String> wordList = null;
    public List<Set<String>> wordSetOfDocs = null;
    public HashMap<String, BitSet> word2DocIds = null;
    public List<Double> numOfRtOfDocs = null;
    public List<Double> logNumOfRtOfDocs = null;

    public WordStatisDoc() {
        this.para = new WordStatisDocSetting();
    }

    public WordStatisDoc(WordStatisDocSetting para) {
        this.para = para;
    }

    public void init (List<Status> tweets) {
        // Must have at least one kind of documents.
        assert para.withOt || para.withWeb;
        // NumOfRetweets only makes sense when having original tweets.
        assert !(para.withRt && !para.withOt);
        // Entity (except word) only makes sense when having original tweets.
        assert !(para.entityType != EntityType.WORD && !para.withOt);

        // Set wordList to be null, so initTweets() will consider all words
        // in tweets.
        wordList = null;

        if (this.para.numOfWords > 0) {
            boolean otBackup = this.para.withOt;
            boolean rtBackup = this.para.withRt;
            this.para.withOt = true;
            this.para.withRt = true;

            initTweets(tweets);
            List<String> topEntities = WordFeature.getTopEntities(this);
            wordList = topEntities; // Only consider the top words.

            this.para.withOt = otBackup;
            this.para.withRt = rtBackup;
        }

        initTweets(tweets);
        if (this.para.withWeb) initWebPages(tweets);
    }

    public static Set<String> getEntitiesFromTweet (Status t,
            EntityType entityType) {
        List<String> entities = new ArrayList<String>();
        if (entityType == EntityType.ALLTYPE || entityType == EntityType.WORD) {
            entities.addAll(WordFeature.splitIntoWords(t, true,
                    FeatureExtractor.NEED_STEM));
        }
        if (entityType == EntityType.ALLTYPE
                || entityType == EntityType.HASHTAG) {
            for (HashtagEntity en : t.getHashtagEntities()) {
                entities.add("#" + en.getText().toLowerCase());
            }
        }
        if (entityType == EntityType.ALLTYPE
                || entityType == EntityType.MENTION) {
            for (UserMentionEntity en : t.getUserMentionEntities()) {
                entities.add("@" + en.getText().toLowerCase());
            }
        }
        if (entityType == EntityType.ALLTYPE || entityType == EntityType.DOMAIN) {
            for (URLEntity en : t.getURLEntities()) {
                String domain =
                        DomainGetter.getInstance().getDomain(en.getURL());
                if (!domain.equals(DomainGetter.UNKNOWN_DOMAIN)) {
                    assert domain.indexOf('.') != -1;
                    entities.add(domain.toLowerCase());
                }
            }
        }
        return new HashSet<String>(entities);
    }

    public static List<Set<String>> getWebPages (List<Status> tweets,
            boolean needStem) {
        DomainGetter domainGetter = DomainGetter.getInstance();
        List<Set<String>> webPages = new ArrayList<Set<String>>();
        for (Status t : tweets) {
            for (URLEntity url : t.getURLEntities()) {
                Set<String> p =
                        domainGetter.getWordsOfWebPage(url.getText(), needStem,
                                DomainGetter.DOMAIN_STOP_WORDS_THRESHOLD);
                if (!p.isEmpty()) {
                    webPages.add(p);
                }
            }
        }
        return webPages;
    }

    public int getDf () {
        return wordSetOfDocs.size();
    }

    public int getDf (String w) {
        return word2DocIds.get(w).cardinality();
    }

    public double getRtSum () {
        assert numOfRtOfDocs != null;
        double totalNumOfRt = 0;
        for (double num : numOfRtOfDocs) {
            totalNumOfRt += num;
        }
        return totalNumOfRt;
    }

    public double getRtSum (String w) {
        assert numOfRtOfDocs != null;
        double totalNumOfRt = 0;
        BitSet docIds = word2DocIds.get(w);
        for (int id = docIds.nextSetBit(0); id >= 0; id =
                docIds.nextSetBit(id + 1)) {
            totalNumOfRt += numOfRtOfDocs.get(id);
        }
        return totalNumOfRt;
    }

    public double getLogRtSum (String w) {
        assert logNumOfRtOfDocs != null;
        double totalNumOfRt = 0;
        BitSet docIds = word2DocIds.get(w);
        for (int id = docIds.nextSetBit(0); id >= 0; id =
                docIds.nextSetBit(id + 1)) {
            totalNumOfRt += logNumOfRtOfDocs.get(id);
        }
        return totalNumOfRt;
    }

    public double getLogRtDev () {
        assert logNumOfRtOfDocs != null;
        return MyMath.getStdDev(Doubles.toArray(logNumOfRtOfDocs));
    }

    public double getLogRtDev (String w) {
        assert logNumOfRtOfDocs != null;
        List<Double> rts = new ArrayList<Double>();
        BitSet docIds = word2DocIds.get(w);
        for (int id = docIds.nextSetBit(0); id >= 0; id =
                docIds.nextSetBit(id + 1)) {
            rts.add(logNumOfRtOfDocs.get(id));
        }
        return MyMath.getStdDev(Doubles.toArray(rts));
    }

    private void initTweets (List<Status> tweets) {
        // wordList could not be null because of pre-specifying. But the
        // specified words might not remain in the wordList finally, because
        // some word might not appear more than twice.
        Set<String> wordSpecified =
                (wordList != null) ? new HashSet<String>(wordList) : null;
        wordList = new ArrayList<String>();
        wordSetOfDocs = new ArrayList<Set<String>>();
        word2DocIds = new HashMap<String, BitSet>();
        numOfRtOfDocs = this.para.withRt ? new ArrayList<Double>() : null;
        logNumOfRtOfDocs = this.para.withRt ? new ArrayList<Double>() : null;

        HashMap<String, Integer> wordCounter = new HashMap<String, Integer>();
        for (Status t : tweets) {
            Set<String> entities =
                    getEntitiesFromTweet(t, this.para.entityType);
            // If specified words at the beginning, only use those words.
            if (wordSpecified != null) entities.retainAll(wordSpecified);
            // Tweet has some entities, so worth to check.
            if (!entities.isEmpty()) {
                if (this.para.withOt) wordSetOfDocs.add(entities);
                for (String e : entities) { // Count frequency of word.
                    Integer c = wordCounter.get(e);
                    if (c == null) c = 0;
                    wordCounter.put(e, c + 1);
                }
                if (this.para.withRt) {
                    double count = t.getRetweetCount();
                    numOfRtOfDocs.add(count);
                    logNumOfRtOfDocs.add(Math.log(count + 1));
                }
            }
        }

        Set<String> wordUsedMoreThanTwice = new HashSet<String>();
        for (Entry<String, Integer> entry : wordCounter.entrySet()) {
            if (entry.getValue() >= LEAST_FREQUENCY) {
                // Only consider the word occurring twice or more.
                wordUsedMoreThanTwice.add(entry.getKey());
            }
        }

        if (DISCARD_USELESS_WORDS_FROM_DOCUMENTS && this.para.withOt) {
            for (Set<String> doc : wordSetOfDocs) {
                doc.retainAll(wordUsedMoreThanTwice);
            }
            Iterator<Set<String>> iter = wordSetOfDocs.iterator();
            while (iter.hasNext()) {
                if (iter.next().isEmpty()) {
                    iter.remove();
                }
            }
        }

        wordList.addAll(wordUsedMoreThanTwice);
        Collections.sort(wordList); // Sort words more clear for debug.

        for (String word : wordList) {
            word2DocIds.put(word, new BitSet(wordSetOfDocs.size()));
        }
        if (this.para.withOt) {
            for (int id = 0; id < wordSetOfDocs.size(); id++) {
                Set<String> doc = wordSetOfDocs.get(id);
                for (String word : doc) {
                    if (word2DocIds.containsKey(word))
                        word2DocIds.get(word).set(id);
                }
            }
        }
    }

    private void initWebPages (List<Status> tweets) {
        List<Set<String>> pages =
                getWebPages(tweets, FeatureExtractor.NEED_STEM);
        Set<String> wordSet = new HashSet<String>(this.wordList);
        // firstWebIdx could be 0 if withOt == false
        int firstWebIdx = wordSetOfDocs.size();
        for (Set<String> p : pages) {
            if (DISCARD_USELESS_WORDS_FROM_DOCUMENTS) {
                p.retainAll(wordSet);
            }
            if (!p.isEmpty()) {
                wordSetOfDocs.add(p);
            }
        }
        for (int id = firstWebIdx; id < wordSetOfDocs.size(); id++) {
            Set<String> doc = wordSetOfDocs.get(id);
            for (String word : doc) {
                if (word2DocIds.containsKey(word)) {
                    word2DocIds.get(word).set(id);
                }
            }
        }
    }
}
