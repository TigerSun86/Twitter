package learners;

import java.util.HashSet;

import main.ExampleGetter;
import util.MyMath;

import common.Hypothesis;
import common.Learner;
import common.RawAttrList;
import common.RawExample;
import common.RawExampleList;

/**
 * FileName: SimpleEasyEnsemble.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 4, 2015 6:05:47 PM
 */
public class SimpleEasyEnsemble implements Learner {
    private final Learner learner;
    private final int times;

    public SimpleEasyEnsemble(int t, Learner l) {
        this.times = t;
        this.learner = l;
    }

    @Override
    public Hypothesis learn (RawExampleList dataSet, RawAttrList attrs) {
        // Count np and nn.
        int np = 0;
        int nn = 0;
        for (int i = 0; i < dataSet.size(); i++) {
            if (dataSet.get(i).t.equals(ExampleGetter.Y)) {
                np++;
            } else {
                nn++;
            }
        }
        int t;
        if (nn < (np * 1.5)) {
            t = 1; // Too few pos examples
        } else {
            t = this.times;
        }
        EnsembleHypo hypos = new EnsembleHypo();
        for (int i = 0; i < t; i++) {
            RawExampleList newset = getNewSet(dataSet, np, nn);
            Hypothesis h = learner.learn(newset, attrs);
            hypos.add(h);
        }
        return hypos;
    }

    /** If np >= nn, the new set will contain all neg examples */
    private RawExampleList getNewSet (RawExampleList dataSet, int np, int nn) {
        RawExampleList newset = new RawExampleList();
        int[] ns = MyMath.mOutofN(np, nn);
        HashSet<Integer> selectedIdxes = new HashSet<Integer>();
        for (int idx : ns) {
            selectedIdxes.add(idx);
        }
        int negidx = 0;
        // new set will have same order.
        for (int i = 0; i < dataSet.size(); i++) {
            RawExample e = dataSet.get(i);
            if (e.t.equals(ExampleGetter.Y)) { // Pos example
                newset.add(e);
            } else { // Neg ex
                if (selectedIdxes.contains(negidx)) {
                    newset.add(e);
                }
                negidx++;
            }
        }
        return newset;
    }

}
