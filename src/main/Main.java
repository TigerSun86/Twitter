package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import learners.MeToWeka;
import learners.RandomForest;
import learners.WekaDt;
import main.ExampleGetter.Exs;
import twitter4j.Status;
import util.MyMath;
import util.SysUtil;
import weka.core.Instance;
import weka.core.Instances;

import common.DataReader;
import common.Learner;
import common.RawAttrList;

import datacollection.Database;
import datacollection.UserInfo;
import features.FeatureExtractor;

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

    // private static final Learner[] LEARNERS =
    // {
    // new DecisionTreeTest(DecisionTreeTest.RP_PRUNE),
    // new DecisionTreeTest(DecisionTreeTest.NO_PRUNE),
    // new DecisionTreeTest(DecisionTreeTest.RP_PRUNE,
    // ID3.SplitCriteria.DKM),
    // new DecisionTreeTest(DecisionTreeTest.NO_PRUNE,
    // ID3.SplitCriteria.DKM),
    // new SimpleEasyEnsemble(5, new DecisionTreeTest(
    // DecisionTreeTest.RP_PRUNE)),
    // new SetSplitLearner(new DecisionTreeTest(
    // DecisionTreeTest.RP_PRUNE)), new RIPPERk(true, 0),
    // new RIPPERk(true, 1) };
    // private static final String[] L_NAMES = { "Entropy", "EntropyNoprune",
    // "DKM", "DKMnoprune", "Easy", "Split", "Ripper", "RipperOp" };
    // private static final Learner[] LEARNERS = { new AnnLearner2(3, 0.1, 0.1),
    // new AnnLearner2(5, 0.1, 0.1), new AnnLearner2(10, 0.1, 0.1) };
    // private static final String[] L_NAMES = { "Ann3", "Ann5", "Ann10" };
    private static final Learner[] LEARNERS = { new WekaDt(),
            new WekaDt(false), new RandomForest() };
    private static final String[] L_NAMES = { "WekaDt", "WekaDtNoprune",
            "RandomForest" };

    public static final HashMap<Long, HashSet<Long>> VALID_USERS =
            getValidUsers();

    private final Database db;
    private final UserInfo author;
    private final ExampleGetter exGetter;
    private final boolean isGlobal;

    public Main(final Database db, final long authorId, final boolean isGlobal) {
        this.db = db;
        this.author = db.getUser(authorId);
        final List<Status> auTweets =
                getAuthorTweets(authorId, ExampleGetter.TRAIN_START_DATE,
                        ExampleGetter.TEST_END_DATE);

        List<Status> auTweetsM2 = null;
        if (isGlobal) {
            auTweetsM2 =
                    getAuthorTweets(authorId, ExampleGetter.TEST_END_DATE,
                            ExampleGetter.TESTM2_END_DATE);
        }
        this.exGetter = new ExampleGetter(db, auTweets, auTweetsM2);
        this.isGlobal = isGlobal;
        for (long folId : db.getTopFollowers(authorId, 0)) {
            FeatureExtractor.GETTER_LIST_OF_PREDICT_NUMBER
                    .add(new FeatureExtractor.Ffol(folId));
        }
    }

    private List<Status> getAuthorTweets (long authorId, Date fromDate,
            Date toDate) {
        final List<Status> auTweets =
                db.getOriginalTweetListInTimeRange(authorId, fromDate, toDate);
        Iterator<Status> iter = auTweets.iterator();
        while (iter.hasNext()) {
            Status t = iter.next();
            if (t.getRetweetCount() == 0) {
                iter.remove();
            }
        }
        Collections.sort(auTweets, ExampleGetter.TWEET_SORTER);
        return auTweets;
    }

    private void testPredictNum () throws Exception {
        assert author != null;
        final Exs exs = exGetter.getExsForPredictNum();
        if (!MeToWeka.hasSetAttribute()) {
            MeToWeka.setAttributes(FeatureExtractor.getAttrListOfPredictNum());
        }
        Instances train = MeToWeka.convertInstances(exs.train);

        weka.classifiers.functions.MultilayerPerceptron cls =
                new weka.classifiers.functions.MultilayerPerceptron();
        cls.setValidationThreshold(30);
        cls.setTrainingTime(1000);
        cls.setLearningRate(0.1);
        cls.setMomentum(0.2);
        // cls.setNormalizeAttributes(false);
        // cls.setNormalizeNumericClass(false);
        cls.buildClassifier(train);
        System.out.println("****************");
        System.out.println("AuthorName Predict Actual");

        Instances test = MeToWeka.convertInstances(exs.testM2);
        double[] ps = new double[test.numInstances()];
        double[] as = new double[test.numInstances()];
        for (int i = 0; i < test.numInstances(); i++) {
            Instance inst = test.instance(i);
            double result = cls.classifyInstance(inst);
            double act = Math.log(exGetter.auTweetsM2.get(i).getRetweetCount());
            System.out.printf("%s %.3f %.2f%n",
                    author.userProfile.getScreenName(), result, act);
            ps[i] = result;
            as[i] = act;
        }
        System.out.println("****************");
        System.out.println("Test Pearson: "
                + MyMath.getPearsonCorrelation(ps, as));
        System.out.println("****************");

        System.out.println("****************");
        System.out.println("AuthorName Predict Actual");

        ps = new double[train.numInstances()];
        as = new double[train.numInstances()];
        for (int i = 0; i < train.numInstances(); i++) {
            Instance inst = train.instance(i);
            double result = cls.classifyInstance(inst);
            double act = Math.log(exGetter.auTweets.get(i).getRetweetCount());
//            System.out.printf("%s %.3f %.2f%n",
//                    author.userProfile.getScreenName(), result, act);
            ps[i] = result;
            as[i] = act;
        }
        System.out.println("****************");
        System.out.println("Train Pearson: "
                + MyMath.getPearsonCorrelation(ps, as));
        System.out.println("****************");
    }

    @SuppressWarnings("unused")
    private void test () {
        assert author != null;
        assert author.followersIds != null;
        RawAttrList attrs = FeatureExtractor.getAttrListOfModel1();
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
            final Exs exs = exGetter.getExs(folId, isGlobal);
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
                                    exs.train, exs.testM1, exs.testM2, attrs,
                                    isGlobal);
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
            StringBuilder sb = new StringBuilder();
            for (int learner = 0; learner < LEARNERS.length; learner++) {
                System.out.println(L_NAMES[learner] + " information");
                HashMap<Long, List<Double>> folToRtProb =
                        listOfFolToProb.get(learner);
                sb.append(showGlobalInfo(fols, folToRtProb, folToAvgRt,
                        folToNumOfFs, L_NAMES[learner],
                        author.userProfile.getScreenName()));
            }
            System.out.println("****************");
            System.out
                    .println("Leaner AuthorName TweetId Actual# LikelihoodSum AvgRtPred NumOfFsPred");
            System.out.println(sb.toString());
            System.out.println("****************");
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

    private String showGlobalInfo (Long[] fols,
            HashMap<Long, List<Double>> folToRtProb,
            HashMap<Long, Double> folToAvgRt,
            HashMap<Long, Integer> folToNumOfFs, String learner, String author) {
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

        // Print result of each tweet.
        StringBuilder sb = new StringBuilder();
        for (int tidx = 0; tidx < exGetter.auTweetsM2.size(); tidx++) {
            Status t = exGetter.auTweetsM2.get(tidx);
            sb.append(String.format("%s %s %d %d %.2f %.2f %.2f%n", learner,
                    author, t.getId(), t.getRetweetCount(),
                    likelihoodSums.get(tidx), avgRtPred.get(tidx),
                    numOfFsPred.get(tidx)));
        }
        return sb.toString();
    }

    public static void main (String[] args) throws Exception {
        // final OutputRedirection or = new OutputRedirection();
        System.out.println("Begin at: " + new Date().toString());
        final Database db = Database.getInstance();
        for (long authorId : VALID_USERS.keySet()) {
            if (authorId != 16958346L) {
                continue;
            }
            new Main(db, authorId, IS_GLOBAL).testPredictNum();

        }
        System.out.println("End at: " + new Date().toString());
        // or.close();
    }
}
