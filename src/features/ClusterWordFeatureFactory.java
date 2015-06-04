package features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.Status;
import twitter4j.URLEntity;
import features.ClusterWord.ClusterWordSetting;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FWordCluster;
import features.FeatureExtractor.FWordCluster.SharedCache;
import features.FeatureExtractor.FeatureGetter;

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

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
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
        ClusterWord cw = new ClusterWord(webPages);
        cw.setWordList(getTweetWordList(tweets, this.para.needStem));
        cw.para = this.para;
        HashMap<String, Integer> word2cl = cw.clusterWords();

        SharedCache cache = new SharedCache();
        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (int cid = 0; cid < this.para.numOfCl; cid++) {
            list.add(new FWordCluster(cid, word2cl, this.para, cache));
        }
        return list;
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
