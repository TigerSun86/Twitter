package features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.Status;
import twitter4j.URLEntity;
import features.FeatureExtractor.FWordCluster;
import features.FeatureExtractor.FeatureGetter;
import features.NewFeatureInserter.NewFeatureGettable;

/**
 * FileName: ClusterWordFeature.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 2, 2015 5:38:05 PM
 */
public class ClusterWordFeature implements NewFeatureGettable {
    public static final String PREFIX = "ClusterWord_";
    private List<Status> tweets;

    public ClusterWordFeature(List<Status> tweets) {
        this.tweets = tweets;
    }

    @Override
    public List<FeatureGetter> getNewFeatures () {
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
        ClusterWord cw = new ClusterWord(webPages);
        cw.setWordList(getTweetWordList(tweets, ClusterWord.NEED_STEM));
        cw.setMinDf(0);
        cw.setNumOfCl(getNumOfCl());
        cw.setNeedStem(getNeedStem());
        HashMap<String, Integer> word2cl = cw.clusterWords();

        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (int cid = 0; cid < getNumOfCl(); cid++) {
            list.add(new FWordCluster(cid, word2cl));
        }
        return list;
    }

    @Override
    public String getNewFeaturePrefix () {
        return PREFIX;
    }

    public static int getNumOfCl () {
        return ClusterWord.NUM_OF_CL;
    }

    public static boolean getNeedStem () {
        return ClusterWord.NEED_STEM;
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
}
