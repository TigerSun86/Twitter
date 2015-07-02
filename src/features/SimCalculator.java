package features;

import java.util.BitSet;
import java.util.HashMap;

import util.Dbg;

import com.google.common.math.DoubleMath;

import features.SimTable.Pair;

/**
 * FileName: SimCalculator.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 18, 2015 8:41:39 PM
 */
public class SimCalculator {
    private static final int LEAST_FREQUENCY = 2;
    private static final boolean M_ESTIMATE = true;
    private static final boolean ONLY_KEEP_VALID_PAIR = true;

    public enum SimMode {
        JACCARD, AEMI, LIFT, DF, SUM, SUM2, IDF, IDF2, AVG
    }

    // Intermediate information for output debug
    public SimTable prescreenTable = null;
    public Pair maxPair = null;

    private SimMode simMode = SimMode.JACCARD;
    private boolean needPrescreen = true;
    private WordStatisDoc doc = null;
    private double totalNumOfRt = 0;

    // Initiate these when every time calculate table.
    private HashMap<String, Double> dfCacheNoRt = null;
    private HashMap<String, Double> dfCacheHasRt = null;
    // A temp variable for getDfaAndb() and getDfaOrb() to avoid allocating
    // memory every time.
    private static final BitSet BS_TEMP = new BitSet();

    public SimCalculator(SimMode simMode, boolean needPrescreen,
            WordStatisDoc doc) {
        super();
        this.simMode = simMode;
        this.needPrescreen = needPrescreen;
        this.doc = doc;
        if (this.doc.para.withRt) {
            this.totalNumOfRt = doc.getRtSum();
        }
    }

    public SimTable getSimTable () {
        SimTable highRelatedPairs = null;
        if (needPrescreen) {
            if (Dbg.dbg) {
                System.out.println("Prescreening section");
            }
            SimMode mBackup = this.simMode;
            boolean rtBackup = this.doc.para.withRt;
            if (simMode != SimMode.AEMI && simMode != SimMode.LIFT
                    && simMode != SimMode.JACCARD) {
                this.simMode = SimMode.AEMI;
            }
            this.doc.para.withRt = false;

            highRelatedPairs = getSimTable(null);
            highRelatedPairs.keepOnlyHighValuePairs();

            this.simMode = mBackup;
            this.doc.para.withRt = rtBackup;
        }
        this.prescreenTable = highRelatedPairs;
        return getSimTable(highRelatedPairs);
    }

    private SimTable getSimTable (SimTable highRelatedPairs) {
        dfCacheNoRt = new HashMap<String, Double>();
        dfCacheHasRt = new HashMap<String, Double>();

        int countOfValidPair = 0;// For debug.
        int maxDf = 0;
        String maxW1 = null;
        String maxW2 = null;

        SimTable table = new SimTable();
        for (int i = 0; i < doc.wordList.size(); i++) {
            String w1 = doc.wordList.get(i);
            for (int j = i; j < doc.wordList.size(); j++) {
                String w2 = doc.wordList.get(j);
                if (!w1.equals(w2)
                        && (highRelatedPairs == null || highRelatedPairs
                                .contains(w1, w2))) {
                    int df = (int) getDfaAndb(w1, w2, false);
                    if (maxDf < df) { // Find max df pair for debug.
                        maxDf = df;
                        maxW1 = w1;
                        maxW2 = w2;
                    }
                    if (df >= LEAST_FREQUENCY) {
                        countOfValidPair++;
                    }
                    if (!ONLY_KEEP_VALID_PAIR || df >= LEAST_FREQUENCY) {
                        // df >= LEAST_FREQUENCY means valid pair.
                        double sim;
                        if (simMode == SimMode.JACCARD) {
                            sim = similarityOfJaccard(w1, w2, doc.para.withRt);
                        } else if (simMode == SimMode.AEMI) {
                            sim = similarityOfAemi(w1, w2, doc.para.withRt);
                        } else if (simMode == SimMode.LIFT) {
                            sim = similarityOfLift(w1, w2, doc.para.withRt);
                        } else if (simMode == SimMode.DF) {
                            sim = simDf(w1, w2);
                        } else if (simMode == SimMode.SUM) {
                            sim = simSum(w1, w2);
                        } else if (simMode == SimMode.IDF) {
                            sim = simIdf(w1, w2);
                        } else if (simMode == SimMode.SUM2) {
                            sim = simSum2(w1, w2);
                        } else if (simMode == SimMode.IDF2) {
                            sim = simIdf2(w1, w2);
                        } else { // if (simMode == SimMode.AVG)
                            sim = simAvg(w1, w2);
                        }
                        table.add(w1, w2, sim);
                    }
                }
            }
        }
        maxPair = new Pair(maxW1, maxW2, maxDf);
        if (Dbg.dbg) {
            // Debug info.
            int to = doc.wordList.size();
            int total = (to * to - to) / 2;
            System.out.printf(
                    "Calculated similarities between %d words, simMode: %s%n",
                    doc.wordList.size(), simMode.toString());
            System.out.printf(
                    "%d word-pairs are valid, %d word-pairs are invalid, "
                            + "the sparsity of upper-triangle is %.2f%%%n",
                    countOfValidPair, total - countOfValidPair,
                    ((total - countOfValidPair) * 100.0) / total);
        }

        return table;
    }

    public double getDfw (String w, boolean rt) {
        HashMap<String, Double> cache = rt ? dfCacheHasRt : dfCacheNoRt;
        if (cache.containsKey(w)) {
            return cache.get(w);
        }
        double df = rt ? doc.getRtSum(w) : doc.getDf(w);
        cache.put(w, df);
        return df;
    }

