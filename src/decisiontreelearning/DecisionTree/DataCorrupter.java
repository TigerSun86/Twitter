package decisiontreelearning.DecisionTree;

/**
 * FileName: DataCorrupter.java
 * @Description: Corrupt example set by given ratio.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import util.Dbg;

public class DataCorrupter {
    public static final String MODULE = "DCRP";
    public static final boolean DBG = false;

    public static ExampleSet corrupt (final ExampleSet examples,
            final AttributeList attrList, final double ratio) {
        if (Double.compare(ratio, 0) <= 0 || Double.compare(ratio, 1) > 0) {
            // Don'tList corrupt example set if ratio equals to 0 or illegal.
            return examples;
        }

        final int total = examples.size();
        final int corNum = (int) Math.round(total * ratio);
        final HashSet<Integer> corIndexes = new HashSet<Integer>();
        final Random ran = new Random();
        while (corIndexes.size() != corNum) {
            // Generate all indexes about to be corrupted.
            corIndexes.add(ran.nextInt(total));
        }
        final ExampleSet corEgs = new ExampleSet();
        int index = 0;
        for (Example e : examples.getExampleSet()) {
            final Example newExample;
            if (corIndexes.contains(index)) { // Corrupt it.
                newExample = new Example();
                for (int i = 0; i < attrList.indexOfTarget(); i++) {
                    newExample.add(e.get(i)); // Add attribute values.
                }
                // Corrupt target value.
                final ArrayList<String> classList = attrList.classList();
                final String classOfEg = e.get(attrList.indexOfTarget());
                String corClass = null;
                while (corClass == null || classOfEg.equals(corClass)) {
                    // Randomly choose another class.
                    corClass = classList.get(ran.nextInt(classList.size()));
                }
                newExample.add(corClass); // add corrupted target value.
                Dbg.print(DBG, MODULE, "Corrupted a example" + Dbg.NEW_LINE
                        + "\tfrom " + e + Dbg.NEW_LINE + "\tto   " + newExample);
            } else { // Don'tList corrupt it.
                newExample = e;
            }
            corEgs.add(newExample);
            index++;
        } // for (Example e : examples.getExampleSet())
        Dbg.print(DBG, MODULE, "Corrupted number: " + corIndexes.size()
                + ", examples number: " + examples.size() + ", ratio: " + ratio);
        return corEgs;
    }
}
