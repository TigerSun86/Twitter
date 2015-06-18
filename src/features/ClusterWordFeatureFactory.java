package features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.ExampleGetter;
import twitter4j.Status;
import twitter4j.URLEntity;
import datacollection.Database;
import datacollection.UserInfo;
import features.ClusterWord.ClusterWordSetting;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FWordCluster;
import features.FeatureExtractor.FWordCluster.SharedCache;
import features.FeatureExtractor.FeatureGetter;
import features.WordFeature.EntityMethods;
import features.WordFeature.WordMethods;

/**
 * FileName: ClusterWordFeatureFactory.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 2, 2015 5:38:05 PM
 */
public class ClusterWordFeatureFactory implements FeatureFactory {
    public static final String PREFIX = "ClusterWord_";

    public ClusterWordSetting para = new ClusterWordSetting();
    public boolean withTweets = false;
    public boolean allAuthors = false;
    public int numOfWords = 0; // 0 means no limitation.
    public WordFeature.Mode mode = WordFeature.Mode.SUM;

    private List<Set<String>> allAuthorWTPageCache = null;
    private List<Set<String>> allAuthorNTPageCache = null;

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        List<Set<String>> webPages;
        if (allAuthors) {
            webPages = getWebPagesForAllAuthors();
        } else {
            webPages = getPages(tweets);
        }

        List<String> wordList;
        if (numOfWords <= 0) {
            wordList = getTweetWordList(tweets, this.para.needStem);
        } else {
            EntityMethods methods = new WordMethods(this.para.needStem);
            methods.analyseTweets(tweets);
            List<String> topEntities =
                    WordFeature.getTopEntities(methods.getEntitiesInTweets(),
                            methods.getNumOfRts(), mode, numOfWords);
            wordList = topEntities;
        }

        ClusterWord cw = new ClusterWord(webPages);
        cw.setWordList(wordList);
        cw.para = this.para;
        HashMap<String, Integer> word2cl = cw.clusterWords();

        SharedCache cache = new SharedCache();
        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (int cid = 0; cid < this.para.numOfCl; cid++) {
            list.add(new FWordCluster(cid, word2cl, this.para, cache));
        }
        return list;
    }

    @Override
    public Set<String> conflictedFeaturesOfBase () {
        Set<String> result = new HashSet<String>();
        result.add("18RtWord");
        return result;
    }

    private List<Set<String>> getWebPagesForAllAuthors () {
        List<Set<String>> webPages;
        if (withTweets && allAuthorWTPageCache != null) {
            webPages = allAuthorWTPageCache;
        } else if (!withTweets && allAuthorNTPageCache != null) {
            webPages = allAuthorNTPageCache;
        } else {
            List<Status> allTweets = new ArrayList<Status>();
            for (long authorId : UserInfo.KEY_AUTHORS) {
                final List<Status> auTweets =
                        Database.getInstance().getAuthorTweets(authorId,
                                ExampleGetter.TRAIN_START_DATE,
                                ExampleGetter.TEST_START_DATE);
                allTweets.addAll(auTweets);
            }
            webPages = getPages(allTweets);
            if (withTweets) {
                allAuthorWTPageCache = webPages;
            } else {
                allAuthorNTPageCache = webPages;
            }
        }
        return webPages;
    }

    private List<Set<String>> getPages (List<Status> tweets) {
        DomainGetter domainGetter = DomainGetter.getInstance();
        List<Set<String>> webPages = new ArrayList<Set<String>>();
        for (Status t : tweets) {
            for (URLEntity url : t.getURLEntities()) {
                Set<String> p =
                        domainGetter.getWordsOfWebPage(url.getText(),
                                para.needStem,
                                DomainGetter.DOMAIN_STOP_WORDS_THRESHOLD);
                if (!p.isEmpty()) {
                    webPages.add(p);
                }
            }
        }
        if (withTweets) {
            webPages.addAll(ClusterWordFeatureFactory.getTweetPages(tweets,
                    para.needStem));
        }
        return webPages;
    }

    private static List<String> getTweetWordList (List<Status> ts,
            boolean needStem) {
        Set<String> set = new HashSet<String>();
        for (Status t : ts) {
            List<String> doc = WordFeature.splitIntoWords(t, true, needStem);
            for (String w : doc) {
                set.add(w);
            }
        }
        return new ArrayList<String>(set);
    }

    private static List<Set<String>> getTweetPages (List<Status> ts,
            boolean needStem) {
        List<Set<String>> pages = new ArrayList<Set<String>>();
        for (Status t : ts) {
            List<String> doc = WordFeature.splitIntoWords(t, true, needStem);
            if (!doc.isEmpty()) {
                Set<String> set = new HashSet<String>(doc);
                pages.add(set);
            }
        }
        return pages;
    }

    private static void test () {
        ClusterWordFeatureFactory fac = new ClusterWordFeatureFactory();
        fac.numOfWords = 1000;
        fac.para.mEstimate = false;
        fac.withTweets = false;
        fac.getNewFeatures(Database.getInstance().getAuthorTweets(3459051L,
                ExampleGetter.TRAIN_START_DATE, ExampleGetter.TEST_START_DATE));
    }

    public static void main (String[] args) {
        test();
    }

}