    public double getDfaAndb (String w1, String w2, boolean rt) {
        BitSet seta = doc.word2DocIds.get(w1);
        BitSet setb = doc.word2DocIds.get(w2);
        BitSet intersection = BS_TEMP;
        intersection.clear();
        intersection.or(seta);
        intersection.and(setb);

        double dfaandb;
        if (!rt) {
            dfaandb = intersection.cardinality();
        } else {
            assert doc.numOfRtOfDocs != null;
            dfaandb = 0; // Sum of number of retweets.
            for (int id = intersection.nextSetBit(0); id >= 0; id =
                    intersection.nextSetBit(id + 1)) {
                dfaandb += doc.numOfRtOfDocs.get(id);
            }
        }
        return dfaandb;
    }

    private double getSumLogRtaAndb (String w1, String w2) {
        BitSet seta = doc.word2DocIds.get(w1);
        BitSet setb = doc.word2DocIds.get(w2);
        BitSet intersection = BS_TEMP;
        intersection.clear();
        intersection.or(seta);
        intersection.and(setb);

        double dfaandb;
        assert doc.logNumOfRtOfDocs != null;
        dfaandb = 0; // Sum of log number of retweets.
        for (int id = intersection.nextSetBit(0); id >= 0; id =
                intersection.nextSetBit(id + 1)) {
            dfaandb += doc.logNumOfRtOfDocs.get(id);
        }
        return dfaandb;
    }

    private double getSumRtaAndb (String w1, String w2) {
        BitSet seta = doc.word2DocIds.get(w1);
        BitSet setb = doc.word2DocIds.get(w2);
        BitSet intersection = BS_TEMP;
        intersection.clear();
        intersection.or(seta);
        intersection.and(setb);

        double dfaandb;
        assert doc.numOfRtOfDocs != null;
        dfaandb = 0; // Sum of number of retweets.
        for (int id = intersection.nextSetBit(0); id >= 0; id =
                intersection.nextSetBit(id + 1)) {
            dfaandb += doc.numOfRtOfDocs.get(id);
        }
        return dfaandb;
    }

    public double getDfaOrb (String w1, String w2, boolean rt) {
        BitSet seta = doc.word2DocIds.get(w1);
        BitSet setb = doc.word2DocIds.get(w2);
        BitSet union = BS_TEMP;
        union.clear();
        union.or(seta);
        union.or(setb);

        double dfaorb;
        if (!rt) {
            dfaorb = union.cardinality();
        } else {
            assert doc.numOfRtOfDocs != null;
            dfaorb = 0; // Sum of number of retweets.
            for (int id = union.nextSetBit(0); id >= 0; id =
                    union.nextSetBit(id + 1)) {
                dfaorb += doc.numOfRtOfDocs.get(id);
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
        if (M_ESTIMATE) {
            double n = rt ? totalNumOfRt : doc.getDf();
            double m = 1 / n;
            jaccard = (dfaandb + m) / (dfaorb + 1);
        } else {
            if (dfaandb == 0) {
                jaccard = 0;
            } else {
                jaccard = dfaandb / dfaorb;
            }
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
        double N = rt ? totalNumOfRt : doc.getDf();
        double da = getDfw(w1, rt);
        double db = getDfw(w2, rt);
        double daandb = getDfaAndb(w1, w2, rt);
        double daorb = getDfaOrb(w1, w2, rt);

        double m;
        double deno;
        if (M_ESTIMATE) {
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
        return aemi;
    }

    /**
     * Lift(a,b) = p(a,b)/(p(a)p(b)) = N*df(a,b)/(df(a)*df(b))
     * In m-estimate Lift(a,b) = (N+1)* (df(a,b)+m)/((df(a)+m )*(df(b)+m))
     */
    private double similarityOfLift (String w1, String w2, boolean rt) {
        double N = rt ? totalNumOfRt : doc.getDf();
        double da = getDfw(w1, rt);
        double db = getDfw(w2, rt);
        double daandb = getDfaAndb(w1, w2, rt);

        double lift;
        if (M_ESTIMATE) {
            double m = 1 / N;
            lift = (N + 1) * (daandb + m) / ((da + m) * (db + m));
        } else {
            if (daandb == 0) {
                lift = 0;
            } else {
                lift = N * daandb / (da * db);
            }
        }
        return lift;
    }

    private double simDf (String w1, String w2) {
        double dfaandb = getDfaAndb(w1, w2, false);
        return dfaandb;
    }

    private double simSum (String w1, String w2) {
        double sumaandb = getSumLogRtaAndb(w1, w2);
        return sumaandb;
    }

    private double simIdf (String w1, String w2) {
        double sumaandb = getSumLogRtaAndb(w1, w2);
        if (sumaandb == 0) {
            return 0;
        }
        double dfaandb = getDfaAndb(w1, w2, false);
        double logD = Math.log(doc.getDf());
        double idf = logD - Math.log(dfaandb);
        return sumaandb * idf;
    }

    private double simSum2 (String w1, String w2) {
        double sumaandb = getSumRtaAndb(w1, w2);
        return sumaandb;
    }

    private double simIdf2 (String w1, String w2) {
        double sumaandb = getSumRtaAndb(w1, w2);
        if (sumaandb == 0) {
            return 0;
        }
        double dfaandb = getDfaAndb(w1, w2, false);
        double logD = Math.log(doc.getDf());
        double idf = logD - Math.log(dfaandb);
        return sumaandb * idf;
    }

    private double simAvg (String w1, String w2) {
        double sumaandb = getSumLogRtaAndb(w1, w2);
        if (sumaandb == 0) {
            return 0;
        }
        double dfaandb = getDfaAndb(w1, w2, false);
        return sumaandb / dfaandb;
    }
}
