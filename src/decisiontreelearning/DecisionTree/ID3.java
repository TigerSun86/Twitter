package decisiontreelearning.DecisionTree;

/**
 * FileName: ID3.java
 * @Description: Learn decision tree by ID3.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.HashMap;

import util.Dbg;

public class ID3 {
    public static final String MODULE = "ID3";
    public static final boolean DBG = false;

    public enum SplitCriteria {
        ENTROPY, DKM // DKM must be boolean classification problem.
    }

    private final SplitCriteria splitCriteria;

    public ID3() {
        this.splitCriteria = SplitCriteria.ENTROPY;
    }

    public ID3(SplitCriteria sc) {
        this.splitCriteria = sc;
    }

    public DecisionTree learnDecisionTree (final ExampleSet examples,
            final AttributeList attrList, final String defaultClass) {
        if (examples.isEmpty()) {
            return new DecisionTree(defaultClass);
        }
        final String classification = examples.sameClassification(attrList);
        if (classification != null) {
            return new DecisionTree(classification);
        }
        if (attrList.isAllDisabled()) {
            return new DecisionTree(examples.mode(attrList));
        }
        // Choose the best attribute for the tree constructing.
        final Attribute best = chooseAttribute(examples, attrList);
        // Delete the best attribute from the attribute list,
        // so attributes becomes attributes - best.
        attrList.disable(best);
        // Construct a decision tree with root is the attribute best.
        final DecisionTree tree = new DecisionTree(best.getName());
        for (String value : best.valueList()) {
            // Get sub example list with best == valueI.
            final ExampleSet subExamples =
                    examples.subExampleSet(attrList, best, value);
            // Construct a sub tree.
            final DecisionTree subTree =
                    learnDecisionTree(subExamples, attrList,
                            examples.mode(attrList));
            subTree.setParent(tree); // Set parent of the sub tree.
            subTree.setValueOfParent(value);
            // Add the valueI and sub tree as a branch to the decision tree.
            tree.addBranch(value, subTree);
        }
        // Recover the deleted attribute in the attribute list before return.
        attrList.enable(best);
        return tree;
    }

    public static final double evalDecisionTree (final DecisionTree tree,
            final ExampleSet testSet, final AttributeList attributes) {
        int hit = 0;
        for (Example example : testSet.getExampleSet()) {
            DecisionTree subTree = tree;
            // Hit the leaf of decision tree according to the example.
            while (!subTree.isLeaf()) {
                // Get current attribute from tree.
                final String attrName = subTree.getRoot();
                // Get value of the attribute from test example
                final int attrIndex =
                        attributes.indexOf(attributes.get(attrName));
                final String value = example.get(attrIndex);
                // Get the sub tree of the tree according to the value.
                subTree = subTree.getSubTree(value);
            }
            // The sub tree is a leaf.
            final String classOfTree = subTree.getRoot();
            final String classOfExam = example.get(attributes.indexOfTarget());
            if (classOfTree.equals(classOfExam)) {
                hit++;
            }
        }
        final int sum = testSet.size();
        return ((double) hit) / sum;
    }

    private Attribute chooseAttribute (final ExampleSet examples,
            final AttributeList attrList) {
        final double before = before(examples, attrList);
        Dbg.print(DBG, MODULE, "Before: " + before);
        double maxGain = Double.NEGATIVE_INFINITY;
        Attribute bestAttr = null;
        for (Attribute attr : attrList.validList()) {
            Dbg.print(DBG, MODULE, "Attribute: " + attr.getName());
            final double gain = before - after(examples, attrList, attr);
            if (Double.compare(maxGain, gain) < 0) {
                maxGain = gain;
                bestAttr = attr;
            }
        }
        assert bestAttr != null;
        Dbg.print(DBG, MODULE, "Best attribute: " + bestAttr.getName()
                + " gain: " + maxGain);
        return bestAttr;
    }

    private double before (final ExampleSet examples,
            final AttributeList attrList) {
        // "Before" only calculates one information uncertainty,
        // so the weight factor is 1.
        return weightedUncertainty(examples, attrList, null, null);
    }

    private double after (final ExampleSet examples,
            final AttributeList attrList, final Attribute attr) {
        double weightedInfoUncerSum = 0;
        for (String value : attr.valueList()) {
            weightedInfoUncerSum +=
                    weightedUncertainty(examples, attrList, attr, value);
        }
        Dbg.print(DBG, MODULE, "Attribute: " + attr.getName()
                + " entropy after: " + weightedInfoUncerSum);
        return weightedInfoUncerSum;
    }

    private double weightedUncertainty (final ExampleSet examples,
            final AttributeList attrList, final Attribute attr,
            final String value) {
        final int sum = examples.numberOf(attrList, attr, value, null);
        if (sum == 0) {
            // If there's no example of this value, the uncertainty is 0.
            Dbg.print(DBG, MODULE, "Value: " + value
                    + " has no example, entropy is 0");
            return 0;
        }

        final ArrayList<Double> probs = new ArrayList<Double>();
        for (String classifi : attrList.classList()) {
            // Get number of examples filtered by class.
            final int number =
                    examples.numberOf(attrList, attr, value, classifi);
            // Calculate the probability of this class.
            probs.add(((double) number) / sum);
            final String str =
                    String.format("Value: %s, class: %s, number: %d, sum: %d",
                            value, classifi, number, sum);
            Dbg.print(DBG, MODULE, str);
        }
        final int universalSet = examples.numberOf(attrList, null, null, null);
        final double factor = ((double) sum) / universalSet;
        final double uncer;
        if (this.splitCriteria == SplitCriteria.ENTROPY) {
            uncer = infoUncertainty(probs);
        } else { // (this.splitCriteria == SplitCriteria.DKM)
            uncer = dkm(probs);
        }
        final double weighted = uncer * factor;
        final String str =
                String.format("Value: %s, entropy: %.3f, weighted: %.3f",
                        value, uncer, weighted);
        Dbg.print(DBG, MODULE, str);
        return weighted;
    }

    private static final double LOG_2 = Math.log(2);
    // To make log calculation faster.
    private static final HashMap<Double, Double> LOG_CACHE =
            new HashMap<Double, Double>();

    private static double
            infoUncertainty (final ArrayList<Double> probabilities) {
        double sum = 0;
        for (double prob : probabilities) {
            if (prob == 0) {
                // If prob == 0, the logProb will be neg infinite, and this part
                // of uncertainty is 0.
                continue;
            }

            Double logP = LOG_CACHE.get(prob);
            if (logP == null) {
                logP = Math.log(prob + Double.MIN_VALUE);
                LOG_CACHE.put(prob, logP);
            }
            // I(P(v1),...P(vn))= sum(-P(vi) * log2 P(vi))
            sum += -prob * (logP / LOG_2);
        }
        return sum;
    }

    private static double dkm (final ArrayList<Double> probabilities) {
        assert probabilities.size() == 2; // Must be 2 classification problem.
        double a = probabilities.get(0);
        double b = probabilities.get(1);
        double sum = 2 * (Math.sqrt(a * b));
        return sum;
    }
}
