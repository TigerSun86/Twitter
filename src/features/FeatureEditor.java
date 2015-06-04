package features;

import java.util.List;

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
    public interface FeatureFactory {
        List<FeatureGetter> getNewFeatures (List<Status> tweets);
    }

    private FeatureFactory newFeature;
    private boolean keepBase;
    public String name;

    public FeatureEditor(FeatureFactory newFeature, boolean keepBase,
            String name) {
        this.newFeature = newFeature;
        this.keepBase = keepBase;
        this.name = name;
    }

    public FeatureEditor(FeatureFactory newFeature, String name) {
        this.newFeature = newFeature;
        this.keepBase = true;
        this.name = name;
    }

    public void
            setFeature (FeatureExtractor featureGetters, List<Status> tweets) {
        // First remove all features
        featureGetters.getterListOfPreNum.clear();
        if (keepBase) { // Add back base features
            featureGetters.getterListOfPreNum
                    .addAll(FeatureExtractor.BASE_FEATURE_SET);
        }
        // Add new features;
        List<FeatureGetter> nfs = newFeature.getNewFeatures(tweets);
        for (FeatureGetter fg : nfs) {
            featureGetters.getterListOfPreNum.add(fg);
        }
    }
}
