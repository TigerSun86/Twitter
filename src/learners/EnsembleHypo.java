package learners;

import java.util.ArrayList;
import java.util.List;

import util.Dbg;
import common.ProbPredictor;

/**
 * FileName: EnsembleHypo.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 4, 2015 5:59:42 PM
 */
public class EnsembleHypo extends ProbPredictor {
    List<ProbPredictor> hypos = new ArrayList<ProbPredictor>();

    public void add (ProbPredictor h) {
        hypos.add(h);
    }

    @Override
    public double predictPosProb (ArrayList<String> attrs) {
        double posProb = 0;
        for (ProbPredictor h : hypos) {
            posProb += h.predictPosProb(attrs);
        }
        posProb /= hypos.size();
        return posProb;
    }

    @Override
    public String toString () {
        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= hypos.size(); i++) {
            sb.append("Predictor " + i + Dbg.NEW_LINE);
            sb.append(hypos.get(i - 1).toString());
            sb.append(Dbg.NEW_LINE);
        }
        return sb.toString();
    }
}
