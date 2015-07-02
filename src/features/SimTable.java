package features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import util.Dbg;
import util.MyMath;

import com.google.common.primitives.Doubles;

/**
 * FileName: SimTable.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 22, 2015 10:31:36 PM
 */
public class SimTable {
    public static final String WORD_SEPARATER = ",";

    private HashMap<String, Double> pair2Value = new HashMap<String, Double>();
    // Much faster than ArrayList in removal.
    private List<Pair> pairs = new LinkedList<Pair>();

    public double max = Double.NaN;
    public double min = Double.NaN;

    public void add (String w1, String w2, double sim) {
        // Even though inverseValues() could make some value become infinite,
        // still should not add an infinite value at first place.
        assert !w1.equals(w2) && !Double.isNaN(sim) && !Double.isInfinite(sim);
        String key = getTwoWordsKey(w1, w2);
        assert !pair2Value.containsKey(key);
        pair2Value.put(key, sim);
        pairs.add(new Pair(key, sim));
    }

    public Double getValue (String wordPair) {
        return pair2Value.get(wordPair);
    }

    public Double getValue (String w1, String w2) {
        return pair2Value.get(getTwoWordsKey(w1, w2));
    }

    public boolean contains (String w1, String w2) {
        return pair2Value.containsKey(getTwoWordsKey(w1, w2));
    }

    public int size () {
        assert pair2Value.size() == pairs.size();
        return pairs.size();
    }

    public List<Pair> getPairs () {
        return this.pairs;
    }

    public void sortPairsAscendingly () {
        Collections.sort(pairs);
    }

    public void sortPairDescendingly () {
        Collections.sort(pairs, Collections.reverseOrder());
    }

    public void keepOnlyHighValuePairs () {
        List<Double> vs = new ArrayList<Double>();
        for (Pair p : pairs) {
            // Some pairs on AEMI could be negative. And Jaccard without
            // m-estimate could be 0, which means not related at all.
            if (p.v > 0) {
                vs.add(p.v);
            }
        }
        double[] values = Doubles.toArray(vs);
        double mean = MyMath.getMean(values);
        double dev = MyMath.getStdDev(values);
        double thres = 0;
        int oldSize = pairs.size();
        Iterator<Pair> iter = pairs.iterator();
        while (iter.hasNext()) {
            Pair next = iter.next();
            if (next.v <= thres) {
                // Remove from both table and pairs.
                pair2Value.remove(next.e);
                iter.remove();
            }
        }

        if (Dbg.dbg) {
            System.out.printf("** getHighValuePairs ");
            System.out.printf("threshold: %.4f, mean: %.4f, "
                    + "std dev: %.4f ", thres, mean, dev);
            System.out.printf("number of pairs: %d, out of: %d%n",
                    pairs.size(), oldSize);
        }
    }

    public void inverseValues () {
        for (Pair p : pairs) {
            double v = p.v;
            if (v > 0) {
                v = 1 / v;
            } else {
                v = Double.POSITIVE_INFINITY;
            }
            p.v = v;
            assert pair2Value.containsKey(p.e);
            pair2Value.put(p.e, v);// Replace old value.
        }
    }

    public void findMaxMinValues () {
        max = Double.NEGATIVE_INFINITY;
        min = Double.POSITIVE_INFINITY;
        for (Pair p : pairs) {
            if (max < p.v) {
                max = p.v;
            }
            if (min > p.v) {
                min = p.v;
            }
        }
    }

    public static String getTwoWordsKey (String w1, String w2) {
        if (w1.compareTo(w2) <= 0) {
            return w1 + WORD_SEPARATER + w2;
        } else {
            return w2 + WORD_SEPARATER + w1;
        }
    }

    public static class Pair implements Comparable<Pair> {
        String e;
        double v;
        String w1;
        String w2;

        public Pair(String e, double v) {
            super();
            this.e = e;
            this.v = v;
            String[] str = e.split(WORD_SEPARATER);
            w1 = str[0];
            w2 = str[1];
        }

        public Pair(String w1, String w2, double v) {
            super();
            this.e = getTwoWordsKey(w1, w2);
            this.v = v;
            this.w1 = w1;
            this.w2 = w2;
        }

        static List<Pair> table2Pairs (HashMap<String, Double> simTable) {
            List<Pair> pairs = new ArrayList<Pair>();
            for (Entry<String, Double> entry : simTable.entrySet()) {
                pairs.add(new Pair(entry.getKey(), entry.getValue()));
            }
            return pairs;
        }

        static HashMap<String, Double> pairs2SimTable (List<Pair> pairs) {
            HashMap<String, Double> simTable = new HashMap<String, Double>();
            for (Pair p : pairs) {
                simTable.put(p.e, p.v);
            }
            return simTable;
        }

        @Override
        public int compareTo (Pair o) {
            int result = Double.compare(this.v, o.v);
            if (result == 0) {
                result = this.e.compareTo(o.e);
            }
            return result;
        }

        @Override
        public String toString () {
            return String.format("%s %.3f", e, v);
        }
    }
}
