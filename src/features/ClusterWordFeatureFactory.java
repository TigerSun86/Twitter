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

    private List<String> allAuthorWTPageCache = null;
    private List<String> allAuthorNTPageCache = null;

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        List<String> webPages;
        if (allAuthors) {
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
                                    ExampleGetter.TEST_END_DATE);
                    allTweets.addAll(auTweets);
                }
                webPages = getPages(allTweets);
                if (withTweets) {
                    allAuthorWTPageCache = webPages;
                } else {
                    allAuthorNTPageCache = webPages;
                }
            }
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

    private List<String> getPages (List<Status> tweets) {
        DomainGetter domainGetter = DomainGetter.getInstance();
        List<String> webPages = new ArrayList<String>();
        for (Status t : tweets) {
            for (URLEntity url : t.getURLEntities()) {
                String p = domainGetter.getWebPage(url.getText());
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

    static List<String> getTweetWordList (List<Status> ts, boolean needStem) {
        Set<String> set = new HashSet<String>();
        for (Status t : ts) {
            List<String> doc = WordFeature.splitIntoWords(t, true, needStem);
            for (String w : doc) {
                set.add(w);
            }
        }
        return new ArrayList<String>(set);
    }

    static List<String> getTweetPages (List<Status> ts, boolean needStem) {
        List<String> pages = new ArrayList<String>();
        for (Status t : ts) {
            StringBuilder sb = new StringBuilder();
            List<String> doc = WordFeature.splitIntoWords(t, true, needStem);
            for (String w : doc) {
                sb.append(w + " ");
            }
            if (sb.length() > 0) {
                pages.add(sb.toString());
            }
        }
        return pages;
    }
}
