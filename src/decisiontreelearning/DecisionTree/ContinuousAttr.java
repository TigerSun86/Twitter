package decisiontreelearning.DecisionTree;

/**
 * FileName: ContinuousAttr.java
 * @Description: Thresholds for one continuous attribute.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import util.MyMath;

public class ContinuousAttr {
    public static final String MODULE = "CA";
    public static final boolean DBG = false;

    public final String name;
    public final ArrayList<Double> thresholds;

    public ContinuousAttr(final ExampleSet eSet, final AttributeList attrList,
            final int index) {
        name = attrList.get(index).getName();
        thresholds = getThresholds(eSet, attrList, index);
    }

    @Override
    public String toString () {
        return name + thresholds.toString();
    }

    private static class ExampleComparator implements Comparator<Example> {
        private final int index;

        /**
         * public ExampleComparator(final int index)
         * 
         * Decide the attribute of specific index for comparing.
         * */
        public ExampleComparator(final int index) {
            this.index = index;
        }

        @Override
        public int compare (final Example e1, final Example e2) {
            final double d1 = Double.parseDouble(e1.get(index));
            final double d2 = Double.parseDouble(e2.get(index));
            return Double.compare(d1, d2);
        }
    }

    private static ArrayList<Double> getThresholds (final ExampleSet eSet,
            final AttributeList attrList, final int index) {
        final ArrayList<Double> retThres = new ArrayList<Double>();

        // Sort example set in ascending order.
        final ExampleComparator comparator = new ExampleComparator(index);
        final PriorityQueue<Example> sortedESet =
                new PriorityQueue<Example>(eSet.size(), comparator);
        sortedESet.addAll(eSet.getExampleSet());
        dbgSortedESet(sortedESet, attrList, index);
        // Traversal example set.
        Example curEg = sortedESet.remove();
        double lastLastValue = Double.NaN;
        double lastValue = Double.parseDouble(curEg.get(index));
        int maxFLen = MyMath.getFractionLength(curEg.get(index));
        String lastClass = curEg.get(attrList.indexOfTarget());
        boolean isMixClass = false;
        while (!sortedESet.isEmpty()) {
            curEg = sortedESet.remove();
            final double curValue = Double.parseDouble(curEg.get(index));
            maxFLen =
                    Math.max(maxFLen,
                            MyMath.getFractionLength(curEg.get(index)));
            final String curClass = curEg.get(attrList.indexOfTarget());
            if (Double.compare(lastValue, curValue) == 0) {
                // Encounter consequent same value.
                if (!isMixClass) {
                    // Haven'tList encountered different class for this value.
                    if (!lastClass.equals(curClass)) {
                        // Encounter a different class for the value.
                        isMixClass = true;
                        if (Double.isNaN(lastLastValue)) {
                            addThreshold(retThres, lastLastValue, curValue,
                                    maxFLen);
                        }
                    }
                } // End of if (!mixClass) {
            } else { // The value is different from last one.
                if (isMixClass) {
                    // Last value has many examples with multiple classes.
                    addThreshold(retThres, lastValue, curValue, maxFLen);
                } else {
                    if (!lastClass.equals(curClass)) {
                        // Current value has different class than last one.
                        addThreshold(retThres, lastValue, curValue, maxFLen);
                    }
                }
                // Update last value.
                lastLastValue = lastValue;
                lastValue = curValue;
                lastClass = curClass;
                isMixClass = false;
            }
        }
        if (retThres.isEmpty()) {
            // If there is no class changed in all examples, just add a
            // threshold after biggest value.
            addThreshold(retThres, lastValue, lastValue + 2, maxFLen);
        }
        return retThres;
    }

    private static boolean addThreshold (final ArrayList<Double> thresList,
            final double n1, final double n2, int maxFLen) {
        // Add a threshold with a value between n1 and n2.
        double threshold = (n1 + n2) / 2;
        // Just keep maxFLen + 1 digits fraction.
        threshold = MyMath.doubleRound(threshold, maxFLen + 1);
        if (thresList.isEmpty()) {
            thresList.add(threshold);
            return true;
        } else {
            final double lastThreshold = thresList.get(thresList.size() - 1);
            if (Double.compare(lastThreshold, threshold) != 0) {
                thresList.add(threshold);
                return true;
            } else { // The threshold has been added last time.
                return false;
            }
        }
    }

    private static void dbgSortedESet (PriorityQueue<Example> sortedESet,
            final AttributeList attrList, int index) {
        if (DBG) {
            System.out.println(MODULE + ": Sorted examples:");
            PriorityQueue<Example> s = new PriorityQueue<Example>(sortedESet);
            while (!s.isEmpty()) {
                Example e = s.remove();
                System.out.println(attrList.get(index).getName() + " "
                        + e.get(index) + " " + e.get(attrList.indexOfTarget()));
            }
        }
    }
}
