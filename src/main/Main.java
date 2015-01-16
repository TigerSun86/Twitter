package main;

import java.util.ArrayList;
import java.util.BitSet;

import main.ExampleExtractor.Exs;
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

    public Main(final long authorId) {
        this.author = OReadWriter.getUserDate(authorId);
        this.exGetter = new ExampleExtractor(author);
    }

    private void pairTest () {
        assert author != null;
        assert author.followersIds != null;

        System.out.println("****************");
        System.out
                .println("AuthorName AuthorId FolName FolId #PosTrain "
                        + "#NegTrain #PosTestM1 #NegTestM1 FeatureTime TrainTime "
                        + "TrainAcc TrainPrecision TrainRecall TrainFM TrainAct#Pos TrainPre#Pos "
                        + "TestAcc TestPrecision TestRecall TestFM TestAct#Pos TestPre#Pos");

        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            final Long folId = fols[i];
            final long time1 = SysUtil.getCpuTime();
            final Exs exs = exGetter.getExsOfPairTest(folId);
            if (exs != null) {
                final long time2 = SysUtil.getCpuTime();
                final String s =
                        ModelExecuter.runPairTest(exs.train, exs.testM1,
                                RAW_ATTR);
                final long time3 = SysUtil.getCpuTime();

                System.out.printf("%s %d %s %d %d %s%n",
                        author.userProfile.getScreenName(),
                        author.userProfile.getId(), exs.followerAndExsInfo,
                        time2 - time1, time3 - time2, s);
            }
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");
    }

    private void globalTest () {
        assert author != null;
        assert author.followersIds != null;

        System.out.println("****************");
        System.out
                .println("AuthorName AuthorId FolName FolId #PosTrain "
                        + "#NegTrain #PosTestM1 #NegTestM1 #PosTestM2 #NegTestM2 FeatureTime TrainTime "
                        + "TrainAcc TrainPrecision TrainRecall TrainFM TrainAct#Pos TrainPre#Pos "
                        + "TestAcc TestPrecision TestRecall TestFM TestAct#Pos TestPre#Pos");

        int userCount = 0;
        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            final Long folId = fols[i];
            final long time1 = SysUtil.getCpuTime();
            final Exs exs = exGetter.getExsOfGlobalTest(folId);
            if (exs != null) {
                final long time2 = SysUtil.getCpuTime();
                final String s =
                        ModelExecuter.runGlobalTest(exs.train, exs.testM1,
                                exs.testM2, RAW_ATTR);
                final long time3 = SysUtil.getCpuTime();

                final String[] ret = s.split("-");

                System.out.printf("%s %d %s %d %d %s%n",
                        author.userProfile.getScreenName(),
                        author.userProfile.getId(), exs.followerAndExsInfo,
                        time2 - time1, time3 - time2, ret[0]);

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
        System.out.println("Tweet ErrorRate "
                + "Accuracy Precision Recall FMeasure Actual#Pos Pre#Pos");
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
            final int numActPos = tp + fn;
            final int numPrePos = tp + fp;
            final double errorRate =
                    ((double) Math.abs(numActPos - numPrePos))
                            / (tp + tn + fp + fn);
            final double accuracy = ((double) tp + tn) / (tp + tn + fp + fn);
            final double precision;
            final double recall;
            if (tp == 0) {
                precision = 0;
                recall = 0;
            } else {
                precision = ((double) tp) / (tp + fp);
                recall = ((double) tp) / (tp + fn);
            }

            final double fmeasure;
            if (precision == 0 || recall == 0) {
                fmeasure = 0;
            } else {
                fmeasure = (2 * precision * recall) / (precision + recall);
            }
            System.out.printf("t%d %.4f %.4f %.4f %.4f %.4f %d %d%n", t,
                    errorRate, accuracy, precision, recall, fmeasure,
                    numActPos, numPrePos);
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

    public static void main (String[] args) {
        final OutputRedirection or = new OutputRedirection();

        for (long authorId : DataCollector.AUTHOR_IDS) {
            // if (authorId == 1642106527L) {
            new Main(authorId).pairTest();
            // }
        }

        or.close();
    }
}
