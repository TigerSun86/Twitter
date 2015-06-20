package features;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import util.Dbg;
import util.MyMath;

import com.google.common.math.DoubleMath;

/**
 * FileName: SimCalculator.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 18, 2015 8:41:39 PM
 */
public class SimCalculator {
    public enum Mode {
        JACCARD, AEMI, LIFT, DF, SUM, IDF, AVG
    }

    public static final String WORD_SEPARATER = ",";

    public HashMap<String, Double> pair2Aemi = null;

    private Mode mode = Mode.JACCARD;
    private boolean mEstimate = true;
    private boolean needPrescreen = true;
    private boolean withRt = false;
    private List<String> wordList = null;
    private List<Set<String>> wordSetOfDocs = null;
    private HashMap<String, BitSet> word2DocIds = null;
    private List<Integer> numOfRtOfDocs = null;
    private int totalNumOfRt = 0;

    private int countOfInvalidPair = 0; // For debug.

    public SimCalculator(Mode mode, boolean mEstimate, boolean needPrescreen,
            boolean withRt, List<String> wordList,
            List<Set<String>> wordSetOfDocs,
            HashMap<String, BitSet> word2DocIds, List<Integer> numOfRtOfDocs) {
        super();
        this.mode = mode;
        this.mEstimate = mEstimate;
        this.needPrescreen = needPrescreen;
        this.withRt = withRt;
        this.wordList = wordList;
        this.wordSetOfDocs = wordSetOfDocs;
        this.word2DocIds = word2DocIds;
        this.numOfRtOfDocs = numOfRtOfDocs;
        if (numOfRtOfDocs != null) {
            for (int num : numOfRtOfDocs) {
                totalNumOfRt += num;
            }
        }
        if (needPrescreen) {
            pair2Aemi = new HashMap<String, Double>();
        }
    }

