package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import learners.MeToWeka;
import learners.WAnn;
import learners.WLr;
import main.ExampleGetter.Exs;
import twitter4j.Status;
import util.MyMath;
import util.SysUtil;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import common.DataReader;
import common.Learner;
import common.ProbPredictor;
import common.RawAttrList;
import common.RawExample;
import datacollection.Database;
import datacollection.UserInfo;
import features.AttrSel;
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
    private static final Learner[] LEARNERS = { new WLr(), new WAnn(10),
            new WAnn(20) };
    private static final String[] L_NAMES = { "LR", "Ann10", "Ann20", };
    // private static final Learner[] LEARNERS = { new WekaDt(),
    // new WekaDt(false), new RandomForest() };
    // private static final String[] L_NAMES = { "WekaDt", "WekaDtNoprune",
    // "RandomForest" };

    public static final HashMap<Long, HashSet<Long>> VALID_USERS =
            getValidUsers();

    private final Database db;
    private final UserInfo author;
    private final FeatureExtractor featureGetters;
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

        this.featureGetters = new FeatureExtractor();
        for (long folId : db.getTopFollowers(authorId, 10)) {
            this.featureGetters.getterListOfPreNum
                    .add(new FeatureExtractor.Ffol(folId));
        }

        this.exGetter =
                new ExampleGetter(db, auTweets, auTweetsM2, featureGetters);
        this.isGlobal = isGlobal;

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

    private static final int[] ITERS =
            { 1, 10, 50, 100, 500, 1000, 5000, 10000 };

    private void testPredictNumWithDifIter () throws Exception {
        assert author != null;
        final Exs exs = exGetter.getExsForPredictNum();
        System.out.println("AuthorName Iteration TrainPearson TestPearson");
        for (int iter : ITERS) {
            WAnn learner = new WAnn(10);
            learner.iter = iter;
            ProbPredictor cls =
                    learner.learn(exs.train,
                            this.featureGetters.getAttrListOfPredictNum());

            double trainP;
            double testP;
            double trainE;
            double testE;

            double[] ps = new double[exs.train.size()];
            double[] as = new double[exs.train.size()];
            for (int i = 0; i < exs.train.size(); i++) {
                RawExample inst = exs.train.get(i);
                double result = cls.predictPosProb(inst.xList);
                double act = Double.parseDouble(inst.t);
                // System.out.printf("%s %.3f %.3f%n",
                // author.userProfile.getScreenName(), result, act);
                ps[i] = result;
                as[i] = act;
            }
            trainP = MyMath.getPearsonCorrelation(ps, as);
            trainE = MyMath.getRootMeanSquareError(ps, as);

            ps = new double[exs.testM2.size()];
            as = new double[exs.testM2.size()];
            for (int i = 0; i < exs.testM2.size(); i++) {
                RawExample inst = exs.testM2.get(i);
                double result = cls.predictPosProb(inst.xList);
                double act = Double.parseDouble(inst.t);
                // System.out.printf("%s %.3f %.3f%n",
                // author.userProfile.getScreenName(), result, act);
                ps[i] = result;
                as[i] = act;
            }
            testP = MyMath.getPearsonCorrelation(ps, as);
            testE = MyMath.getRootMeanSquareError(ps, as);
            System.out.printf("%s %5d %.3f %.3f %.3f %.3f%n",
                    author.userProfile.getScreenName(), iter, trainP, testP,
                    trainE, testE);
        }
    }

    private void testPredictNum () throws Exception {
        assert author != null;
        // System.out
        // .println("AuthorName Learner TrainPearson TestPearson TrainError TestError");
        for (int li = 0; li < LEARNERS.length; li++) {
            final Exs exs = exGetter.getExsForPredictNum();
            Learner learner = LEARNERS[li];
            ProbPredictor cls =
                    learner.learn(exs.train,
                            this.featureGetters.getAttrListOfPredictNum());

            double trainP;
            double testP;
            double trainE;
            double testE;
            double[] ps = new double[exs.train.size()];
            double[] as = new double[exs.train.size()];
            for (int i = 0; i < exs.train.size(); i++) {
                RawExample inst = exs.train.get(i);
                double result = cls.predictPosProb(inst.xList);
                double act = Double.parseDouble(inst.t);
                // System.out.printf("%s %.3f %.3f%n",
                // author.userProfile.getScreenName(), result, act);
                ps[i] = result;
                as[i] = act;
            }
            trainP = MyMath.getPearsonCorrelation(ps, as);
            trainE = MyMath.getRootMeanSquareError(ps, as);

            ps = new double[exs.testM2.size()];
            as = new double[exs.testM2.size()];
            for (int i = 0; i < exs.testM2.size(); i++) {
                RawExample inst = exs.testM2.get(i);
                double result = cls.predictPosProb(inst.xList);
                double act = Double.parseDouble(inst.t);
                // System.out.printf("%s %.3f %.3f%n",
                // author.userProfile.getScreenName(), result, act);
                ps[i] = result;
                as[i] = act;
            }
            testP = MyMath.getPearsonCorrelation(ps, as);
            testE = MyMath.getRootMeanSquareError(ps, as);

            System.out.printf("%s %s %.3f %.3f %.3f %.3f%n",
                    author.userProfile.getScreenName(), L_NAMES[li], trainP,
                    testP, trainE, testE);
        }

    }

    // @SuppressWarnings("unused")
    private void test () {
        assert author != null;
        assert author.followersIds != null;
        RawAttrList attrs = featureGetters.getAttrListOfModel1();
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
        List<Long> checkedFols = new ArrayList<Long>();
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
                        checkedFols.add(folId);
                    }
                } // for (int l = 0; l < LEARNERS.length; l++) {
            } // if (exs != null) {
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");
        if (isGlobal) {
            learnerToPearson = new HashMap<String, List<Double>>();
            StringBuilder sb = new StringBuilder();
            for (int learner = 0; learner < LEARNERS.length; learner++) {
                System.out.println(L_NAMES[learner] + " information");
                HashMap<Long, List<Double>> folToRtProb =
                        listOfFolToProb.get(learner);
                sb.append(showGlobalInfo(checkedFols, folToRtProb, folToAvgRt,
                        folToNumOfFs, L_NAMES[learner],
                        author.userProfile.getScreenName()));
            }
            System.out.println("****************");
            System.out
                    .println("Learner AuthorName TweetId Actual# LikelihoodSum AvgRtPred NumOfFsPred");
            System.out.println(sb.toString());
            System.out.println("****************");
            System.out
                    .println("Learner AuthorName LikelihoodSum AvgRtPred NumOfFsPred");

            for (Entry<String, List<Double>> entry : learnerToPearson
                    .entrySet()) {
                System.out.printf("%s %s %.4f %.4f %.4f%n", entry.getKey(),
                        author.userProfile.getScreenName(), entry.getValue()
                                .get(0), entry.getValue().get(1), entry
                                .getValue().get(2));
            }
            learnerToPearson = null;
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

    private String showGlobalInfo (List<Long> checkedFols,
            HashMap<Long, List<Double>> folToRtProb,
            HashMap<Long, Double> folToAvgRt,
            HashMap<Long, Integer> folToNumOfFs, String learner, String author) {
        List<Double> likelihoodSums = new ArrayList<Double>();
        List<Double> avgRtPred = new ArrayList<Double>();
        List<Double> numOfFsPred = new ArrayList<Double>();

        // Print each followers' id.
        System.out.print("FolIdAndTid");
        for (long folId : checkedFols) {
            System.out.print(" " + folId);
        }
        System.out.println();
        // Print each followers' AvgRt.
        System.out.print("AvgRt");
        for (long folId : checkedFols) {
            System.out.printf(" %.2f", folToAvgRt.get(folId));
        }
        System.out.println();
        // Print each followers' num of followers.
        System.out.print("NumOfFs");
        for (long folId : checkedFols) {
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
            for (long folId : checkedFols) {
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
        double[] act = new double[exGetter.auTweetsM2.size()];
        double[] likesum = new double[exGetter.auTweetsM2.size()];
        double[] rt = new double[exGetter.auTweetsM2.size()];
        double[] fs = new double[exGetter.auTweetsM2.size()];
        for (int i = 0; i < exGetter.auTweetsM2.size(); i++) {
            act[i] = Math.log(exGetter.auTweetsM2.get(i).getRetweetCount());
            likesum[i] = likelihoodSums.get(i);
            rt[i] = avgRtPred.get(i);
            fs[i] = numOfFsPred.get(i);
        }

        List<Double> pearsons = new ArrayList<Double>();
        pearsons.add(MyMath.getPearsonCorrelation(likesum, act));
        pearsons.add(MyMath.getPearsonCorrelation(rt, act));
        pearsons.add(MyMath.getPearsonCorrelation(fs, act));
        learnerToPearson.put(learner, pearsons);
        return sb.toString();
    }

    private HashMap<String, List<Double>> learnerToPearson = null;

    public static void main (String[] args) throws Exception {
        System.out.println("Begin at: " + new Date().toString());
        System.out.print("AuthorName, Learner, ");
        System.out
                .print("Train Correlation coefficient, Train Mean absolute error, Train Root mean squared error, Train Relative absolute error, Train Root relative squared error, ");
        System.out
                .println("Test Correlation coefficient, Test Mean absolute error, Test Root mean squared error, Test Relative absolute error, Test Root relative squared error");

        final Database db = Database.getInstance();
        for (long authorId : VALID_USERS.keySet()) {
            if (authorId != 16958346L) {
                // continue;
            }
            new Main(db, authorId, IS_GLOBAL).testAttrSel();

        }
        System.out.println("End at: " + new Date().toString());
    }

    private void testAttrSel () throws Exception {

        final Exs exs = exGetter.getExsForPredictNum();
        MeToWeka w =
                new MeToWeka(this.featureGetters.getAttrListOfPredictNum());
        Instances train = w.convertInstances(exs.train);
        AttrSel attrSel = new AttrSel();
        attrSel.selectAttr(train,
                new weka.classifiers.functions.LinearRegression());
        Instances selTrain = attrSel.reduceInstDi(train);
        Instances selTest =
                attrSel.reduceInstDi(w.convertInstances(exs.testM2));
        attrSel.showSelAttr();

        Classifier cls = new weka.classifiers.functions.LinearRegression();
        cls.buildClassifier(selTrain);

        Evaluation e;
        e = new Evaluation(selTrain);
        e.evaluateModel(cls, selTrain);
        System.out.printf("%s, %s, %.4f, %.4f, %.4f, %.4f%%, %.4f%%, ",
                author.userProfile.getScreenName(), "LR",
                e.correlationCoefficient(), e.meanAbsoluteError(),
                e.rootMeanSquaredError(), e.relativeAbsoluteError(),
                e.rootRelativeSquaredError());
        e = new Evaluation(selTrain);
        e.evaluateModel(cls, selTest);
        System.out.printf("%.4f, %.4f, %.4f, %.4f%%, %.4f%%%n",
                e.correlationCoefficient(), e.meanAbsoluteError(),
                e.rootMeanSquaredError(), e.relativeAbsoluteError(),
                e.rootRelativeSquaredError());

    }
}
