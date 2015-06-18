package features;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import twitter4j.Status;
import features.FeatureExtractor.FeatureGetter;

/**
 * FileName: FeatureEditor.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 2, 2015 5:21:56 PM
 */
public class FeatureEditor {
    private static final boolean REMOVE_CONFLICTED_FEATURE = false;

    public interface FeatureFactory {
        List<FeatureGetter> getNewFeatures (List<Status> tweets);

        Set<String> conflictedFeaturesOfBase ();
    }

    private List<FeatureFactory> newFeatures;
    private boolean keepBase;
    public String name;

    public FeatureEditor(List<FeatureFactory> newFeatures, boolean keepBase,
            String name) {
        this.newFeatures = newFeatures;
        this.keepBase = keepBase;
        this.name = name;
    }

    public FeatureEditor(List<FeatureFactory> newFeatures, String name) {
        this.newFeatures = newFeatures;
        this.keepBase = true;
        this.name = name;
    }

    public void
            setFeature (FeatureExtractor featureGetters, List<Status> tweets) {
        // Get conflicted features.
        Set<String> conflictedFeatures = new HashSet<String>();
        if (REMOVE_CONFLICTED_FEATURE) {
            for (FeatureFactory newFeature : newFeatures) {
                conflictedFeatures
                        .addAll(newFeature.conflictedFeaturesOfBase());
            }
        }

        // First remove all features
        featureGetters.getterListOfPreNum.clear();
        if (keepBase) { // Add back base features
            for (FeatureGetter baseFeature : FeatureExtractor.BASE_FEATURE_SET) {
                if (!conflictedFeatures.contains(baseFeature.toString())) {
                    featureGetters.getterListOfPreNum.add(baseFeature);
                }
            }
        }
        // Add new features;
        for (FeatureFactory newFeature : newFeatures) {
            List<FeatureGetter> nfs = newFeature.getNewFeatures(tweets);
            for (FeatureGetter fg : nfs) {
                featureGetters.getterListOfPreNum.add(fg);
            }
        }
    }
}
