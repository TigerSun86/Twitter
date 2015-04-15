package learners;

import weka.core.Instances;

import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExampleList;

/**
 * FileName:     WAnn.java
 * @Description: 
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 3, 2015 12:34:56 AM 
 */
public class WAnn implements Learner{
    private int node = 10;
    public WAnn(int node){
        this.node = node;
    }
    @Override
    public ProbPredictor learn (RawExampleList dataSet, RawAttrList attrs) {
        if (!MeToWeka.hasSetAttribute()) {
            MeToWeka.setAttributes(attrs);
        }
        Instances train = MeToWeka.convertInstances(dataSet);
        try {
            weka.classifiers.functions.MultilayerPerceptron cls =
                    new weka.classifiers.functions.MultilayerPerceptron();
            cls.setTrainingTime(1000);
            cls.setValidationSetSize(30);
            cls.setLearningRate(0.1);
            cls.setMomentum(0.2);
            cls.setHiddenLayers(""+node);
            // cls.setNormalizeAttributes(false);
            // cls.setNormalizeNumericClass(false);
            cls.buildClassifier(train);
            return new WekaPredictor(cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
