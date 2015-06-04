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
import weka.core.Instances;

import common.WLearner;

import datacollection.Database;
import datacollection.UserInfo;
import features.BaseFeatureFactory;
import features.ClusterWordFeatureFactory;
import features.FeatureEditor;
import features.FeatureExtractor;
import features.WordFeature;
import features.WordFeatureFactory;

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
                        ExampleGetter.TEST_END_DATE);

        List<Status> auTweetsM2 =
                db.getAuthorTweets(authorId, ExampleGetter.TEST_END_DATE,
                        ExampleGetter.TESTM2_END_DATE);

        this.featureGetters = new FeatureExtractor();
        this.exGetter =
                new ExampleGetter(db, auTweets, auTweetsM2, featureGetters);

    }

    private static final WLearner[] W_LEARNERS = { new WLr(), new WAnn(10),
            new WAnn(), new WLibSvm(0), new WLibSvm(1) };
    private static final String[] W_L_NAMES = { "LR", "Ann10", "AnnA",
            "epsilonSVR", "nuSVR" };

    private static final List<FeatureEditor> FEATURE_EDITORS;
    static {
        FEATURE_EDITORS = new ArrayList<FeatureEditor>();
        FEATURE_EDITORS
                .add(new FeatureEditor(new BaseFeatureFactory(), "Base"));
        FEATURE_EDITORS.add(new FeatureEditor(new WordFeatureFactory(
                WordFeature.Type.WORD, WordFeature.Mode.SUM), "Top10WordSum"));
        ClusterWordFeatureFactory ff;
        ff = new ClusterWordFeatureFactory();
        ff.para.numOfCl = 10;
        ff.withTweets = true;
        FEATURE_EDITORS.add(new FeatureEditor(ff, "10T"));
        ff = new ClusterWordFeatureFactory();
        ff.para.numOfCl = 10;
        ff.withTweets = false;
        FEATURE_EDITORS.add(new FeatureEditor(ff, "10NoT"));
        ff = new ClusterWordFeatureFactory();
        ff.para.numOfCl = 100;
        ff.withTweets = true;
        FEATURE_EDITORS.add(new FeatureEditor(ff, "100T"));
        ff = new ClusterWordFeatureFactory();
        ff.para.numOfCl = 100;
        ff.withTweets = false;
        FEATURE_EDITORS.add(new FeatureEditor(ff, "100NoT"));
        ff = new ClusterWordFeatureFactory();
        ff.para.numOfCl = 100;
        ff.withTweets = true;
        FEATURE_EDITORS.add(new FeatureEditor(ff, false, "100TOnly"));
    }

    private void testClusterFeature () throws Exception {
        for (FeatureEditor featureEditor : FEATURE_EDITORS) {
            featureEditor.setFeature(this.featureGetters, exGetter.auTweets);
            final ExsForWeka exs = exGetter.getExsInWekaForPredictNum();
            Instances train = exs.train;
            Instances test = exs.test;
            for (int li = 0; li < W_LEARNERS.length; li++) {
                File f = new File("debug.txt");
                if (!f.exists()) {
                    f.createNewFile();
                }
                final PrintStream ps = System.out;
                System.setOut(new PrintStream(new FileOutputStream(f, true)));

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

            } // for (int li = 0; li < W_LEARNERS.length; li++) {
        }
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
            if (authorId != 15461733L) {
                // continue;
            }
            new Main(db, authorId).testClusterFeature();

        }
        System.out.println("End at: " + new Date().toString());
    }
}
