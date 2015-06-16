package learners;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;
import weka.core.SelectedTag;

import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExampleList;
import common.WLearner;

/**
 * FileName: WLibSvm.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date May 19, 2015 10:59:56 PM
 */
public class WLibSvm implements Learner, WLearner {
    private int sType;
    private int kType;

    public WLibSvm(int sType, int kType) {
        this.sType = sType;
        this.kType = kType;
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
            LibSVM cls = new LibSVM();
            cls.setSVMType(new SelectedTag(sType, LibSVM.TAGS_SVMTYPE));
            cls.setSVMType(new SelectedTag(kType, LibSVM.TAGS_KERNELTYPE));
            cls.buildClassifier(data);
            return cls;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
