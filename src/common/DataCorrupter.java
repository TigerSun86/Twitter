package common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import util.Dbg;

/**
 * FileName: DataCorrupter.java
 * @Description: Corrupt example set by given ratio.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Mar 18, 2014 1:15:10 PM
 */
public class DataCorrupter {
    public static final String MODULE = "DCRP";
    public static final boolean DBG = false;

    public static RawExampleList corrupt (final RawExampleList examples,
            final RawAttrList attrList, final double ratio) {
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
        final RawExampleList corEgs = new RawExampleList();
        int index = 0;
        for (RawExample e : examples) {
            final RawExample newExample;
            if (corIndexes.contains(index)) { // Corrupt it.
                newExample = new RawExample();
                newExample.xList = e.xList; // Keep attributes the same.
                // Corrupt target value.
                final ArrayList<String> classList = attrList.t.valueList;
                final String classOfEg = e.t;
                String corClass = null;
                while (corClass == null || classOfEg.equals(corClass)) {
                    // Randomly choose another class.
                    corClass = classList.get(ran.nextInt(classList.size()));
                }
                newExample.t = corClass; // add corrupted target value.
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
