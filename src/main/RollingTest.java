package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import learners.WAnn;
import learners.WLibSvm;
import learners.WLr;
import main.ExampleGetter.ExsForWeka;
import twitter4j.Status;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;
import common.WLearner;
import datacollection.Database;
import datacollection.UserInfo;
import features.BaseFeatureFactory;
import features.ClusterWordFeatureFactory;
import features.EntityPairFactory;
import features.FeatureEditor;
import features.FeatureEditor.FeatureFactory;
import features.FeatureExtractor;
import features.LdaFeatureFactory;
import features.SimCalculator.SimMode;
import features.SingleCutAlg;
import features.WordFeature.WordSelectingMode;
import features.WordFeatureFactory;
import features.WordStatisDoc.EntityType;

/**
 * FileName: RollingTest.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Aug 11, 2015 10:16:09 PM
 */
public class RollingTest {
    private static final int TRAIN_SET_WEEK_NUM = 4;
    private static final int TEST_SET_WEEK_NUM = 1;
    private static final int FIRST_AVAILABLE_WEEK_INDEX = 6;
    private static final int END_AVAILABLE_WEEK_INDEX = 24;

    private static final Date START_DATE;
    static {
        // Date should start at "Sun Feb 01 00:00:00 EST 2015".
        // Last available second is "Sat Jun 13 23:59:59 EST 2015".
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.WEEK_OF_YEAR, FIRST_AVAILABLE_WEEK_INDEX);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        START_DATE = cal.getTime();
    }

    private static final ArrayList<Date[]> DATA_SET_TIME_RANGES;
    static {
        DATA_SET_TIME_RANGES = new ArrayList<Date[]>();
        int weekIdx = FIRST_AVAILABLE_WEEK_INDEX;
        boolean isEnd = false;
        while (!isEnd) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(START_DATE);
            cal.set(Calendar.WEEK_OF_YEAR, weekIdx);
            Date trainStartDate = cal.getTime();
            cal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR)
                    + TRAIN_SET_WEEK_NUM);
            cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) - 1);
            Date trainEndDate = cal.getTime();
            cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) + 1);
            Date testStartDate = cal.getTime();
            cal.set(Calendar.WEEK_OF_YEAR, cal.get(Calendar.WEEK_OF_YEAR)
                    + TEST_SET_WEEK_NUM);
            cal.set(Calendar.SECOND, cal.get(Calendar.SECOND) - 1);
            Date testEndDate = cal.getTime();
            if (cal.get(Calendar.WEEK_OF_YEAR) <= END_AVAILABLE_WEEK_INDEX) {
                DATA_SET_TIME_RANGES.add(new Date[] { trainStartDate,
                        trainEndDate, testStartDate, testEndDate });
                // System.out.println("Round "
                // + (weekIdx - FIRST_AVAILABLE_WEEK_INDEX));
                // System.out.println("trainStartDate "
                // + trainStartDate.toString());
                // System.out.println("trainEndDate " +
                // trainEndDate.toString());
                // System.out.println("testStartDate " +
                // testStartDate.toString());
                // System.out.println("testEndDate " + testEndDate.toString());
            } else {
                isEnd = true;
            }
            weekIdx++;
        }
    }

    private static final WLearner[] W_LEARNERS = { new WLr(),
            new WAnn(-1, 0.1, 0.1),
            new WLibSvm(LibSVM.SVMTYPE_EPSILON_SVR, LibSVM.KERNELTYPE_RBF),
            new WLibSvm(LibSVM.SVMTYPE_NU_SVR, LibSVM.KERNELTYPE_RBF) };
    private static final String[] W_L_NAMES = { "LR", "AnnA1", "EpSvrRbf",
            "NuSvrRbf", };

    private static final List<FeatureEditor> TOP_SERIES_FEATURE_EDITORS;
    static {
        TOP_SERIES_FEATURE_EDITORS = new ArrayList<FeatureEditor>();
        List<FeatureFactory> featureList;
        int[] nums = { 10, 20, 30 };

        for (EntityType type : EntityType.values()) {
            for (WordSelectingMode mode : WordSelectingMode.values()) {
                if (mode != WordSelectingMode.SUM2) {
                    continue;
                }
                for (int numOfWords : nums) {
                    featureList = new ArrayList<FeatureFactory>();
                    featureList.add(new WordFeatureFactory(type, numOfWords,
                            mode));
                    TOP_SERIES_FEATURE_EDITORS.add(new FeatureEditor(
                            featureList, "Top" + numOfWords + type + mode));
                }
            }
        }
    }

    private static final List<FeatureEditor> ENTITY_PAIR_FEATURE_EDITORS;
    static {
        ENTITY_PAIR_FEATURE_EDITORS = new ArrayList<FeatureEditor>();
        int[] nums = { 10, 20, 30 };
        boolean[] rts = { true, false };
        for (SimMode mode : SimMode.values()) {
            if (!(mode == SimMode.SUM2 || mode == SimMode.IDF2)) continue;
            for (boolean rt : rts) {
                if (!rt
                        && !(mode == SimMode.AEMI || mode == SimMode.JACCARD || mode == SimMode.LIFT)) {
                    continue;
                }
                for (EntityType type : EntityType.values()) {
                    if (!(type == EntityType.ALLTYPE || type == EntityType.WORD)) {
                        continue;
                    }
                    for (int num : nums) {
                        List<FeatureFactory> featureList =
                                new ArrayList<FeatureFactory>();
                        EntityPairFactory fac = new EntityPairFactory();
                        fac.para.docPara.withOt = true;
                        fac.para.docPara.withRt = rt;
                        fac.para.docPara.withWeb = false;
                        fac.para.docPara.entityType = type;
                        fac.para.docPara.numOfWords = -1;
                        fac.para.simMode = mode;
                        fac.para.numOfPairs = num;
                        fac.para.needPrescreen = false;

                        featureList.add(fac);
                        ENTITY_PAIR_FEATURE_EDITORS
                                .add(new FeatureEditor(featureList,
                                        EntityPairFactory.PREFIX + mode + "_"
                                                + +num + "_" + type + "_RT"
                                                + rt));
                    }
                }
            }
        }
    }

    private static final List<FeatureEditor> CLUSTER_FEATURE_EDITORS;
    static {
        CLUSTER_FEATURE_EDITORS = new ArrayList<FeatureEditor>();

        SimMode[] modes = { SimMode.AEMI, SimMode.JACCARD };
        int[] nums = { 10, 20, 30 };
        boolean[] rts = { true, false };
        boolean[] webs = { true, false };
        EntityType[] ens = { EntityType.ALLTYPE, EntityType.WORD };
        for (SimMode simMode : modes) {
            for (boolean rt : rts) {
                for (boolean web : webs) {
                    if (rt && web) {
                        continue;
                    }
                    for (EntityType en : ens) {
                        for (int num : nums) {
                            List<FeatureFactory> featureList =
                                    new ArrayList<FeatureFactory>();
                            ClusterWordFeatureFactory fac =
                                    new ClusterWordFeatureFactory();

                            fac.para.docPara.entityType = en;
                            fac.para.docPara.numOfWords = -1;
                            fac.para.docPara.withOt = true;
                            fac.para.docPara.withRt = rt;
                            fac.para.docPara.withWeb = web;

                            fac.para.simMode = simMode;
                            fac.para.needPrescreen = true;
                            fac.para.clAlg = new SingleCutAlg(num, false);

                            featureList.add(fac);
                            CLUSTER_FEATURE_EDITORS.add(new FeatureEditor(
                                    featureList, simMode.toString() + num + "_"
                                            + en.toString() + "_RT" + rt
                                            + "_Web" + web));
                        }
                    }
                }
            }
        }
    }

    private static enum KickFeature {
        NONE, E, P, C
    }

    private static final List<FeatureEditor> ALL_COMBINE_FEATURE_EDITORS;
    static {
        ALL_COMBINE_FEATURE_EDITORS = new ArrayList<FeatureEditor>();

        EntityType eType = EntityType.ALLTYPE;
        WordSelectingMode eMode = WordSelectingMode.IDF2;

        boolean pRt = true;
        boolean pWeb = false;
        EntityType pType = EntityType.ALLTYPE;
        SimMode pMode = SimMode.AEMI;

        boolean cOt = true;
        boolean cRt = false;
        boolean cWeb = true;
        EntityType cType = EntityType.ALLTYPE;
        // SimMode cMode = SimMode.AEMI;

        int[] nums = { 10, 20, 30 };
        for (KickFeature kick : KickFeature.values()) {
            if (kick == KickFeature.C) {
                continue;
            }
            for (int num : nums) {
                List<FeatureFactory> featureList =
                        new ArrayList<FeatureFactory>();
                StringBuilder info = new StringBuilder("Com");
                if (kick != KickFeature.E) { // Entity
                    featureList.add(new WordFeatureFactory(eType, num, eMode));
                    info.append(String.format("_E_%d_%s_%s", num, eType, eMode));
                }
                if (kick != KickFeature.P) { // Pair
                    EntityPairFactory fac = new EntityPairFactory();
                    fac.para.docPara.withOt = true;
                    fac.para.docPara.withRt = pRt;
                    fac.para.docPara.withWeb = pWeb;
                    fac.para.docPara.entityType = pType;
                    fac.para.docPara.numOfWords = -1;
                    fac.para.simMode = pMode;
                    fac.para.numOfPairs = num;
                    fac.para.needPrescreen = false;

                    featureList.add(fac);
                    info.append(String.format("_P_%d_RT%b_Web%b_%s_%s", num,
                            pRt, pWeb, pType, pMode));
                }
                if (kick != KickFeature.C) { // Cluster
                    LdaFeatureFactory fac = new LdaFeatureFactory();

                    fac.para.docPara.entityType = cType;
                    fac.para.docPara.numOfWords = -1;
                    fac.para.docPara.withOt = cOt;
                    fac.para.docPara.withRt = cRt;
                    fac.para.docPara.withWeb = cWeb;

                    fac.para.numOfCl = num;
                    fac.para.numOfIter = 2000;

                    featureList.add(fac);
                    info.append(String.format("_C_%d_OT%b_RT%b_Web%b_%s", num,
                            cOt, cRt, cWeb, cType));
                }
                ALL_COMBINE_FEATURE_EDITORS.add(new FeatureEditor(featureList,
                        info.toString()));
            }
        }
    }

    private static final List<FeatureEditor> LDA_FEATURE_EDITORS;
    static {
        LDA_FEATURE_EDITORS = new ArrayList<FeatureEditor>();
        int[] nums = { 10, 20, 30 };
        boolean[] ots = { true, false };
        boolean[] webs = { true, false };
        EntityType[] ens = { EntityType.ALLTYPE, EntityType.WORD };
        for (boolean ot : ots) {
            for (boolean web : webs) {
                if (!ot && !web) {
                    continue;
                }
                for (EntityType en : ens) {
                    if (!ot && !en.equals(EntityType.WORD)) {
                        continue;
                    }
                    for (int num : nums) {
                        List<FeatureFactory> featureList =
                                new ArrayList<FeatureFactory>();
                        LdaFeatureFactory fac = new LdaFeatureFactory();

                        fac.para.docPara.entityType = en;
                        fac.para.docPara.numOfWords = -1;
                        fac.para.docPara.withOt = ot;
                        fac.para.docPara.withRt = false;
                        fac.para.docPara.withWeb = web;

                        fac.para.numOfCl = num;
                        fac.para.numOfIter = 2000;

                        featureList.add(fac);
                        LDA_FEATURE_EDITORS.add(new FeatureEditor(featureList,
                                "LDA" + num + "_" + en.toString() + "_OT" + ot
                                        + "_Web" + web));
                    }
                }
            }
        }
    }

    private static final List<FeatureEditor> FEATURE_EDITORS;
    static {
        FEATURE_EDITORS = new ArrayList<FeatureEditor>();
        List<FeatureFactory> featureList;
        featureList = new ArrayList<FeatureFactory>();
        featureList.add(new BaseFeatureFactory());
        FEATURE_EDITORS.add(new FeatureEditor(featureList, "Base"));
        FEATURE_EDITORS.addAll(ALL_COMBINE_FEATURE_EDITORS);
    }

    final Database db = Database.getInstance();

    private void testOneDataSet (long authorId, int round, boolean useWeights,
            List<Status> trainSet, List<Status> testSet) throws Exception {
        File f = new File("debug.txt");
        if (!f.exists()) {
            f.createNewFile();
        }
        final PrintStream ps = System.out;
        System.setOut(new PrintStream(new FileOutputStream(f, true)));

        FeatureExtractor featureGetters = new FeatureExtractor();
        ExampleGetter exGetter =
                new ExampleGetter(db, trainSet, testSet, featureGetters);

        String weightMode = (useWeights ? "W_" : "");
        System.out.println("**** Use weights: " + useWeights);
        for (FeatureEditor featureEditor : FEATURE_EDITORS) {
            featureEditor.setFeature(featureGetters, exGetter.auTweets);
            final ExsForWeka exs =
                    exGetter.getExsInWekaForPredictNum(useWeights);
            Instances train = exs.train;
            Instances test = exs.test;
            for (int li = 0; li < W_LEARNERS.length; li++) {
                WLearner learner = W_LEARNERS[li];
                Classifier cls = learner.buildClassifier(train);

                Evaluation e;
                e = new Evaluation(train);
                e.evaluateModel(cls, train);

                System.out.close();
                System.setOut(ps);
                System.out.printf(
                        "%s, %d, %s, %s, %.4f, %.4f, %.4f, %.4f%%, %.4f%%, ",
                        UserInfo.KA_ID2SCREENNAME.get(authorId), round,
                        W_L_NAMES[li], weightMode + featureEditor.name,
                        e.correlationCoefficient(), e.meanAbsoluteError(),
                        e.rootMeanSquaredError(), e.relativeAbsoluteError(),
                        e.rootRelativeSquaredError());
                e = new Evaluation(train);
                e.evaluateModel(cls, test);
                System.out.printf("%.4f, %.4f, %.4f, %.4f%%, %.4f%%%n",
                        e.correlationCoefficient(), e.meanAbsoluteError(),
                        e.rootMeanSquaredError(), e.relativeAbsoluteError(),
                        e.rootRelativeSquaredError());
                System.setOut(new PrintStream(new FileOutputStream(f, true)));

            } // for (int li = 0; li < W_LEARNERS.length; li++) {
        }
        System.out.close();
        System.setOut(ps);
    }

    private void run (long authorId) throws Exception {
        for (int round = 0; round < DATA_SET_TIME_RANGES.size(); round++) {
            Date[] ds = DATA_SET_TIME_RANGES.get(round);
            final List<Status> train =
                    db.getAuthorTweets(authorId, ds[0], ds[1]);
            final List<Status> test =
                    db.getAuthorTweets(authorId, ds[2], ds[3]);
            testOneDataSet(authorId, round + 1, false, train, test);
            //testOneDataSet(authorId, round + 1, true, train, test);
        }
    }

    public static void main (String[] args) throws Exception {
        System.out.println("Begin at: " + new Date().toString());
        System.out.print("AuthorName, Round, Learner, Mode, ");
        System.out
                .print("Train Correlation coefficient, Train Mean absolute error, Train Root mean squared error, Train Relative absolute error, Train Root relative squared error, ");
        System.out
                .println("Test Correlation coefficient, Test Mean absolute error, Test Root mean squared error, Test Relative absolute error, Test Root relative squared error");

        for (long authorId : UserInfo.KEY_AUTHORS) {
            if (authorId != 16958346L) {
                // continue;
            }
            new RollingTest().run(authorId);
        }
        System.out.println("End at: " + new Date().toString());
    }
}
