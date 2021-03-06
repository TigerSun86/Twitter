package features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.ExampleGetter;
import twitter4j.Status;
import datacollection.Database;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FLda;
import features.FeatureExtractor.FLda.LdaSharedCache;
import features.FeatureExtractor.FeatureGetter;
import features.Lda.InferencerAndPipe;
import features.Lda.LdaSetting;
import features.WordStatisDoc.EntityType;

/**
 * FileName: LdaFeatureFactory.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Aug 3, 2015 10:31:40 PM
 */
public class LdaFeatureFactory implements FeatureFactory {
    public static final String PREFIX = "Lda_";

    public LdaSetting para = new LdaSetting();

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        Lda cw = new Lda();
        cw.para = this.para;
        InferencerAndPipe model = cw.cluster(tweets);

        LdaSharedCache cache = new LdaSharedCache();
        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (int cid = 0; cid < para.numOfCl; cid++) {
            list.add(new FLda(cid, model.inferencer, model.pipe, cache));
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
        LdaFeatureFactory fac = new LdaFeatureFactory();
        fac.para.docPara.withOt = true;
        fac.para.docPara.withRt = false;
        fac.para.docPara.withWeb = false;
        fac.para.docPara.entityType = EntityType.WORD;
        fac.para.docPara.numOfWords = -1;

        fac.para.numOfCl = 10;
        fac.para.numOfIter = 2000;
        fac.getNewFeatures(Database.getInstance().getAuthorTweets(16958346L,
                ExampleGetter.TRAIN_START_DATE, ExampleGetter.TEST_START_DATE));
    }

    public static void main (String[] args) {
        test();
    }

}
