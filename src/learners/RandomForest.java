package learners;

import weka.core.Instances;

import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExampleList;

/**
 * FileName: RandomForest.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 30, 2015 10:10:13 PM
 */
public class RandomForest implements Learner {
    @Override
    public ProbPredictor learn (RawExampleList dataSet, RawAttrList attrs) {
        if (!MeToWeka.hasSetAttribute()) {
            MeToWeka.setAttributes(attrs);
        }
        Instances train = MeToWeka.convertInstances(dataSet);
        try {
            weka.classifiers.trees.RandomForest cls =
                    new weka.classifiers.trees.RandomForest();
            cls.buildClassifier(train);
            return new WekaPredictor(cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
