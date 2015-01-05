package main;

import common.Evaluator;
import common.Hypothesis;
import common.Learner;
import common.MappedAttrList;
import common.RawAttrList;
import common.RawExampleList;

import decisiontreelearning.DecisionTree.DecisionTreeTest;

/**
 * FileName: ModelExecuter.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Dec 18, 2014 5:11:23 PM
 */
public class ModelExecuter {
    public static void main (String[] args) {
        final RawAttrList rawAttr = new RawAttrList(ATTR);
        final RawExampleList originalExs = new RawExampleList(DATA);
        System.out.println(run(originalExs, rawAttr));
    }

    public static final String ATTR =
            "file://localhost/C:/WorkSpace/Twitter/data/attr.txt";
    public static final String DATA =
            "file://localhost/C:/WorkSpace/Twitter/data/2551981338L-407374096.txt";

    public static String run (final RawExampleList originalExs,
            final RawAttrList rawAttr) {
        // Map all attributes in range 0 to 1.
        final MappedAttrList mAttr = new MappedAttrList(originalExs, rawAttr);

        // 2/3 as train, 1/3 as test.
        RawExampleList train = new RawExampleList();
        RawExampleList test = new RawExampleList();
        final int mid = originalExs.size() * 2 / 3;
        for (int i = 0; i < mid; i++) {
            train.add(originalExs.get(i));
        }
        for (int i = mid; i < originalExs.size(); i++) {
            test.add(originalExs.get(i));
        }

        // Rescale (map) all data in range 0 to 1.
        train = mAttr.mapExs(train, rawAttr);
        test = mAttr.mapExs(test, rawAttr);

        Learner learner = new DecisionTreeTest(DecisionTreeTest.NO_PRUNE);
        Hypothesis h = learner.learn(train, rawAttr);
        double atrain = Evaluator.evaluate(h, train);
        double atest = Evaluator.evaluate(h, test);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.4f %.4f", atrain, atest));

        learner = new DecisionTreeTest(DecisionTreeTest.RP_PRUNE);
        h = learner.learn(train, rawAttr);
        atrain = Evaluator.evaluate(h, train);
        atest = Evaluator.evaluate(h, test);
        sb.append(String.format(" %.4f %.4f", atrain, atest));
        return sb.toString();
    }

    public static String run2 (final RawExampleList trainIn,
            final RawExampleList testM1In, final RawExampleList testM2In,
            final RawAttrList rawAttr) {
        // Map all attributes in range 0 to 1.
        final MappedAttrList mAttr = new MappedAttrList(trainIn, rawAttr);

        // Rescale (map) all data in range 0 to 1.
        final RawExampleList train = mAttr.mapExs(trainIn, rawAttr);
        final RawExampleList testM1 = mAttr.mapExs(testM1In, rawAttr);
        final RawExampleList testM2 = mAttr.mapExs(testM2In, rawAttr);

        Learner learner = new DecisionTreeTest(DecisionTreeTest.RP_PRUNE);
        Hypothesis h = learner.learn(train, rawAttr);
        double atrain = Evaluator.evaluate(h, train);
        double atest = Evaluator.evaluate(h, testM1);
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.4f %.4f", atrain, atest));
        sb.append("-");
        for (int i = 0; i < testM2.size(); i++) { // actual.
            if (i != 0) {
                sb.append(" ");
            }
            sb.append(testM2.get(i).t);
        }
        sb.append("-");
        for (int i = 0; i < testM2.size(); i++) { // predict.
            if (i != 0) {
                sb.append(" ");
            }
            final String predict = h.predict(testM2.get(i).xList);
            sb.append(predict);
        }
        return sb.toString();
    }
}
