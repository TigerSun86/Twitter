package ripperk;

import java.util.ArrayList;
import java.util.Iterator;

import main.ExampleGetter;
import common.ProbPredictor;
import common.RawExample;
import common.RawExampleList;

/**
 * FileName: ProbRuleList.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 25, 2015 12:15:04 AM
 */
public class ProbRuleList extends ProbPredictor {
    RuleList rl;
    public double defPosProb = -1;

    public ProbRuleList(RuleList rl2) {
        rl = rl2;
    }

    @Override
    public double predictPosProb (ArrayList<String> in) {
        double ret = -1;
        for (Rule r : rl) {
            final String prediction = r.rulePredict(in, rl.attrs);
            if (prediction != null) { // Accepted by r.
                ret = r.posProb;
                assert ret != -1;
                break;
            }
        }
        assert defPosProb != -1;
        return (ret != -1) ? ret : defPosProb;
    }

    public void setPosProb (RawExampleList dataSet) {
        // Could have problem when minority class is negative class, each rule
        // is learning positive class, so pos prob for each rule here could
        // still be high, even the default pos prob would be much higher.
        RawExampleList exs = new RawExampleList();
        int npos = 0;
        for (RawExample e : dataSet) {
            exs.add(e);
            if (e.t.equals(ExampleGetter.Y)) {
                npos++;
            }
        }
        double priorProb = ((double) npos) / exs.size();
        for (Rule r : rl) {
            int numOfMatched = 0;
            int numOfPos = 0;
            Iterator<RawExample> iter = exs.iterator();
            while (iter.hasNext()) {
                RawExample e = iter.next();
                final String prediction = r.rulePredict(e.xList, rl.attrs);
                if (prediction != null) { // Accepted by r.
                    numOfMatched++;
                    if (e.t.equals(ExampleGetter.Y)) {
                        numOfPos++;
                    }
                    iter.remove();// Remove example when the rule matched it.
                }
            } // while (iter.hasNext()) {

            // Pos prob by m-estimate.
            double prob = (numOfPos + priorProb) / (numOfMatched + 1);
            r.setPosProb(prob);
        }

        int np2 = 0;
        for (RawExample e : exs) {
            if (e.t.equals(ExampleGetter.Y)) {
                np2++;
            }
        }
        this.defPosProb = ((double) np2) / exs.size();
    }

    @Override
    public String toString () {
        return String.format("%2.2f%% %s", defPosProb, rl.toString());
    }
}