    public HashMap<String, Double> getSimilarityTable () {
        HashMap<String, Double> similarityTableOfTwoWords =
                new HashMap<String, Double>();
        countOfInvalidPair = 0;// For debug.
        for (int i = 0; i < wordList.size(); i++) {
            String w1 = wordList.get(i);
            for (int j = i; j < wordList.size(); j++) {
                String w2 = wordList.get(j);
                if (w1.equals(w2)) {
                    // The distance of a word itself should be 0.
                    // distanceTableOfTwoWords.put(getTwoWordsKey(w1, w2), 0.0);
                } else {
                    if (needPrescreen) {
                        // Mesure AEMI without retweets.
                        double aemi = similarityOfAemi(w1, w2, false);
                        if (aemi <= 0) { // Definitely not a good pair.
                            continue;
                        } else {
                            pair2Aemi.put(getTwoWordsKey(w1, w2), aemi);
                        }
                    }
                    double sim;
                    if (mode == Mode.JACCARD) {
                        sim = similarityOfJaccard(w1, w2, withRt);
                    } else if (mode == Mode.AEMI) {
                        sim = similarityOfAemi(w1, w2, withRt);
                    } else if (mode == Mode.LIFT) {
                        sim = similarityOfLift(w1, w2, withRt);
                    } else if (mode == Mode.DF) {
                        sim = simDf(w1, w2);
                    } else if (mode == Mode.SUM) {
                        sim = simSum(w1, w2);
                    } else if (mode == Mode.IDF) {
                        sim = simIdf(w1, w2);
                    } else { // if (mode == Mode.AVG)
                        sim = simAvg(w1, w2);
                    }

                    assert !Double.isNaN(sim) && !Double.isInfinite(sim);
                    similarityTableOfTwoWords.put(getTwoWordsKey(w1, w2), sim);
                }
            }
        }
        if (Dbg.dbg) {
            // Debug info.
            int to = wordList.size();
            int total = (to * to - to) / 2;
            System.out.printf("Calculated similarities between %d words%n",
                    wordList.size());
            System.out.printf(
                    "%d word-pairs are valid, %d word-pairs are invalid, "
                            + "the sparsity of upper-triangle is %.2f%%%n",
                    total - countOfInvalidPair, countOfInvalidPair,
                    ((double) countOfInvalidPair * 100.0) / total);
        }

        if (needPrescreen) {
            List<Pair> aemiPairs = Pair.getPairs(pair2Aemi);
            double[] values = new double[aemiPairs.size()];
            for (int i = 0; i < aemiPairs.size(); i++) {
                values[i] = aemiPairs.get(i).v;
            }
            double mean = MyMath.getMean(values);
            double dev = MyMath.getStdDev(values);
            //double thres = mean + dev;
            double thres = 0;
            List<Pair> filteredPairs = new ArrayList<Pair>();
            for (Pair p : aemiPairs) {
                if (p.v > thres) {
                    Pair realPair =
                            new Pair(p.e, similarityTableOfTwoWords.get(p.e));
                    filteredPairs.add(realPair);
                }
            }
            if (Dbg.dbg) {
                System.out.println("Prescreening section");
                System.out.printf("AEMI threshold: %.4f, mean: %.4f, "
                        + "std dev: %.4f%n", thres, mean, dev);
                System.out.printf("Number of pairs: %d, out of: %d%n",
                        filteredPairs.size(), aemiPairs.size());
            }
            similarityTableOfTwoWords = new HashMap<String, Double>();
            for (Pair p : filteredPairs) {
                similarityTableOfTwoWords.put(p.e, p.v);
            }
        }
        return similarityTableOfTwoWords;
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

        public static List<Pair> getAscendingPairs (
                HashMap<String, Double> simTable) {
            // Sort all pairs.
            List<Pair> sortedPairs = getPairs(simTable);
            Collections.sort(sortedPairs);
            return sortedPairs;
        }

        public static List<Pair> getDescendingPairs (
                HashMap<String, Double> simTable) {
            // Sort all pairs.
            List<Pair> sortedPairs = getPairs(simTable);
            Collections.sort(sortedPairs, Collections.reverseOrder());
            return sortedPairs;
        }

        static List<Pair> getPairs (HashMap<String, Double> simTable) {
            List<Pair> pairs = new ArrayList<Pair>();
            for (Entry<String, Double> entry : simTable.entrySet()) {
                pairs.add(new Pair(entry.getKey(), entry.getValue()));
            }
            return pairs;
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

    public static String getTwoWordsKey (String w1, String w2) {
        if (w1.compareTo(w2) <= 0) {
            return w1 + WORD_SEPARATER + w2;
        } else {
            return w2 + WORD_SEPARATER + w1;
        }
    }

    private HashMap<String, Integer> dfCacheNoRt =
            new HashMap<String, Integer>();
    private HashMap<String, Integer> dfCacheHasRt =
            new HashMap<String, Integer>();

    public int getDfw (String w, boolean rt) {
        HashMap<String, Integer> cache = rt ? dfCacheHasRt : dfCacheNoRt;
        if (cache.containsKey(w)) {
            return cache.get(w);
        }
        BitSet set = word2DocIds.get(w);
        int df;
        if (!rt) {
            df = set.cardinality();
        } else {
            assert numOfRtOfDocs != null;
            df = 0; // Sum of number of retweets.
            for (int id = set.nextSetBit(0); id >= 0; id =
                    set.nextSetBit(id + 1)) {
                df += numOfRtOfDocs.get(id);
            }
        }
        cache.put(w, df);
        return df;
    }

    public int getDfaAndb (String w1, String w2, boolean rt) {
        BitSet seta = word2DocIds.get(w1);
        BitSet setb = word2DocIds.get(w2);
        BitSet intersection = (BitSet) seta.clone();
        intersection.and(setb);
        int dfaandb;
        if (!rt) {
            dfaandb = intersection.cardinality();
        } else {
            assert numOfRtOfDocs != null;
            dfaandb = 0; // Sum of number of retweets.
            for (int id = intersection.nextSetBit(0); id >= 0; id =
                    intersection.nextSetBit(id + 1)) {
                dfaandb += numOfRtOfDocs.get(id);
            }
        }
        return dfaandb;
    }

    public int getDfaOrb (String w1, String w2, boolean rt) {
        BitSet seta = word2DocIds.get(w1);
        BitSet setb = word2DocIds.get(w2);
        BitSet union = (BitSet) seta.clone();
        union.or(setb);
        int dfaorb;
        if (!rt) {
            dfaorb = union.cardinality();
        } else {
            assert numOfRtOfDocs != null;
            dfaorb = 0; // Sum of number of retweets.
            for (int id = union.nextSetBit(0); id >= 0; id =
                    union.nextSetBit(id + 1)) {
                dfaorb += numOfRtOfDocs.get(id);
            }
        }
        return dfaorb;
    }

    /**
     * Jaccard(a,b) = df(a and b)/ df(a or b)
     * In m-estimate Jaccard(a,b) = (df(a and b) + m)/ (df(a or b)+1)
     */
    private double similarityOfJaccard (String w1, String w2, boolean rt) {
        double dfaandb = getDfaAndb(w1, w2, rt);
        double dfaorb = getDfaOrb(w1, w2, rt);
        double jaccard;
        if (mEstimate) {
            double n = rt ? totalNumOfRt : wordSetOfDocs.size();
            double m = 1 / n;
            jaccard = (dfaandb + m) / (dfaorb + 1);
        } else {
            if (dfaandb == 0) {
                jaccard = 0;
            } else {
                jaccard = dfaandb / dfaorb;
            }
        }

        if (dfaandb == 0) {
            countOfInvalidPair++;
        }
        return jaccard;
    }

    private static final boolean NEED_PART2 = false;;

    /**
     * Aemi(a,b) = p(a,b)*log(p(a,b)/(p(a)*p(b)))
     * +p(na,nb)*log(p(na,nb)/(p(na)*p(nb)))
     * -p(a,nb)*log(p(a,nb)/(p(a)*p(nb)))
     * -p(na,b)*log(p(na,b)/(p(na)*p(b)))
     * where
     * p(a) = d(a)/N
     * p(b) = d(b)/N
     * p(na) = (N-d(a))/N
     * p(nb) = (N-d(b))/N
     * p(a,b) = d(a and b)/N
     * p(na,nb) = (N-d(a or b))/N
     * p(na,b) = (d(b) - d(a and b))/N
     * p(a,nb) = (d(a) - d(a and b))/N
     * m-estimate p(a) will be (d(a)+m)/(N+1)
     */

    public double similarityOfAemi (String w1, String w2, boolean rt) {
        double N = rt ? totalNumOfRt : wordSetOfDocs.size();
        double da = getDfw(w1, rt);
        double db = getDfw(w2, rt);
        double daandb = getDfaAndb(w1, w2, rt);
        double daorb = getDfaOrb(w1, w2, rt);

        double m;
        double deno;
        if (mEstimate) {
            m = 1 / N;
            deno = N + 1;
        } else {
            m = 0;
            deno = N;
        }
        /* p(a) = d(a)/N
         * p(b) = d(b)/N
         * p(na) = (N-d(a))/N
         * p(nb) = (N-d(b))/N
         * p(a,b) = d(a and b)/N
         * p(na,nb) = (N-d(a or b))/N
         * p(na,b) = (d(b) - d(a and b))/N
         * p(a,nb) = (d(a) - d(a and b))/N */
        double pa = (da + m) / deno;
        double pb = (db + m) / deno;
        double pna = (N - da + m) / deno;
        double pnb = (N - db + m) / deno;
        double pab = (daandb + m) / deno;
        double pnanb = (N - daorb + m) / deno;
        double pnab = (db - daandb + m) / deno;
        double panb = (da - daandb + m) / deno;

        /* Aemi(a,b) = p(a,b)*log(p(a,b)/(p(a)*p(b)))
         * +p(na,nb)*log(p(na,nb)/(p(na)*p(nb)))
         * -p(a,nb)*log(p(a,nb)/(p(a)*p(nb)))
         * -p(na,b)*log(p(na,b)/(p(na)*p(b))) */
        double part1 =
                (pab == 0) ? 0 : (pab * DoubleMath.log2(pab / (pa * pb)));
        double part2 = 0;
        if (NEED_PART2) {
            part2 =
                    (pnanb == 0) ? 0 : (pnanb * DoubleMath.log2(pnanb
                            / (pna * pnb)));
        }

        double part3 =
                (panb == 0) ? 0 : (panb * DoubleMath.log2(panb / (pa * pnb)));
        double part4 =
                (pnab == 0) ? 0 : (pnab * DoubleMath.log2(pnab / (pna * pb)));
        double aemi = part1 + part2 - part3 - part4;
        if (daandb == 0) {
            countOfInvalidPair++;
        }
        return aemi;
    }

    /**
     * Lift(a,b) = p(a,b)/(p(a)p(b)) = N*df(a,b)/(df(a)*df(b))
     * In m-estimate Lift(a,b) = (N+1)* (df(a,b)+m)/((df(a)+m )*(df(b)+m))
     */
    private double similarityOfLift (String w1, String w2, boolean rt) {
        double N = rt ? totalNumOfRt : wordSetOfDocs.size();
        double da = getDfw(w1, rt);
        double db = getDfw(w2, rt);
        double daandb = getDfaAndb(w1, w2, rt);

        double lift;
        if (mEstimate) {
            double m = 1 / N;
            lift = (N + 1) * (daandb + m) / ((da + m) * (db + m));
        } else {
            if (daandb == 0) {
                lift = 0;
            } else {
                lift = N * daandb / (da * db);
            }
        }

        if (daandb == 0) {
            countOfInvalidPair++;
        }
        return lift;
    }

    private double simDf (String w1, String w2) {
        double dfaandb = getDfaAndb(w1, w2, false);
        return dfaandb;
    }

    private double simSum (String w1, String w2) {
        assert numOfRtOfDocs != null;
        double sumaandb = getDfaAndb(w1, w2, true);
        return sumaandb;
    }

    private double simIdf (String w1, String w2) {
        assert numOfRtOfDocs != null;
        double sumaandb = getDfaAndb(w1, w2, true);
        if (sumaandb == 0) {
            return 0;
        }
        double dfaandb = getDfaAndb(w1, w2, false);
        double logD = Math.log(wordSetOfDocs.size());
        double idf = logD - Math.log(dfaandb);
        return sumaandb * idf;
    }

    private double simAvg (String w1, String w2) {
        assert numOfRtOfDocs != null;
        double sumaandb = getDfaAndb(w1, w2, true);
        if (sumaandb == 0) {
            return 0;
        }
        double dfaandb = getDfaAndb(w1, w2, false);
        return sumaandb / dfaandb;
    }
}
