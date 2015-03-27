package main;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
    private static final boolean IS_GLOBAL = true;
    private static final RawAttrList RAW_ATTR = new RawAttrList(
            ModelExecuter.ATTR);

    private static final Learner[] LEARNERS =
            {
                    new DecisionTreeTest(DecisionTreeTest.RP_PRUNE),
                    new DecisionTreeTest(DecisionTreeTest.NO_PRUNE),
                    new DecisionTreeTest(DecisionTreeTest.RP_PRUNE,
                            ID3.SplitCriteria.DKM),
                    new DecisionTreeTest(DecisionTreeTest.NO_PRUNE,
                            ID3.SplitCriteria.DKM),
                    new SimpleEasyEnsemble(5, new DecisionTreeTest(
                            DecisionTreeTest.RP_PRUNE)),
                    new SetSplitLearner(new DecisionTreeTest(
                            DecisionTreeTest.RP_PRUNE)), new RIPPERk(true, 0),
                    new RIPPERk(true, 1) };
    private static final String[] L_NAMES = { "Entropy", "EntropyNoprune",
            "DKM", "DKMnoprune", "Easy", "Split", "Ripper", "RipperOp" };
    // private static final Learner[] LEARNERS = { new AnnLearner2(3, 0.1, 0.1),
    // new AnnLearner2(5, 0.1, 0.1), new AnnLearner2(10, 0.1, 0.1) };
    // private static final String[] L_NAMES = { "Ann3", "Ann5", "Ann10" };

    private static final HashMap<Long, HashSet<Long>> VALID_USERS =
            getValidUsers();

    // It will be initialized at first time usage.
    /** @deprecated */
    private ResultTable resultTable = null;

    private final UserInfo author;
    private final ExampleGetter exGetter;
    private final Database db;
    private final boolean isGlobal;

    public Main(final Database db, final long authorId, final boolean isGlobal) {
        this.author = db.getUser(authorId);
        final List<Status> auTweets =
                db.getOriginalTweetListInTimeRange(authorId,
                        ExampleGetter.TRAIN_START_DATE,
                        ExampleGetter.TEST_END_DATE);
        Collections.sort(auTweets, ExampleGetter.TWEET_SORTER);
        List<Status> auTweetsM2 = null;
        if (isGlobal) {
            auTweetsM2 =
                    db.getOriginalTweetListInTimeRange(authorId,
                            ExampleGetter.TEST_END_DATE,
                            ExampleGetter.TESTM2_END_DATE);
            Collections.sort(auTweetsM2, ExampleGetter.TWEET_SORTER);
        }
        this.exGetter = new ExampleGetter(db, auTweets, auTweetsM2);
        this.db = db;
        this.isGlobal = isGlobal;
    }

    private void test () {
        assert author != null;
        assert author.followersIds != null;

        System.out.println("****************");
        printHeader();

        List<HashMap<Long, List<Double>>> listOfFolToProb =
                new ArrayList<HashMap<Long, List<Double>>>();
        for (int learner = 0; learner < LEARNERS.length; learner++) {
            listOfFolToProb.add(new HashMap<Long, List<Double>>());
        }
        HashMap<Long, Double> folToAvgRt = new HashMap<Long, Double>();
        HashMap<Long, Integer> folToNumOfFs = new HashMap<Long, Integer>();

        final Long[] fols = VALID_USERS.get(author.userId).toArray(new Long[0]);
        for (long folId : fols) {
            final long time1 = SysUtil.getCpuTime();
            final Exs exs = exGetter.getExs(folId, true);
            final long time2 = SysUtil.getCpuTime();

            if (exs != null) {
                double influence = db.getAvgRetweetedCount(folId);
                folToAvgRt.put(folId, influence); // Average RT.
                int numOfFs = db.getUser(folId).userProfile.getFollowersCount();
                folToNumOfFs.put(folId, numOfFs); // Num of followers.

                for (int learner = 0; learner < LEARNERS.length; learner++) {
                    final long time3 = SysUtil.getCpuTime();
                    final String s =
                            new ModelExecuter(LEARNERS[learner]).runTest(
                                    exs.train, exs.testM1, exs.testM2,
                                    RAW_ATTR, isGlobal);
                    final long time4 = SysUtil.getCpuTime();

                    String[] ret = s.split("-");
                    String testResult = ret[0];
                    System.out.printf("%s %s %d %s %d %d %s%n",
                            L_NAMES[learner],
                            author.userProfile.getScreenName(),
                            author.userProfile.getId(), exs.followerAndExsInfo,
                            time2 - time1, time4 - time3, testResult);

                    if (isGlobal) {
                        final String[] predicts = ret[1].split(" ");
                        List<Double> m2Probs = new ArrayList<Double>();
                        for (String prob : predicts) {
                            double p = Double.parseDouble(prob);
                            m2Probs.add(p);
                        }
                        listOfFolToProb.get(learner).put(folId, m2Probs);
                    }
                } // for (int l = 0; l < LEARNERS.length; l++) {
            } // if (exs != null) {
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");
        if (isGlobal) {
            for (int learner = 0; learner < LEARNERS.length; learner++) {
                System.out.println(L_NAMES[learner] + " information");
                HashMap<Long, List<Double>> folToRtProb =
                        listOfFolToProb.get(learner);
                showGlobalInfo(fols, folToRtProb, folToAvgRt, folToNumOfFs);
            }
        }
    }

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

    private void printHeader () {
        System.out
                .println("Learner AuthorName AuthorId FolName FolId #PosTrain "
                        + "#NegTrain #PosTestM1 #NegTestM1 FeatureTime TrainTime "
                        + "TrainAcc TrainPrecision TrainRecall TrainFP TrainFM TrainAct#Pos TrainPre#Pos TrainAuc "
                        + "TestAcc TestPrecision TestRecall TestFP TestFM TestAct#Pos TestPre#Pos TestAuc");
    }

    private void showGlobalInfo (Long[] fols,
            HashMap<Long, List<Double>> folToRtProb,
            HashMap<Long, Double> folToAvgRt,
            HashMap<Long, Integer> folToNumOfFs) {
        List<Double> likelihoodSums = new ArrayList<Double>();
        List<Double> avgRtPred = new ArrayList<Double>();
        List<Double> numOfFsPred = new ArrayList<Double>();

        // Print each followers' id.
        System.out.print("FolIdAndTid");
        for (long folId : fols) {
            System.out.print(" " + folId);
        }
        System.out.println();
        // Print each followers' AvgRt.
        System.out.print("AvgRt");
        for (long folId : fols) {
            System.out.printf(" %.2f", folToAvgRt.get(folId));
        }
        System.out.println();
        // Print each followers' num of followers.
        System.out.print("NumOfFs");
        for (long folId : fols) {
            System.out.printf(" %d", folToNumOfFs.get(folId));
        }
        System.out.println();
        for (int tidx = 0; tidx < exGetter.auTweetsM2.size(); tidx++) {
            // Print author's tweet id.
            System.out.print(exGetter.auTweetsM2.get(tidx).getId());
            double sum = 0;
            double aSum = 0;
            double nSum = 0;
            // Print each followers' retweet likelihood.
            for (long folId : fols) {
                double likelihood = folToRtProb.get(folId).get(tidx);
                sum += likelihood;
                aSum += likelihood * folToAvgRt.get(folId);
                nSum += likelihood * folToNumOfFs.get(folId);
                System.out.printf(" %.2f", likelihood);
            }
            System.out.println();
            likelihoodSums.add(sum);
            avgRtPred.add(aSum);
            numOfFsPred.add(nSum);
        }
        System.out.println("****************");
        // Print result of each tweet.
        System.out
                .println("TweetId actual# likelihoodSum avgRtPred numOfFsPred");
        for (int tidx = 0; tidx < exGetter.auTweetsM2.size(); tidx++) {
            Status t = exGetter.auTweetsM2.get(tidx);
            System.out.printf("%d %d %.2f %.2f %.2f%n", t.getId(),
                    t.getRetweetCount(), likelihoodSums.get(tidx),
                    avgRtPred.get(tidx), numOfFsPred.get(tidx));
        }
        System.out.println("****************");
    }

    /** @deprecated */
    @SuppressWarnings("unused")
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

    /** @deprecated */
    private static class ResultTable {
        public final ArrayList<BitSet> a = new ArrayList<BitSet>();
        public final ArrayList<BitSet> p = new ArrayList<BitSet>();

        @SuppressWarnings("unused")
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
            new Main(db, authorId, IS_GLOBAL).test();

        }
        System.out.println("End at: " + new Date().toString());
        // or.close();
    }
}
