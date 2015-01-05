package decisiontreelearning.DecisionTree;

/**
 * FileName: Discretizor.java
 * @Description: Discretize the continuous example set and attribute.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;

import util.Dbg;

public class Discretizor {
    public static final String MODULE = "DCT";
    public static final boolean DBG = true;

    public static final String VALUET = "T";
    public static final String VALUEF = "F";
    public static final String CONT = "continuous";

    public final ArrayList<ContinuousAttr> contiAttrs;

    public Discretizor(final ExampleSet eSet, final AttributeList attrList) {
        contiAttrs = new ArrayList<ContinuousAttr>();
        for (int index = 0; index < attrList.size(); index++) {
            // Traversal all attributes and target.
            final Attribute attr = attrList.get(index);
            final ArrayList<String> valueList = attr.valueList();
            // It's a continuous attribute if it has just one value which is
            // "continuous".
            if (valueList.size() == 1
                    && (valueList.get(0)).equalsIgnoreCase(CONT)) {
                final ContinuousAttr ca =
                        new ContinuousAttr(eSet, attrList, index);
                contiAttrs.add(ca);
                Dbg.print(DBG, MODULE, "Attribute thresholds: " + ca.toString());
            }
        }
    }

    /**
     * public static ExampleSet DiscretizeExampleSet (ExampleSet
     * eSet,AttributeList oldAttrList, Discretizor discretizor)
     * 
     * The AttributeList should be the raw attributes without discretizing.
     * */
    public static ExampleSet DiscretizeExampleSet (final ExampleSet eSet,
            final AttributeList oldAttrList, final Discretizor discretizor) {
        final ExampleSet newESet = new ExampleSet();
        // Convert each example into discrete.
        for (Example example : eSet.getExampleSet()) {
            final Example newExample = new Example();
            // Convert each attribute value into several discrete attributes.
            for (int index = 0; index < oldAttrList.size(); index++) {

                final Attribute attr = oldAttrList.get(index);
                final String name = attr.getName();
                final ContinuousAttr ca = discretizor.get(name);
                if (ca == null) {
                    // This attribute is not continuous, just keep it.
                    // Target will be in this case.
                    newExample.add(example.get(index));
                } else { // Continuous attribute.
                    final double value = Double.parseDouble(example.get(index));
                    for (double threshold : ca.thresholds) {
                        if (Double.compare(value, threshold) > 0) {
                            // Bigger than threshold, value is true.
                            newExample.add(VALUET);
                        } else {// Otherwise is false.
                            newExample.add(VALUEF);
                        }
                    }
                }
            } // End of for (int index = 0; index < attrList.size(); index++) {
              // Add the converted example into new ExampleSet.
            newESet.add(newExample);
        }
        return newESet;
    }

    public static AttributeList DiscretizeAttrList (
            final AttributeList attrList, final Discretizor discretizor) {
        final AttributeList newAttrList = new AttributeList();
        // Convert each attribute into several discrete attributes.
        for (int index = 0; index < attrList.size(); index++) {
            // Traversal all attributes and target.
            final Attribute attr = attrList.get(index);
            final String name = attr.getName();
            final ContinuousAttr ca = discretizor.get(name);
            if (ca == null) {
                // This attribute is not continuous, just keep it.
                // Target will be in this case.
                newAttrList.add(attr);
            } else { // Continuous attribute.
                // Need number of attributes as number of thresholds
                for (double threshold : ca.thresholds) {
                    final String newName = name + ">" + threshold;
                    final Attribute newAttr = new Attribute(newName);
                    newAttr.add(VALUET);
                    newAttr.add(VALUEF);
                    newAttrList.add(newAttr);
                }
            }
        } // End of for (int index = 0; index < attrList.size(); index++) {
        return newAttrList;
    }

    public ContinuousAttr get (String name) {
        for (ContinuousAttr ca : contiAttrs) {
            if (ca.name.equals(name)) {
                return ca;
            }
        }
        return null;
    }
}
