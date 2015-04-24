package learners;

import weka.classifiers.Classifier;
import weka.core.Instances;
import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExampleList;
import common.WLearner;

/**
 * FileName: WLr.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 8, 2015 5:43:19 PM
 */
public class WLr implements Learner, WLearner {
    @Override
    public ProbPredictor learn (RawExampleList dataSet, RawAttrList attrs) {
        MeToWeka w = new MeToWeka(attrs);
        Instances train = w.convertInstances(dataSet);
        Classifier cls = buildClassifier(train);
        return new WekaPredictor(cls, w);
    }

    @Override
    public Classifier buildClassifier (Instances data) {
        try {
            weka.classifiers.functions.LinearRegression cls =
                    new weka.classifiers.functions.LinearRegression();
            cls.buildClassifier(data);
            return cls;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
