package learners;

import java.util.ArrayList;

import main.ExampleGetter;
import weka.classifiers.Classifier;
import weka.core.Instance;
import common.ProbPredictor;
import common.RawExample;

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

    public WekaPredictor(Classifier cls2) {
        this.cls = cls2;
    }

    @Override
    public double predictPosProb (ArrayList<String> attrs) {
        RawExample e = new RawExample();
        e.xList = attrs;
        e.t = ExampleGetter.Y; // Fake class for convert.
        Instance inst = MeToWeka.convertInstance(e);
        inst.setDataset(MeToWeka.dataForConvert);
        try {
            double[] dist = cls.distributionForInstance(inst);
            return dist[0];
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return 0;
    }
}
