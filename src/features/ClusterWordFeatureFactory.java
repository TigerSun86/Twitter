package features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.ExampleGetter;
import twitter4j.Status;
import datacollection.Database;
import features.ClusterWord.ClusterWordSetting;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FWordCluster;
import features.FeatureExtractor.FWordCluster.SharedCache;
import features.FeatureExtractor.FeatureGetter;
import features.SimCalculator.SimMode;
import features.WordStatisDoc.EntityType;

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

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        ClusterWord cw = new ClusterWord();
        cw.para = this.para;
        HashMap<String, Integer> word2cl = cw.clusterWords(tweets);

        SharedCache cache = new SharedCache();
        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (int cid = 0; cid < cw.numOfCl; cid++) {
            list.add(new FWordCluster(cid, cw.numOfCl, word2cl, this.para,
                    cache));
        }
        return list;
    }

    @Override
    public Set<String> conflictedFeaturesOfBase () {
        Set<String> result = new HashSet<String>();
        result.add("18RtWord");
        return result;
    }

    private static void test () {
        ClusterWordFeatureFactory fac = new ClusterWordFeatureFactory();
        fac.para.docPara.withOt = true;
        fac.para.docPara.withRt = false;
        fac.para.docPara.withWeb = false;
        fac.para.docPara.entityType = EntityType.HASHTAG;
        fac.para.docPara.numOfWords = -1;

        fac.para.simMode = SimMode.AEMI;
        fac.para.needPrescreen = false;
        fac.para.clAlg = new SingleCutAlg(10, false);
        fac.getNewFeatures(Database.getInstance().getAuthorTweets(16958346L,
                ExampleGetter.TRAIN_START_DATE, ExampleGetter.TEST_START_DATE));
    }

    public static void main (String[] args) {
        test();
    }

}
