package learners;

import java.util.ArrayList;
import java.util.HashSet;

import main.ExampleGetter;
import util.MyMath;

import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExample;
import common.RawExampleList;

/**
 * FileName: SetSplitLearner.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 4, 2015 6:27:13 PM
 */
public class SetSplitLearner implements Learner {
    private long randomSeed = 1;
    private final Learner learner;

    public SetSplitLearner(Learner l) {
        this.learner = l;
    }

    @Override
    public ProbPredictor learn (RawExampleList dataSet, RawAttrList attrs) {
        // Count np and nn.
        int np = 0;
        int nn = 0;
        ArrayList<Integer> posidxes = new ArrayList<Integer>();
        ArrayList<Integer> negidxes = new ArrayList<Integer>();
        for (int i = 0; i < dataSet.size(); i++) {
            if (dataSet.get(i).t.equals(ExampleGetter.Y)) {
                np++;
                posidxes.add(i);
            } else {
                nn++;
                negidxes.add(i);
            }
        }
        String minorClass = (np < nn ? ExampleGetter.Y : ExampleGetter.N);
        ArrayList<Integer> majoridxes = (np < nn ? negidxes : posidxes);
        int maj = Math.max(np, nn);
        int min = Math.min(np, nn);
        int t = (int) Math.floor(((double) maj) / min);

        ArrayList<HashSet<Integer>> splitNegSets =
                MyMath.splitToKSets(majoridxes, t, randomSeed);
        EnsembleHypo hypos = new EnsembleHypo();
        for (int i = 0; i < t; i++) {
            HashSet<Integer> selected = splitNegSets.get(i);
            RawExampleList newset = new RawExampleList();
            for (int idx = 0; idx < dataSet.size(); idx++) {
                RawExample e = dataSet.get(idx);
                if (e.t.equals(minorClass) || selected.contains(idx)) {
                    // pos example or selected neg
                    newset.add(e);
                }
            }
            ProbPredictor h = learner.learn(newset, attrs);
            hypos.add(h);
        }
        return hypos;
    }
}
