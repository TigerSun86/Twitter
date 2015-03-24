package common;

import java.util.ArrayList;
import java.util.Collections;

import main.ExampleGetter;

/**
 * FileName: AucCalculator.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 24, 2015 3:45:37 PM
 */
public class AucCalculator {

    public static double calAuc (ProbPredictor h, final RawExampleList dataSet) {
        ArrayList<ExAndProb> predictAns = new ArrayList<ExAndProb>();
        int actp = 0;
        int actn = 0;
        for (RawExample e : dataSet) {
            double prob = h.posProb(e.xList);
            ExAndProb ans = new ExAndProb(e.t, prob);
            predictAns.add(ans);
            if (e.t.equals(ExampleGetter.Y)) {
                actp++;
            } else {
                actn++;
            }
        }
        if (actp == 0 || actn == 0) {
            // No pos or neg examples.
            return Double.NaN;
        }

        // Sort by probability of pos from the highest to lowest.
        Collections.sort(predictAns, Collections.reverseOrder());
        // TP and FP count.
        int tp = 0;
        int fp = 0;
        // Last TPR and FPR.
        double ltpr = 0;
        double lfpr = 0;
        double auc = 0;
        // Decrease the prob threshold by each example.
        for (ExAndProb eap : predictAns) {
            if (eap.actClass.equals(ExampleGetter.Y)) {
                tp++; // This example is a TP.
            } else { // Is FP.
                fp++;
            }
            double tpr = ((double) tp) / actp;
            double fpr = ((double) fp) / actn;
            auc += (tpr + ltpr) * (fpr - lfpr) / 2;
            ltpr = tpr;
            lfpr = fpr;
            // The first auc the ltpr is 0, so trapezoid equation will become
            // triangle area equation.
            // It won't hurt if there are two neg ex have same pos prob, because
            // the fpr will equal to lfpr, so new auc will be 0, but ltpr will
            // be changed, so next time when fpr changes the new auc calculation
            // still correct.
        }
        return auc;
    }

    private static class ExAndProb implements Comparable<ExAndProb> {
        String actClass;
        double posProb;

        public ExAndProb(String cl, double p) {
            actClass = cl;
            posProb = p;
        }

        @Override
        public int compareTo (ExAndProb o) {
            return Double.compare(this.posProb, o.posProb);
        }
    }
}
