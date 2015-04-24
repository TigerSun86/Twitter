package common;

import weka.core.Instances;

/**
 * FileName: WLearner.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 24, 2015 2:23:47 AM
 */
public interface WLearner {
    public weka.classifiers.Classifier buildClassifier (Instances data);
}
