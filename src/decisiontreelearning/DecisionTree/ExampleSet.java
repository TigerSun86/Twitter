package decisiontreelearning.DecisionTree;

/**
 * FileName: ExampleSet.java
 * @Description: Example set and functions for subset and counting example
 *               number and so on.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.HashSet;
import java.util.Random;

import main.ExampleGetter;

public class ExampleSet {
    private final HashSet<Example> eSet;
    // Initialize it by initPriorPosProb()
    public double priorPosProb = -1;

    public ExampleSet() {
        this.eSet = new HashSet<Example>();
    }

    public final void add (final Example example) {
        eSet.add(example);
    }

    public final boolean isEmpty () {
        return eSet.isEmpty();
    }

    public final int size () {
        return eSet.size();
    }

    public final HashSet<Example> getExampleSet () {
        return eSet;
    }

    /** Initialize prior probability of positive examples */
    public void initPriorPosProb () {
        int numOfPos = 0;
        for (Example e : eSet) {
            if (e.get(e.size() - 1).equals(ExampleGetter.Y)) {
                numOfPos++;
            }
        }
        if (eSet.size() != 0) {
            priorPosProb = ((double) numOfPos) / eSet.size();
        } else {
            priorPosProb = 0;
        }
    }

    public static class ClassAndPosProb {
        String cl;
        double posProb;

        public ClassAndPosProb(String cl2, double prob) {
            cl = cl2;
            posProb = prob;
        }
    }

    /**
     * public final String sameClassification ()
     * 
     * Returns a certain classification if all examples are the same
     * classification.
     * And the probability of positive examples.
     * 
     * @return: class, if all examples have the same classification. null, if
     *          examples don'tList have the same classification or there's no
     *          example.
     */
    public final ClassAndPosProb sameClassification (
            final AttributeList attrList) {
        if (eSet.isEmpty()) {
            return null;
        }

        String sameClass = null;
        for (String classifi : attrList.classList()) {
            final int num = numberOf(attrList, null, null, classifi);
            if (num == eSet.size()) {
                // All examples are this class.
                sameClass = classifi;
                break;
            } else if (num != 0) {
                // There are at least two kinds of classification.
                break;
            }
        }
        if (sameClass != null) {
            // Calculate prob by m-estimate.
            assert priorPosProb != -1;
            double posProb;
            if (sameClass.equals(ExampleGetter.Y)) {// Pos class.
                posProb = (eSet.size() + priorPosProb) / (eSet.size() + 1);
            } else { // Neg class
                posProb = priorPosProb / (eSet.size() + 1);
            }
            return new ClassAndPosProb(sameClass, posProb);
        } else {
            return null;
        }
    }

    /**
     * public final ClassAndPosProb mode ()
     * 
     * Returns the classification occurring the most times in the example set.
     * And the probability of positive examples.
     * 
     * @return: class occurring the most. null, if there's no example.
     */
    public final ClassAndPosProb mode (final AttributeList attrList) {
        int max = 0;
        int sum = 0;
        int numOfPos = 0;
        String maxClass = null;
        for (String classifi : attrList.classList()) {
            final int num = numberOf(attrList, null, null, classifi);
            sum += num;
            if (classifi.equals(ExampleGetter.Y)) {
                numOfPos = num;
            }

            if (max < num) {
                max = num;
                maxClass = classifi;
            }
        }

        // M-estimate to calculate probability.
        assert priorPosProb != -1;
        double posProb = (numOfPos + priorPosProb) / (sum + 1);
        return new ClassAndPosProb(maxClass, posProb);
    }

    /**
     * public ExampleSet subExampleSet (Attribute attr, String valueFilter)
     * 
     * @return: subExampleList with examples which have specified value of
     *          attribute.
     */
    public final ExampleSet subExampleSet (final AttributeList attrList,
            final Attribute attr, final String valueFilter) {
        assert attr.valueList().contains(valueFilter);
        final int attrIndex = attrList.indexOf(attr);
        assert attrIndex != -1;
        final ExampleSet subExampleList = new ExampleSet();
        for (Example example : eSet) {
            if (example.checkValue(attrIndex, valueFilter)) {
                subExampleList.add(example);
            }
        }
        // Set the prior prob of pos of subset the same to full set.
        assert this.priorPosProb != -1;
        subExampleList.priorPosProb = this.priorPosProb;
        return subExampleList;
    }

    /**
     * public int numberOf (AttributeList attrList, Attribute attr, String
     * valueFilter, String classFilter)
     * 
     * Return number of examples. Last 3 parameters are filters, can be
     * null.
     * 
     * @return: number of examples with specified attribute, value of the
     *          attribute and classification of example.
     */
    public final int numberOf (final AttributeList attrList,
            final Attribute attr, final String valueFilter,
            final String classFilter) {
        if (classFilter == null && valueFilter == null) {
            // No filter, return whole size.
            assert attr == null;
            return eSet.size();
        }

        int sum = 0;
        for (Example example : eSet) {
            boolean isClassEqual = true;
            boolean isValueEqual = true;
            if (classFilter != null) {
                isClassEqual =
                        example.checkValue(attrList.indexOfTarget(),
                                classFilter);
            }

            if (isClassEqual && valueFilter != null) {
                final int index = attrList.indexOf(attr);
                isValueEqual = example.checkValue(index, valueFilter);
            }
            if (isClassEqual && isValueEqual) {
                // Pass all the filters, count it.
                sum++;
            }
        }
        return sum;
    }

    /**
     * public static ExampleSet[] split(ExampleSet exs, double ratio)
     * 
     * Splits given ExampleSet into 2 ExampleSets by given ratio in percentage.
     * The first output ExampleSet has the number of examples of the ratio, the
     * second one has remaining examples. Splits examples randomly.
     * 
     * @return: An array with 2 ExampleSets; null, if given ratio is not between
     *          0 and 1.
     */
    public static ExampleSet[] split (final ExampleSet exs, final double ratio) {
        if (ratio > 1.0 || ratio < 0) {
            return null;
        }
        final Random ran = new Random(); // Randomly split example set.
        final ExampleSet[] exArray = new ExampleSet[2];
        exArray[0] = new ExampleSet();
        exArray[1] = new ExampleSet();

        final int numOfFirst = (int) (exs.size() * ratio);
        final int numOfSecond = exs.size() - numOfFirst;
        int countFirst = 0;
        int countSecond = 0;
        for (Example e : exs.getExampleSet()) {
            if (countFirst >= numOfFirst) {
                // Already added enough examples into exArray[0].
                exArray[1].add(e);
                countSecond++;
            } else if (countSecond >= numOfSecond) {
                // Already added enough examples into exArray[1].
                exArray[0].add(e);
                countFirst++;
            } else {
                if (Double.compare(ran.nextDouble(), ratio) < 0) {
                    // Probability of ratio to add example into exArray[0].
                    exArray[0].add(e);
                    countFirst++;
                } else { // Remain probability for exArray[1].
                    exArray[1].add(e);
                    countSecond++;
                }
            }
        }

        return exArray;
    }
}
