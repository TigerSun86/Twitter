package features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            methods = new WordMethods(FeatureExtractor.NEED_STEM);
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

    @Override
    public Set<String> conflictedFeaturesOfBase () {
        Set<String> result = new HashSet<String>();
        if (type.equals(Type.WORD)) {
            result.add("18RtWord");
        } else if (type.equals(Type.HASH)) {
            result.add("29Hashtag");
        } else if (type.equals(Type.MENTION)) {
            result.add("30Mention");
        } else { // if(type.equals(Type.DOMAIN))
            result.add("5Url");
        }
        return result;
    }

}
