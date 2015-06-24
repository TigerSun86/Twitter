package features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.ExampleGetter;
import twitter4j.Status;
import datacollection.Database;
import features.EntityPair.EntityPairSetting;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FEntityPair;
import features.FeatureExtractor.FeatureGetter;
import features.SimCalculator.SimMode;
import features.SimTable.Pair;
import features.WordStatisDoc.EntityType;

/**
 * FileName: EntityPairFactory.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 18, 2015 10:06:08 PM
 */
public class EntityPairFactory implements FeatureFactory {
    public static final String PREFIX = "EntityPair_";

    public EntityPairSetting para = new EntityPairSetting();

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        EntityPair ep = new EntityPair();
        ep.para = this.para;
        List<Pair> pairs = ep.getTopEntityPairs(tweets);
        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (Pair p : pairs) {
            list.add(new FEntityPair(p.w1, p.w2));
        }

        return list;
    }

    @Override
    public Set<String> conflictedFeaturesOfBase () {
        return new HashSet<String>();
    }

    private static void test () {
        EntityPairFactory fac = new EntityPairFactory();
        fac.para.docPara.withOt = true;
        fac.para.docPara.withRt = true;
        fac.para.docPara.withWeb = false;
        fac.para.docPara.entityType = EntityType.ALLTYPE;
        fac.para.docPara.numOfWords = -1;
        fac.para.simMode = SimMode.IDF;
        fac.para.numOfPairs = 10;
        fac.para.needPrescreen = false;
        fac.getNewFeatures(Database.getInstance().getAuthorTweets(16958346L,
                ExampleGetter.TRAIN_START_DATE, ExampleGetter.TEST_START_DATE));
    }

    public static void main (String[] args) {
        test();
    }
}
