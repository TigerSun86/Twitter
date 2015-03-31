package learners;

import weka.classifiers.trees.J48;
import weka.core.Instances;

import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExampleList;

/**
 * FileName: WekaDt.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 30, 2015 9:29:52 PM
 */
public class WekaDt implements Learner {
    private final boolean prune;

    public WekaDt() {
        this(true);
    }

    public WekaDt(boolean prune) {
        this.prune = prune;
    }

    @Override
    public ProbPredictor learn (RawExampleList dataSet, RawAttrList attrs) {
        if (!MeToWeka.hasSetAttribute()) {
            MeToWeka.setAttributes(attrs);
        }
        Instances train = MeToWeka.convertInstances(dataSet);
        try {
            J48 cls = new J48();
            if (!prune) cls.setOptions(new String[] { "-U" });
            cls.buildClassifier(train);
            return new WekaPredictor(cls);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
