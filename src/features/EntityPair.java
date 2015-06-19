package features;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;
import util.Dbg;
import features.SimCalculator.Pair;

/**
 * FileName: EntityPair.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 18, 2015 7:37:31 PM
 */
public class EntityPair {

    public static class EntityPairSetting {
        public SimCalculator.Mode mode = SimCalculator.Mode.JACCARD;
        public boolean mEstimate = true;
        public boolean needEntity = true;
        public int num = 10;
    }

    public EntityPairSetting para = new EntityPairSetting();

    private List<String> wordList = null;
    private List<Set<String>> wordSetOfDocs = null;
    private HashMap<String, BitSet> word2DocIds = null;
    private List<Integer> numOfRtOfDocs = null;

    public List<Pair> getTopEntities (List<Status> tweets) {
        init(tweets);
        SimCalculator simCal =
                new SimCalculator(para.mode, para.mEstimate, true, wordList,
                        wordSetOfDocs, word2DocIds, numOfRtOfDocs);
        HashMap<String, Double> simTable = simCal.getSimilarityTable();
        List<Pair> pairs = Pair.getDescendingPairs(simTable);
        List<Pair> topPairs = new ArrayList<Pair>();
        for (int i = 0; i < Math.min(para.num, pairs.size()); i++) {
            topPairs.add(pairs.get(i));
        }
        if (Dbg.dbg) {
            int maxDf = 0;
            String maxPair = "";
            for (int i = 0; i < wordList.size(); i++) {
                String w1 = wordList.get(i);
                for (int j = i; j < wordList.size(); j++) {
                    String w2 = wordList.get(j);
                    if (!w1.equals(w2)) {
                        int df = simCal.getDfaAndb(w1, w2, true);
                        if (maxDf < df) {
                            maxDf = df;
                            maxPair = w1 + SimCalculator.WORD_SEPARATER + w2;
                        }
                    }
                }
            }
            System.out.printf("Author: %s, number of tweets: %d, "
                    + "number of top pairs: %d, mode: %s, needEntity: %b, "
                    + "most frequent pair: %s, frequence: %s%n", tweets.get(0)
                    .getUser().getScreenName(), tweets.size(), topPairs.size(),
                    para.mode.toString(), para.needEntity, maxPair, maxDf);
            System.out.println("Top pairs:");
            for (Pair p : topPairs) {
                String[] ens = p.e.split(SimCalculator.WORD_SEPARATER);
                String w1 = ens[0];
                String w2 = ens[1];
                System.out.printf("%s, Df: %d, NumOfRt: %d, %s_Df: %d, "
                        + "%s_NumOfRt: %d, %s_Df: %d, %s_NumOfRt: %d%n",
                        p.toString(), simCal.getDfaAndb(w1, w2, true),
                        simCal.getDfaAndb(w1, w2, false), w1,
                        simCal.getDfw(w1, true), w1, simCal.getDfw(w1, false),
                        w2, simCal.getDfw(w2, true), w2,
                        simCal.getDfw(w2, false));
            }
        }
        return topPairs;
    }

    public static Set<String>
            getEntitiesFromTweet (Status t, boolean needEntity) {
        List<String> entities =
                WordFeature.splitIntoWords(t, true, FeatureExtractor.NEED_STEM);
        if (needEntity) { // Add entities.
            for (HashtagEntity en : t.getHashtagEntities()) {
                entities.add("#" + en.getText());
            }
            for (UserMentionEntity en : t.getUserMentionEntities()) {
                entities.add("@" + en.getText());
            }
            for (URLEntity en : t.getURLEntities()) {
                String domain =
                        DomainGetter.getInstance().getDomain(en.getURL());
                if (!domain.equals(DomainGetter.UNKNOWN_DOMAIN)) {
                    assert domain.indexOf('.') != -1;
                    entities.add(domain);
                }
            }
        }
        return new HashSet<String>(entities);
    }

    private void init (List<Status> tweets) {
        wordSetOfDocs = new ArrayList<Set<String>>();
        numOfRtOfDocs = new ArrayList<Integer>();
        Set<String> wordSet = new HashSet<String>();
        for (Status t : tweets) {
            Set<String> entities =
                    getEntitiesFromTweet(t, this.para.needEntity);
            wordSetOfDocs.add(entities);
            wordSet.addAll(entities);
            numOfRtOfDocs.add(t.getRetweetCount());
        }
        wordList = new ArrayList<String>(wordSet);
        Collections.sort(wordList);

        word2DocIds = new HashMap<String, BitSet>();
        for (String word : wordList) {
            word2DocIds.put(word, new BitSet(wordSetOfDocs.size()));
        }
        for (int id = 0; id < wordSetOfDocs.size(); id++) {
            Set<String> doc = wordSetOfDocs.get(id);
            for (String word : doc) {
                assert word2DocIds.containsKey(word);
                word2DocIds.get(word).set(id);
            }
        }

    }
}
