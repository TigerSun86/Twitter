package learners;

import weka.classifiers.Classifier;
import weka.core.Instances;

import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExampleList;
import common.WLearner;

/**
 * FileName: WAnn.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 3, 2015 12:34:56 AM
 */
public class WAnn implements Learner, WLearner {
    private int node = 10;
    public int iter = 5000;

    public WAnn(int node) {
        this.node = node;
    }

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
            weka.classifiers.functions.MultilayerPerceptron cls =
                    new weka.classifiers.functions.MultilayerPerceptron();
            cls.setTrainingTime(iter);
            cls.setValidationSetSize(30);
            cls.setLearningRate(0.1);
            cls.setMomentum(0.2);
            cls.setHiddenLayers("" + node);
            cls.setNormalizeAttributes(false);
            cls.setNormalizeNumericClass(false);
            cls.buildClassifier(data);
            return cls;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
