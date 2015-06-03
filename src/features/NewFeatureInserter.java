package features;

import java.util.Iterator;
import java.util.List;

import common.RawAttr;

import features.FeatureExtractor.FeatureGetter;

/**
 * FileName: NewFeatureInserter.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 2, 2015 5:21:56 PM
 */
public class NewFeatureInserter {

    public interface NewFeatureGettable {
        List<FeatureGetter> getNewFeatures ();

        String getNewFeaturePrefix ();
    }

    public static void setFeature (FeatureExtractor featureGetters,
            NewFeatureGettable newFeature) {
        // First remove all specified type features.
        String pre = newFeature.getNewFeaturePrefix();
        Iterator<FeatureGetter> iter =
                featureGetters.getterListOfPreNum.iterator();
        while (iter.hasNext()) {
            FeatureGetter f = iter.next();
            RawAttr attr = f.getAttr();
            if (attr.name.startsWith(pre)) {
                iter.remove();
            }
        }
        // Add new features;
        List<FeatureGetter> nfs = newFeature.getNewFeatures();
        for (FeatureGetter fg : nfs) {
            featureGetters.getterListOfPreNum.add(fg);
        }
    }
}
