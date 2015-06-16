package features;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.Status;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor.FeatureGetter;

/**
 * FileName: BaseFeatureFactory.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 3, 2015 8:52:27 PM
 */
public class BaseFeatureFactory implements FeatureFactory {

    @Override
    public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
        // Do nothing but need to keep base in FeatureEditor's constructor.
        return new ArrayList<FeatureGetter>();
    }

    @Override
    public Set<String> conflictedFeaturesOfBase () {
        return new HashSet<String>();
    }

}
