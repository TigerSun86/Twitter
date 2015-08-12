package features;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jldadmm.models.GibbsSamplingLDA;
import jldadmm.models.GibbsSamplingLDA.LdaModel;
import jldadmm.utility.CmdArgs;
import twitter4j.Status;
import util.Dbg;
import util.SysUtil;
import features.WordStatisDoc.WordStatisDocSetting;

/**
 * FileName: Lda.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jul 31, 2015 6:46:43 PM
 */
public class Lda2 {
    private static final double WORD_PROB_THRESH = 0.01;

    public static class LdaSetting {
        public WordStatisDocSetting docPara = new WordStatisDocSetting();
        public int numOfCl = 10;
        public int numOfIter = 2000;
    }

    public LdaSetting para = new LdaSetting();

    public List<Map<String, Double>> cluster (List<Status> tweets) {
        List<Map<String, Double>> wordProbsInTopics = null;
        long time = SysUtil.getCpuTime();

        WordStatisDoc doc = new WordStatisDoc(this.para.docPara);
        doc.init(tweets);

        if (Dbg.dbg) {
            System.out.println("**** LDA");
            System.out.printf("Author: %s, numOfCl: %d, numOfIter: %d%n",
                    tweets.get(0).getUser().getScreenName(), para.numOfCl,
                    para.numOfIter);
            System.out.println(para.docPara.toString());
            System.out.println("Size of vocabulary: " + doc.wordList.size());
        }

        try {
            CmdArgs cmdArgs = new CmdArgs();
            cmdArgs.ntopics = para.numOfCl;
            cmdArgs.niters = para.numOfIter;
            cmdArgs.twords = 2000;

            GibbsSamplingLDA lda =
                    new GibbsSamplingLDA(doc.wordsOfDocs, cmdArgs.ntopics,
                            cmdArgs.alpha, cmdArgs.beta, cmdArgs.niters,
                            cmdArgs.twords, cmdArgs.expModelName,
                            cmdArgs.initTopicAssgns, cmdArgs.savestep);
            LdaModel model = lda.inference2();
            if (Dbg.dbg) {
                System.out.println("*****");
                System.out.println("Cluster top 20 entities:");
                List<List<String>> topWordsOfTopics =
                        lda.getTopWordsOfTopics(20);
                for (int i = 0; i < topWordsOfTopics.size(); i++) {
                    System.out.println("Cluster " + i + ":");
                    System.out.println(topWordsOfTopics.get(i).toString());
                }
                System.out.println("Time used: "
                        + (SysUtil.getCpuTime() - time));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        System.exit(0);
        return wordProbsInTopics;
    }
}
