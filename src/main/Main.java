package main;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import learners.SetSplitLearner;
import learners.SimpleEasyEnsemble;
import main.ExampleGetter.Exs;
import ripperk.RIPPERk;
import twitter4j.Status;
import util.SysUtil;
import common.DataReader;
import common.Learner;
import common.RawAttrList;
import datacollection.Database;
import datacollection.UserInfo;
import decisiontreelearning.DecisionTree.DecisionTreeTest;
import decisiontreelearning.DecisionTree.ID3;

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

    private static final Learner[] LEARNERS =
            {
                    new DecisionTreeTest(DecisionTreeTest.RP_PRUNE),
                    new DecisionTreeTest(DecisionTreeTest.RP_PRUNE,
                            ID3.SplitCriteria.DKM),
                    new DecisionTreeTest(DecisionTreeTest.NO_PRUNE,
                            ID3.SplitCriteria.DKM),
                    new SimpleEasyEnsemble(5, new DecisionTreeTest(
                            DecisionTreeTest.RP_PRUNE)),
                    new SetSplitLearner(new DecisionTreeTest(
                            DecisionTreeTest.RP_PRUNE)), new RIPPERk(true, 0),
                    new RIPPERk(true, 1) };
    private static final String[] L_NAMES = { "DecisionTree", "DKM",
            "DKMnoprune", "Easy", "Split", "Ripper", "RipperOp" };

    private static final HashMap<Long, HashSet<Long>> VALID_USERS =
            getValidUsers();

    private static HashMap<Long, HashSet<Long>> getValidUsers () {
        String fileName =
                "file://localhost/C:/WorkSpace/Twitter/data/validUsers.txt";
        final DataReader in = new DataReader(fileName);
        HashMap<Long, HashSet<Long>> map = new HashMap<Long, HashSet<Long>>();
        while (true) {
            final String str = in.nextLine();
            if (str == null) {
                break;
            }
            if (!str.isEmpty() && Character.isDigit(str.charAt(0))) {
                String[] s = str.split(" ");
                long au = Long.parseLong(s[0]);
                long fol = Long.parseLong(s[1]);
                long pos = Long.parseLong(s[2]);
                if (pos >= ExampleGetter.LEAST_POS_NUM) {
                    if (map.containsKey(au)) {
                        map.get(au).add(fol);
                    } else {
                        HashSet<Long> set = new HashSet<Long>();
                        set.add(fol);
                        map.put(au, set);
                    }
                }
            }
        } // End of while (true) {
        in.close();
        return map;
    }

    // It will be initialized at first time usage.
    private ResultTable resultTable = null;

    private final UserInfo author;
    private final ExampleGetter exGetter;

    public Main(final Database db, final long authorId) {
        this.author = db.getUser(authorId);
        final List<Status> auTweets =
                db.getOriginalTweetListInTimeRange(authorId,
                        ExampleGetter.TRAIN_START_DATE,
                        ExampleGetter.TEST_END_DATE);
        Collections.sort(auTweets, ExampleGetter.TWEET_SORTER);
        this.exGetter = new ExampleGetter(db, auTweets);
    }

    private void pairTest () {
        assert author != null;
        assert author.followersIds != null;

        System.out.println("****************");
        System.out
                .println("Learner AuthorName AuthorId FolName FolId #PosTrain "
                        + "#NegTrain #PosTestM1 #NegTestM1 FeatureTime TrainTime "
                        + "TrainAcc TrainPrecision TrainRecall TrainFP TrainFM TrainAct#Pos TrainPre#Pos TrainAuc"
                        + "TestAcc TestPrecision TestRecall TestFP TestFM TestAct#Pos TestPre#Pos TestAuc");
        final Set<Long> fols = VALID_USERS.get(author.userId);
        for (long folId : fols) {
            final long time1 = SysUtil.getCpuTime();
            final Exs exs = exGetter.getExsOfPairTest(folId);
            final long time2 = SysUtil.getCpuTime();

            if (exs != null) {
                for (int l = 0; l < LEARNERS.length; l++) {
                    final long time3 = SysUtil.getCpuTime();
                    final String s =
                            new ModelExecuter(LEARNERS[l]).runPairTest2(
                                    exs.train, exs.testM1, RAW_ATTR);
                    final long time4 = SysUtil.getCpuTime();

                    System.out.printf("%s %s %d %s %d %d %s%n", L_NAMES[l],
                            author.userProfile.getScreenName(),
                            author.userProfile.getId(), exs.followerAndExsInfo,
                            time2 - time1, time4 - time3, s);
                }
            }
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");
    }

    @SuppressWarnings("unused")
    private void globalTest () {
        assert author != null;
        assert author.followersIds != null;

        System.out.println("****************");
        System.out
                .println("AuthorName AuthorId FolName FolId #PosTrain "
                        + "#NegTrain #PosTestM1 #NegTestM1 #PosTestM2 #NegTestM2 FeatureTime TrainTime "
                        + "TrainAcc TrainPrecision TrainRecall TrainFP TrainFM TrainAct#Pos TrainPre#Pos "
                        + "TestAcc TestPrecision TestRecall TestFP TestFM TestAct#Pos TestPre#Pos");

        int userCount = 0;
        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            final Long folId = fols[i];
            final long time1 = SysUtil.getCpuTime();
            final Exs exs = null;// exGetter.getExsOfGlobalTest(folId);
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
                            (actuals[t].equals(ExampleGetter.Y)));
                    resultTable.p.get(t).set(userCount,
                            (predicts[t].equals(ExampleGetter.Y)));
                }
                userCount++;
            }
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");

        showFMeasure(userCount);
    }

    private void showFMeasure (int userCount) {
        System.out.println("Tweet ErrorRate "
                + "Accuracy Precision Recall FP FMeasure Actual#Pos Pre#Pos");
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
            final double falsePositive;
            if (fp == 0) {
                falsePositive = 0;
            } else {
                falsePositive = ((double) fp) / (fp + tn);
            }
            final double fmeasure;
            if (precision == 0 || recall == 0) {
                fmeasure = 0;
            } else {
                fmeasure = (2 * precision * recall) / (precision + recall);
            }
            System.out.printf("t%d %.4f %.4f %.4f %.4f %.4f %.4f %d %d%n", t,
                    errorRate, accuracy, precision, recall, falsePositive,
                    fmeasure, numActPos, numPrePos);
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
        // final OutputRedirection or = new OutputRedirection();
        System.out.println("Begin at: " + new Date().toString());
        final Database db = Database.getInstance();
        for (long authorId : VALID_USERS.keySet()) {
            new Main(db, authorId).pairTest();

        }
        System.out.println("End at: " + new Date().toString());
        // or.close();
    }
}
