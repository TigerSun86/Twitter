package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
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
import weka.core.Instance;
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
 * FileName: Main.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 9, 2015 2:33:10 PM
 */
public class Main {
    private final UserInfo author;
    private final FeatureExtractor featureGetters;
    private final ExampleGetter exGetter;

    public Main(final Database db, final long authorId) {
        this.author = db.getUser(authorId);
        final List<Status> auTweets =
                db.getAuthorTweets(authorId, ExampleGetter.TRAIN_START_DATE,
                        ExampleGetter.TEST_START_DATE);

        List<Status> auTweetsM2 =
                db.getAuthorTweets(authorId, ExampleGetter.TEST_START_DATE,
                        ExampleGetter.TEST_END_DATE);

        this.featureGetters = new FeatureExtractor();
        this.exGetter =
                new ExampleGetter(db, auTweets, auTweetsM2, featureGetters);

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

        boolean cRt = false;
        boolean cWeb = true;
        EntityType cType = EntityType.WORD;
        SimMode cMode = SimMode.AEMI;

        int[] nums = { 10 };
        for (KickFeature kick : KickFeature.values()) {
            if (kick != KickFeature.E) {
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
                    ClusterWordFeatureFactory fac =
                            new ClusterWordFeatureFactory();

                    fac.para.docPara.withOt = true;
                    fac.para.docPara.withRt = cRt;
                    fac.para.docPara.withWeb = cWeb;
                    fac.para.docPara.entityType = cType;
                    fac.para.docPara.numOfWords = -1;

                    fac.para.simMode = cMode;
                    fac.para.needPrescreen = true;
                    fac.para.clAlg = new SingleCutAlg(num, false);

                    featureList.add(fac);
                    info.append(String.format("_C_%d_RT%b_Web%b_%s_%s", num,
                            cRt, cWeb, cType, cMode));
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
        // FEATURE_EDITORS.add(new FeatureEditor(featureList, "Base"));
        FEATURE_EDITORS.addAll(LDA_FEATURE_EDITORS);
    }

    private void testClusterFeature () throws Exception {
        File f = new File("debug.txt");
        if (!f.exists()) {
            f.createNewFile();
        }
        final PrintStream ps = System.out;
        System.setOut(new PrintStream(new FileOutputStream(f, true)));

        for (FeatureEditor featureEditor : FEATURE_EDITORS) {
            featureEditor.setFeature(this.featureGetters, exGetter.auTweets);
            final ExsForWeka exs = exGetter.getExsInWekaForPredictNum();
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
                System.out
                        .printf("%s, %s, %s, %.4f, %.4f, %.4f, %.4f%%, %.4f%%, ",
                                author.userProfile.getScreenName(),
                                W_L_NAMES[li], featureEditor.name,
                                e.correlationCoefficient(),
                                e.meanAbsoluteError(),
                                e.rootMeanSquaredError(),
                                e.relativeAbsoluteError(),
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

    @SuppressWarnings("unused")
    private void showCorrelation () throws Exception {
        File f = new File("debug.txt");
        if (!f.exists()) {
            f.createNewFile();
        }
        System.out.println("****\nIndex,Actual,Predicted");
        final PrintStream ps = System.out;
        System.setOut(new PrintStream(new FileOutputStream(f, true)));

        for (FeatureEditor featureEditor : FEATURE_EDITORS) {
            featureEditor.setFeature(this.featureGetters, exGetter.auTweets);
            final ExsForWeka exs = exGetter.getExsInWekaForPredictNum();
            Instances train = exs.train;
            Instances test = exs.test;
            int li = 2;
            WLearner learner = W_LEARNERS[li];
            Classifier cls = learner.buildClassifier(train);
            System.out.close();
            System.setOut(ps);

            for (int i = 0; i < test.numInstances(); i++) {
                Instance inst = test.instance(i);
                double act = inst.classValue();
                double pre = cls.classifyInstance(inst);
                System.out.printf("%d,%.2f,%.2f%n", i, act, pre);
            }

            System.setOut(new PrintStream(new FileOutputStream(f, true)));
        }
        System.out.close();
        System.setOut(ps);
    }

    public static void main (String[] args) throws Exception {
        System.out.println("Begin at: " + new Date().toString());
        System.out.print("AuthorName, Learner, Mode, ");
        System.out
                .print("Train Correlation coefficient, Train Mean absolute error, Train Root mean squared error, Train Relative absolute error, Train Root relative squared error, ");
        System.out
                .println("Test Correlation coefficient, Test Mean absolute error, Test Root mean squared error, Test Relative absolute error, Test Root relative squared error");

        final Database db = Database.getInstance();
        for (long authorId : UserInfo.KEY_AUTHORS) {
            if (authorId != 3459051L) {
                // continue;
            }
            new Main(db, authorId).testClusterFeature();

        }
        System.out.println("End at: " + new Date().toString());
    }
}
