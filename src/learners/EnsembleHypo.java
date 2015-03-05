package learners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import common.Hypothesis;

/**
 * FileName: EnsembleHypo.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 4, 2015 5:59:42 PM
 */
public class EnsembleHypo implements Hypothesis {
    List<Hypothesis> hypos = new ArrayList<Hypothesis>();

    public void add (Hypothesis h) {
        hypos.add(h);
    }

    @Override
    public String predict (ArrayList<String> attrs) {
        HashMap<String, Integer> counts = new HashMap<String, Integer>();
        for (Hypothesis h : hypos) { // Vote
            String p = h.predict(attrs);
            if (counts.containsKey(p)) {
                counts.put(p, counts.get(p) + 1);
            } else {
                counts.put(p, 1);
            }
        }
        String maxp = null;
        int maxc = 0;
        for (Entry<String, Integer> en : counts.entrySet()) {
            if (maxc < en.getValue()) { // If tie, pick first.
                maxp = en.getKey();
                maxc = en.getValue();
            }
        }
        assert maxp != null;
        return maxp;
    }

}
