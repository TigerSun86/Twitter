package learners;

import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.core.Instance;

import common.ProbPredictor;

/**
 * FileName: WekaPredictor.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 30, 2015 9:52:57 PM
 */
public class WekaPredictor extends ProbPredictor {
    private final Classifier cls;
    private final MeToWeka weka;

    public WekaPredictor(Classifier cls2, MeToWeka w) {
        this.cls = cls2;
        this.weka = w;
    }

    @Override
    public double predictPosProb (ArrayList<String> attrs) {
        Instance inst = weka.getInstForPredict(attrs);
        try {
            double[] dist = cls.distributionForInstance(inst);
            return dist[0];
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return 0;
    }
}
