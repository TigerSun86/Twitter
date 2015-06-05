package features;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Status;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FeatureGetter;
import features.WordFeature.DomainMethods;
import features.WordFeature.EntityMethods;
import features.WordFeature.HashMethods;
import features.WordFeature.MentionMethods;
import features.WordFeature.Mode;
import features.WordFeature.Type;
import features.WordFeature.WordMethods;

/**
 * FileName: WordFeatureFactory.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 3, 2015 9:11:35 PM
 */
public class WordFeatureFactory implements FeatureFactory {
    private Type type;
    private Mode mode;
    private int numOfWords;

    public WordFeatureFactory(Type type, Mode mode, int numOfWords) {
        this.type = type;
        this.mode = mode;
        this.numOfWords = numOfWords;
    }

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        EntityMethods methods;
        if (type.equals(Type.WORD)) {
            methods = new WordMethods(false);
        } else if (type.equals(Type.HASH)) {
            methods = new HashMethods();
        } else if (type.equals(Type.MENTION)) {
            methods = new MentionMethods();
        } else { // if(type.equals(Type.DOMAIN))
            methods = new DomainMethods();
        }

        // Convert tweets to entities.
        methods.analyseTweets(tweets);
        List<String> topEntities =
                WordFeature.getTopEntities(methods.getEntitiesInTweets(),
                        methods.getNumOfRts(), mode, numOfWords);
        List<FeatureGetter> list = new ArrayList<FeatureGetter>();
        for (String entity : topEntities) { // Add entities as new features.
            list.add(methods.getFeatureInstance(entity));
        }
        return list;
    }

}
