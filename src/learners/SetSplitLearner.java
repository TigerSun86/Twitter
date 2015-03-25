package learners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import main.ExampleGetter;

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

        ArrayList<HashSet<Integer>> splitNegSets = splitToKSets(majoridxes, t);
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

    /**
     * Return k subsets by splitting idxesIn randomly, each set will have same
     * (as possible) amount of idxes.
     * If k <=1, it will be only 1 subset with all idxes.
     * If k > size of idxesIn, it will still return k subsets, each the first
     * sets have 1 idx and other sets have no idx.
     * */
    private ArrayList<HashSet<Integer>> splitToKSets (List<Integer> idxesIn,
            int k) {
        ArrayList<HashSet<Integer>> ret = new ArrayList<HashSet<Integer>>();
        if (k <= 1) { // Special case.
            HashSet<Integer> set = new HashSet<Integer>();
            set.addAll(idxesIn);
            ret.add(set);
            return ret;
        }

        Random ran = new Random();
        ArrayList<Integer> idxes = new ArrayList<Integer>();
        idxes.addAll(idxesIn);
        int endIdx = idxes.size() - 1; // 0 to endIdx are all available.
        for (int kremain = Math.max(1, k); kremain > 0; kremain--) {
            int size = getSize(endIdx + 1, kremain);
            HashSet<Integer> selected = new HashSet<Integer>();
            while (selected.size() < size) {
                int r = ran.nextInt(endIdx + 1);
                int sel = idxes.get(r);
                selected.add(sel);
                swap(idxes, r, endIdx);
                endIdx--;
            }
            ret.add(selected);
        }
        return ret;
    }

    private int getSize (int idxRemain, int kRemain) {
        int size;
        if (idxRemain > 0) {
            size = (int) Math.round(((double) idxRemain) / kRemain);
            if (size == 0) {
                // if idx =1 but k =3 will lead to size = 0, but the last idx
                // should be used.
                size = 1;
            }
        } else { // There is no idx remain.
            size = 0;
        }
        return size;
    }

    private void swap (ArrayList<Integer> idxes, int a, int b) {
        if (a == b) {
            return;
        }
        assert !(a < 0 || b < 0 || a >= idxes.size() || b >= idxes.size());
        int tmp = idxes.get(a);
        idxes.set(a, idxes.get(b));
        idxes.set(b, tmp);
        return;
    }

}
