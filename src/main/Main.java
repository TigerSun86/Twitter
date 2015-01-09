package main;

import java.util.ArrayList;
import java.util.BitSet;

import main.ExampleExtractor.ExsOfGlobalTest;
import test.DataCollector;
import test.UserData;
import util.OReadWriter;
import util.OutputRedirection;
import util.SysUtil;

import common.RawAttrList;

/**
 * FileName: Main.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 9, 2015 2:33:10 PM
 */
public class Main {
    private static final RawAttrList RAW_ATTR = new RawAttrList(
            ModelExecuter.ATTR);

    // It will be initialized at first time usage.
    private ResultTable resultTable = null;
    private final UserData author;
    private final ExampleExtractor exGetter;

    public static void main (String[] args) {
        final OutputRedirection or = new OutputRedirection();

        for (long authorId : DataCollector.AUTHOR_IDS) {
            // if (authorId == 497178013L) {
            new Main(authorId).globalTest();
            // }
        }

        or.close();
    }

    private void globalTest () {
        assert author != null;
        assert author.followersIds != null;

        System.out.println("****************");
        System.out.println("AuthorName AuthorId FolName FolId #PosTrain "
                + "#NegTrain #PosTestM1 #NegTestM1 #PosTestM2 #NegTestM2 "
                + "TrainAcc TestAcc FeatureTime TrainTime");

        int userCount = 0;
        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            final Long folId = fols[i];
            final long time1 = SysUtil.getCpuTime();
            final ExsOfGlobalTest exs = exGetter.getExsOfGlobalTest(folId);
            if (exs != null) {
                final long time2 = SysUtil.getCpuTime();
                final String s =
                        ModelExecuter.runGlobalTest(exs.train, exs.testM1, exs.testM2,
                                RAW_ATTR);
                final long time3 = SysUtil.getCpuTime();

                final String[] ret = s.split("-");

                System.out.printf("%s %d %s %s %d %d%n",
                        author.userProfile.getScreenName(),
                        author.userProfile.getId(), exs.followerAndExsInfo,
                        ret[0], time2 - time1, time3 - time2);

                final String[] actuals = ret[1].split(" ");
                final String[] predicts = ret[2].split(" ");

                if (this.resultTable == null) { // Initialize resultTable;
                    this.resultTable = new ResultTable(actuals.length);
                }

                for (int t = 0; t < actuals.length; t++) {
                    resultTable.a.get(t).set(userCount,
                            (actuals[t].equals(ExampleExtractor.Y)));
                    resultTable.p.get(t).set(userCount,
                            (predicts[t].equals(ExampleExtractor.Y)));
                }
                userCount++;
            }
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");

        showFMeasure(userCount);
    }

    private void showFMeasure (int userCount) {
        System.out.println("Tweet Actual#Ret Pre#Ret ErrorRate "
                + "Accuracy Precision Recall FMeasure");
        for (int t = 0; t < resultTable.a.size(); t++) {
            final BitSet arow = resultTable.a.get(t);
            final BitSet prow = resultTable.p.get(t);
            int tp = 0;
            int tn = 0;
            int fp = 0;
            int fn = 0;
            for (int f = 0; f < userCount; f++) {
                final boolean a = arow.get(f);
                final boolean p = prow.get(f);
                if (a && p) {
                    tp++;
                } else if (a && !p) {
                    fn++;
                } else if (!a && p) {
                    fp++;
                } else {// !a && !p
                    tn++;
                }
            }
            final int actualRet = tp + fn;
            final int preRet = tp + fp;
            final double errorRate =
                    ((double) Math.abs(actualRet - preRet))
                            / (tp + tn + fp + fn);
            final double accuracy = ((double) tp + tn) / (tp + tn + fp + fn);
            final double precision = ((double) tp) / (tp + fp);
            final double recall = ((double) tp) / (tp + fn);
            final double fmeasure =
                    (2 * precision * recall) / (precision + recall);
            System.out.printf("t%d %d %d %.4f %.4f %.4f %.4f %.4f%n", t,
                    actualRet, preRet, errorRate, accuracy, precision, recall,
                    fmeasure);
        }
    }

    private static class ResultTable {
        public final ArrayList<BitSet> a = new ArrayList<BitSet>();
        public final ArrayList<BitSet> p = new ArrayList<BitSet>();

        public ResultTable(int numT) {
            for (int i = 0; i < numT; i++) {
                a.add(new BitSet());
                p.add(new BitSet());
            }
        }
    }

    public Main(final long authorId) {
        this.author = OReadWriter.getUserDate(authorId);
        this.exGetter = new ExampleExtractor(author);
    }
}
