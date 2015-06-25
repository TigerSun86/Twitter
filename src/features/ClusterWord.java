package features;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import twitter4j.Status;
import util.Dbg;
import util.SysUtil;
import features.SimCalculator.SimMode;
import features.WordStatisDoc.WordStatisDocSetting;

/**
 * FileName: ClusterWord.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date May 21, 2015 5:52:30 PM
 */
public class ClusterWord {
    public static class ClusterWordSetting {
        public WordStatisDocSetting docPara = new WordStatisDocSetting();
        public SimMode simMode = SimMode.AEMI;
        public boolean needPrescreen = true;
        public ClAlg clAlg = new SingleCutAlg(10, false);
    }

    public ClusterWordSetting para = new ClusterWordSetting();
    public int numOfCl = -1; // output.

    public HashMap<String, Integer> clusterWords (List<Status> tweets) {
        long time = SysUtil.getCpuTime();
        if (Dbg.dbg) {
            System.out.println("**** ClusterWord");
            System.out.printf(
                    "Author: %s, simMode: %s, needPrescreen: %b, clAlg: %s%n",
                    tweets.get(0).getUser().getScreenName(), para.simMode
                            .toString(), para.needPrescreen, para.clAlg
                            .getClass().getSimpleName());
            System.out.println(para.docPara.toString());
        }
        WordStatisDoc doc = new WordStatisDoc(this.para.docPara);
        doc.init(tweets);

        SimCalculator simCal =
                new SimCalculator(para.simMode, para.needPrescreen, doc);

        SimTable simTable = simCal.getSimTable();
        List<Set<String>> clusters = para.clAlg.cluster(simTable, doc.wordList);

        HashMap<String, Integer> word2Cl = new HashMap<String, Integer>();
        for (int cid = 0; cid < clusters.size(); cid++) {
            Set<String> cl = clusters.get(cid);
            for (String w : cl) {
                word2Cl.put(w, cid);
            }
        }
        // Important! don't forget tell upper level how many clusters have.
        this.numOfCl = clusters.size();
        if (Dbg.dbg) {
            System.out.println("Time used: " + (SysUtil.getCpuTime() - time));
        }
        return word2Cl;
    }

    public interface ClAlg {
        public List<Set<String>> cluster (SimTable simTable,
                List<String> wordList);
    }
}
