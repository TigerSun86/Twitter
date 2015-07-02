package features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.ExampleGetter;
import twitter4j.Status;
import util.Dbg;
import datacollection.Database;
import datacollection.UserInfo;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FTopEntity;
import features.FeatureExtractor.FeatureGetter;
import features.WordFeature.WordSelectingMode;
import features.WordStatisDoc.EntityType;

/**
 * FileName: WordFeatureFactory.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 3, 2015 9:11:35 PM
 */
public class WordFeatureFactory implements FeatureFactory {
    public static final String FEATURE_PRIFIX = "TopEntity_";

    private EntityType type;
    private int numOfWords;
    private WordSelectingMode selectingMode;

    public WordFeatureFactory(EntityType type, int numOfWords,
            WordSelectingMode selectingMode) {
        this.type = type;
        this.numOfWords = numOfWords;
        this.selectingMode = selectingMode;
    }

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        WordStatisDoc doc = new WordStatisDoc();
        doc.para.withOt = true;
        doc.para.withRt = true;
        doc.para.withWeb = false;
        doc.para.entityType = this.type;
        doc.para.numOfWords = this.numOfWords;
        doc.para.selectingMode = this.selectingMode;
        doc.init(tweets);
        List<String> topEntities = doc.wordList;
        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (String entity : topEntities) { // Add entities as new features.
            list.add(new FTopEntity(entity));
        }
        if (Dbg.dbg) {
            System.out.println("Selected " + topEntities.size() + " entities.");
        }
        return list;
    }

    @Override
    public Set<String> conflictedFeaturesOfBase () {
        Set<String> result = new HashSet<String>();
        if (type == EntityType.ALLTYPE || type == EntityType.WORD) {
            result.add("18RtWord");
        } else if (type == EntityType.ALLTYPE || type == EntityType.HASHTAG) {
            result.add("29Hashtag");
        } else if (type == EntityType.ALLTYPE || type == EntityType.MENTION) {
            result.add("30Mention");
        } else if (type == EntityType.ALLTYPE || type == EntityType.DOMAIN) {
            result.add("5Url");
        }
        return result;
    }

    private static void test () {
        for (long authorId : UserInfo.KEY_AUTHORS) {
            if (authorId != 16958346L) {
                // continue;
            }
            System.out.println("**** Author: "
                    + UserInfo.KA_ID2SCREENNAME.get(authorId));
            WordFeatureFactory fac =
                    new WordFeatureFactory(EntityType.ALLTYPE, 30,
                            WordSelectingMode.SUM2);
            fac.getNewFeatures(Database.getInstance().getAuthorTweets(authorId,
                    ExampleGetter.TRAIN_START_DATE,
                    ExampleGetter.TEST_START_DATE));
        }
    }

    public static void main (String[] args) {
        test();
    }

}
