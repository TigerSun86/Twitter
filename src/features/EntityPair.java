package features;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Status;
import util.Dbg;
import features.SimCalculator.SimMode;
import features.SimTable.Pair;
import features.WordStatisDoc.WordStatisDocSetting;

/**
 * FileName: EntityPair.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 18, 2015 7:37:31 PM
 */
public class EntityPair {
    public static class EntityPairSetting {
        public WordStatisDocSetting docPara = new WordStatisDocSetting();
        public SimMode simMode = SimMode.AEMI;
        public boolean needPrescreen = true;
        public int numOfPairs = 10;
    }

    public EntityPairSetting para = new EntityPairSetting();

    public List<Pair> getTopEntityPairs (List<Status> tweets) {
        WordStatisDoc doc = new WordStatisDoc(this.para.docPara);
        doc.init(tweets);

        SimCalculator simCal =
                new SimCalculator(para.simMode, para.needPrescreen, doc);
        SimTable simTable = simCal.getSimTable();
        simTable.sortPairDescendingly();

        final int numPairs;
        if (para.numOfPairs <= 0) {
            assert para.simMode == SimMode.AEMI;
            simTable.keepOnlyHighValuePairs();
            numPairs = simTable.size();
        } else {
            numPairs = Math.min(para.numOfPairs, simTable.size());
        }
        List<Pair> topPairs = new ArrayList<Pair>();
        for (int i = 0; i < numPairs; i++) {
            topPairs.add(simTable.getPairs().get(i));
        }

        if (Dbg.dbg) {
            System.out.println("**** EntityPair ****");
            System.out.printf("Author: %s, number of tweets: %d, "
                    + "number of top pairs: %d, simMode: %s, "
                    + "needPrescreen: %b, most frequent pair: %s%n", tweets
                    .get(0).getUser().getScreenName(), tweets.size(),
                    topPairs.size(), para.simMode.toString(),
                    para.needPrescreen, simCal.maxPair.toString());
            System.out.println(para.docPara.toString());
            System.out.println("Top pairs:");
            for (Pair p : topPairs) {
                String w1 = p.w1;
                String w2 = p.w2;
                System.out.printf("%s, Df: %.0f, %s_Df: %.0f, %s_Df: %.0f",
                        p.toString(), simCal.getDfaAndb(w1, w2, false), w1,
                        simCal.getDfw(w1, false), w2, simCal.getDfw(w2, false));
                if (para.docPara.withRt) {
                    System.out
                            .printf(", PairNumOfRt: %.2f, %s_NumOfRt: %.2f, %s_NumOfRt: %.2f",
                                    simCal.getDfaAndb(w1, w2, true), w1,
                                    simCal.getDfw(w1, true), w2,
                                    simCal.getDfw(w2, true));
                }
                if (para.needPrescreen) {
                    System.out.printf(", PrescreenValue: %.3f",
                            simCal.prescreenTable.getValue(p.e));
                }
                System.out.println();
            }
        }
        return topPairs;
    }
}
