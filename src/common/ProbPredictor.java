package common;

import java.util.ArrayList;

import main.ExampleGetter;

/**
 * FileName: ProbPredictor.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 24, 2015 4:42:11 PM
 */
public abstract class ProbPredictor implements Hypothesis {
    private double threshold = 0.5;

    public void setThreshold (double th) {
        this.threshold = th;
    }

    @Override
    public String predict (final ArrayList<String> attrs) {
        double posProb = this.posProb(attrs);
        if (Double.compare(posProb, threshold) >= 0) {
            return ExampleGetter.Y;
        } else {
            return ExampleGetter.N;
        }
    }

    public abstract double posProb (final ArrayList<String> attrs);
}
