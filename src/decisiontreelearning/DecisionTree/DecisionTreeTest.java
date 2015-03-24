package decisiontreelearning.DecisionTree;

/**
 * FileName: DecisionTreeTest.java
 * @Description: Test decision tree learning and pruning.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.List;

import util.Dbg;
import common.Hypothesis;
import common.Learner;
import common.RawAttr;
import common.RawAttrList;
import common.RawExample;
import common.RawExampleList;
import common.TrainTestSplitter;
import decisiontreelearning.Rule.CombinedRulePostPruning;
import decisiontreelearning.Rule.RuleList;
import decisiontreelearning.Rule.RulePostPruning;
import decisiontreelearning.Rule.TreeToRule;

public class DecisionTreeTest implements Learner {
    public static final String MODULE = "DTT";
    public static final boolean DBG = true;

    public static final int NO_PRUNE = 0;
    public static final int RP_PRUNE = 1;
    public static final int CR_PRUNE = 2;

    private final int pruneWay;
    private final ID3.SplitCriteria sc;

    public DecisionTreeTest(final int pruneWay, final ID3.SplitCriteria sc) {
        this.pruneWay = pruneWay;
        this.sc = sc;
    }

    public DecisionTreeTest(final int pruneWay) {
        this.pruneWay = pruneWay;
        this.sc = ID3.SplitCriteria.ENTROPY;
    }

    private static ExampleSet convertExs (RawExampleList set) {
        final ExampleSet ret = new ExampleSet();
        for (RawExample e : set) {
            Example e2 = new Example();
            e2.addAll(e.xList);
            e2.add(e.t);
            ret.add(e2);
        }
        return ret;
    }

    private static AttributeList convertAttr (RawAttrList attrs) {
        final AttributeList ret = new AttributeList();
        for (RawAttr a : attrs.xList) {
            final Attribute a2 = new Attribute(a.name);
            if (a.isContinuous) {
                a2.add(Discretizor.CONT);
            } else {
                a2.addAll(a.valueList);
            }
            ret.add(a2);
        }
        final Attribute a2 = new Attribute(attrs.t.name);
        if (attrs.t.isContinuous) {
            a2.add(Discretizor.CONT);
        } else {
            a2.addAll(attrs.t.valueList);
        }
        ret.add(a2);
        return ret;
    }

    private List<RawExampleList> splitTrainVal (RawExampleList dataSet,
            RawAttrList attrs, boolean isRandomSplit) {
        List<RawExampleList> list = new ArrayList<RawExampleList>();
        if (isRandomSplit) {
            // Train and test will split into same pos rate.
            final RawExampleList[] exs2 =
                    TrainTestSplitter.split(dataSet, attrs,
                            TrainTestSplitter.DEFAULT_RATIO);
            final RawExampleList train = exs2[0];
            final RawExampleList val = exs2[1];
            list.add(train);
            list.add(val);
        } else { // Time split
            // 2/3 as train, 1/3 as validation.
            RawExampleList train = new RawExampleList();
            RawExampleList val = new RawExampleList();
            final int mid = dataSet.size() * 2 / 3;
            for (int i = 0; i < mid; i++) {
                train.add(dataSet.get(i));
            }
            for (int i = mid; i < dataSet.size(); i++) {
                val.add(dataSet.get(i));
            }
            list.add(train);
            list.add(val);
        }
        return list;
    }

    @Override
    public Hypothesis learn (RawExampleList dataSet, RawAttrList attrs) {
        AttrAndData aad = new AttrAndData();
        if (pruneWay != NO_PRUNE) {
            List<RawExampleList> list = splitTrainVal(dataSet, attrs, true);
            ExampleSet train2 = convertExs(list.get(0));
            ExampleSet val2 = convertExs(list.get(1));
            aad.trainSet = train2;
            aad.validationSet = val2;
        } else { // No need prune.
            aad.trainSet = convertExs(dataSet);
        }

        AttributeList a = convertAttr(attrs);
        aad.attrList = a;

        AttributeList oldAttrList;
        oldAttrList = a;
        AttributeList newAttrList;
        Discretizor discretizor;
        // Ensure all attributes are discrete.
        aad = DiscretizeContinuousValues(aad);
        newAttrList = aad.attrList;
        discretizor = aad.discretizor;

        // Learning the decision tree.
        final DecisionTree dTree =
                new ID3(this.sc).learnDecisionTree(aad.trainSet, aad.attrList,
                        aad.trainSet.mode(aad.attrList));
        dTree.oldAttrList = oldAttrList;
        dTree.newAttrList = newAttrList;
        dTree.discretizor = discretizor;
        // System.out.println(dTree);
        if (pruneWay == NO_PRUNE) {
            return dTree;
        } else {
            // Convert decision tree to rules.
            final RuleList ruleList = TreeToRule.convert(dTree);
            final RuleList prunedRL;

            if (pruneWay == RP_PRUNE) {
                // Prune rule by Rule Post-pruning.
                prunedRL =
                        RulePostPruning.prune(ruleList, aad.validationSet,
                                aad.attrList);
            } else { // if (pruneWay == CR_PRUNE)
                // Prune rule by Combined Rule Post-pruning.
                prunedRL =
                        CombinedRulePostPruning.prune(ruleList,
                                aad.validationSet, aad.attrList);
            }

            prunedRL.oldAttrList = oldAttrList;
            prunedRL.newAttrList = newAttrList;
            prunedRL.discretizor = discretizor;
            prunedRL.defaultPre = aad.trainSet.mode(aad.attrList).cl;
            return prunedRL;
        }
    }

    public static double testDecisionTree (final String attrFName,
            final String trainFName, final String testFName,
            final int pruneWay, final double corruptRatio) {
        AttrAndData aad =
                init(attrFName, trainFName, testFName, pruneWay, corruptRatio);
        if (aad == null) {
            return 0;
        }
        // Ensure all attributes are discrete.
        aad = DiscretizeContinuousValues(aad);

        // Learning the decision tree.
        final DecisionTree dTree =
                new ID3().learnDecisionTree(aad.trainSet, aad.attrList,
                        aad.trainSet.mode(aad.attrList));
        Dbg.print(DBG, MODULE,
                "Decision Tree:" + Dbg.NEW_LINE + dTree.toString());

        double accur = evalAccuracy(aad, dTree);
        if (aad.validationSet != null) {
            accur = testPrune(aad, dTree, pruneWay);
        }
        return accur;
    }

    private static class AttrAndData {
        public AttributeList attrList;
        public ExampleSet trainSet;
        public ExampleSet validationSet;
        public ExampleSet testSet;
        public Discretizor discretizor;
    }

    private static AttrAndData init (final String attrFName,
            final String trainFName, final String testFName,
            final int pruneWay, final double corruptRatio) {
        // Initialize the attributes.
        AttributeList attrList = getAttributeList(attrFName);
        if (attrList == null) {
            return null;
        }
        // Initialize the train set.
        ExampleSet trainSet = getExampleSet(trainFName);
        if (trainSet == null) {
            return null;
        }
        // Corrupt the train set by given ratio.
        trainSet = DataCorrupter.corrupt(trainSet, attrList, corruptRatio);

        ExampleSet testSet = null;
        if (testFName != null) { // Initialize the test set.
            testSet = getExampleSet(testFName);
        }
        ExampleSet validationSet = null;
        if (pruneWay != NO_PRUNE) { // Initialize the validation and train set.
            // Split data set into train set and validation set.
            final ExampleSet[] eArray = ExampleSet.split(trainSet, 0.6);
            trainSet = eArray[0];
            validationSet = eArray[1];
            Dbg.print(DBG, MODULE, "Train set size: " + trainSet.size()
                    + ", validation set size: " + validationSet.size()
                    + ", ratio: " + 0.6);
        }
        AttrAndData aad = new AttrAndData();
        aad.attrList = attrList;
        aad.trainSet = trainSet;
        aad.validationSet = validationSet;
        aad.testSet = testSet;
        return aad;
    }

    private static AttrAndData
            DiscretizeContinuousValues (final AttrAndData aad) {
        final AttrAndData ret;
        Discretizor cal = new Discretizor(aad.trainSet, aad.attrList);
        if (cal.contiAttrs.size() != 0) {
            // There is at least one continuous attribute.
            ret = new AttrAndData();
            ret.trainSet =
                    Discretizor.DiscretizeExampleSet(aad.trainSet,
                            aad.attrList, cal);
            if (aad.validationSet != null) {
                ret.validationSet =
                        Discretizor.DiscretizeExampleSet(aad.validationSet,
                                aad.attrList, cal);
            } else {
                ret.validationSet = null;
            }
            if (aad.testSet != null) {
                ret.testSet =
                        Discretizor.DiscretizeExampleSet(aad.testSet,
                                aad.attrList, cal);
            } else {
                ret.testSet = null;
            }
            ret.attrList = Discretizor.DiscretizeAttrList(aad.attrList, cal);
            ret.discretizor = cal;
        } else { // All attributes are discrete.
            ret = aad;
        }

        return ret;
    }

    private static double evalAccuracy (final AttrAndData aad,
            final DecisionTree dTree) {
        // Accuracy in train set.
        double accur = ID3.evalDecisionTree(dTree, aad.trainSet, aad.attrList);
        String str = String.format("Train set accuracy: %.3f", accur);
        Dbg.print(DBG, MODULE, str);
        if (aad.validationSet != null) { // Accuracy in validation set.
            accur =
                    ID3.evalDecisionTree(dTree, aad.validationSet, aad.attrList);
            str = String.format("Validation set accuracy: %.3f", accur);
            Dbg.print(DBG, MODULE, str);
        }
        final double retAccur;
        if (aad.testSet != null) { // Accuracy in test set.
            accur = ID3.evalDecisionTree(dTree, aad.testSet, aad.attrList);
            str = String.format("Test set accuracy: %.3f", accur);
            Dbg.print(DBG, MODULE, str);
            retAccur = accur;
        } else { // If there's no test set, accuracy is 0.
            retAccur = 0;
        }
        return retAccur;
    }

    private static double testPrune (final AttrAndData aad,
            final DecisionTree dTree, final int pruneWay) {
        // Convert decision tree to rules.
        final RuleList ruleList = TreeToRule.convert(dTree);
        final RuleList prunedRL;
        if (pruneWay == RP_PRUNE) {
            // Prune rule by Rule Post-pruning.
            prunedRL =
                    RulePostPruning.prune(ruleList, aad.validationSet,
                            aad.attrList);
        } else { // if (pruneWay == CR_PRUNE)
            // Prune rule by Combined Rule Post-pruning.
            prunedRL =
                    CombinedRulePostPruning.prune(ruleList, aad.validationSet,
                            aad.attrList);
        }

        // Display accuracy after pruning in all data set.
        double accu =
                RulePostPruning.evalRule(prunedRL, aad.trainSet, aad.attrList);
        String str =
                String.format("Train set accuracy after pruning: %.3f", accu);
        Dbg.print(DBG, MODULE, str);

        accu =
                RulePostPruning.evalRule(prunedRL, aad.validationSet,
                        aad.attrList);
        str =
                String.format("Validation set accuracy after pruning: %.3f",
                        accu);
        Dbg.print(DBG, MODULE, str);

        if (aad.testSet != null) {
            accu =
                    RulePostPruning.evalRule(prunedRL, aad.testSet,
                            aad.attrList);
            str = String.format("Test set accuracy after pruning: %.3f", accu);
            Dbg.print(DBG, MODULE, str);
            return accu;
        }
        return 0;
    }

    public static AttributeList getAttributeList (final String attrFName) {
        final AttributeList attrList = new AttributeList();
        final DataExtractor in = new DataExtractor(attrFName);

        while (true) {
            final String[] attrStr = in.nextLine();
            if (attrStr == null) {
                break;
            }

            final Attribute attr = new Attribute(attrStr[0]);
            for (int i = 1; i < attrStr.length; i++) {
                attr.add(attrStr[i]);
            }
            attrList.add(attr);
        }

        in.close();

        if (attrList.size() <= 1) {
            System.err.println("No enough attributes in: " + attrFName);
            return null;
        } else {
            return attrList;
        }
    }

    public static ExampleSet getExampleSet (final String examFName) {
        final ExampleSet exampleSet = new ExampleSet();
        final DataExtractor in = new DataExtractor(examFName);

        while (true) {
            final String[] examStr = in.nextLine();
            if (examStr == null) {
                break;
            }
            final Example example = new Example();
            for (String str : examStr) {
                example.add(str);
            }

            exampleSet.add(example);
        }

        in.close();

        if (exampleSet.isEmpty()) {
            System.err.println("No example in: " + examFName);
            return null;
        } else {
            return exampleSet;
        }
    }

}
