package decisiontreelearning.DecisionTree;

/**
 * FileName: AttributeList.java
 * @Description: List for all attributes, the last one is target. Has methods to
 *               enable and disable specific attribute, instead of actual
 *               deleting.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.HashMap;

public class AttributeList {
    // Store attributes and target(in last position).
    private final ArrayList<Attribute> aList;
    private final HashMap<Attribute, Integer> indexMap;
    private final ArrayList<Boolean> validFlags;
    // The target index, always the last one in the list.
    private int targetIndex;

    public AttributeList() {
        this.aList = new ArrayList<Attribute>();
        this.indexMap = new HashMap<Attribute, Integer>();
        this.validFlags = new ArrayList<Boolean>();
        this.targetIndex = -1;
    }

    public final void add (final Attribute attr) {
        if (aList.contains(attr)) {
            // Do not add redundant attribute.
            return;
        }
        aList.add(attr);
        targetIndex++;
        indexMap.put(attr, targetIndex);
        validFlags.add(true);
    }

    public final Attribute get (final int index) {
        return aList.get(index);
    }

    /**
     * public final Attribute get (String attrName)
     * @return: an attribute with the specified attribute name.
     */
    public final Attribute get (final String attrName) {
        for (Attribute attr : aList) {
            if (attrName.equals(attr.getName())) {
                return attr;
            }
        }
        return null;
    }

    public final Attribute getTarget () {
        return aList.get(targetIndex);
    }

    public final int indexOf (final Attribute attr) {
        return indexMap.get(attr);
    }

    public final int indexOfTarget () {
        return targetIndex;
    }

    public final ArrayList<String> classList () {
        final Attribute target = getTarget();
        return target.valueList();
    }

    /**
     * public final int size ()
     * Returns int value with the number of all attributes and target.
     * @return: the number of all attributes and target.
     */
    public final int size () {
        return aList.size();
    }

    /**
     * public final int validSize ()
     * Returns int value with the number of valid attributes.
     * Not include target.
     * @return: the number of valid attributes.
     */
    public final int validSize () {
        int sum = 0;
        for (boolean valid : validFlags) {
            if (valid) {
                sum++;
            }
        }
        // Subtract the target, which is not a attribute, and is always valid.
        sum = sum - 1;
        return sum;
    }

    /**
     * public final boolean isAllDisabled ()
     * @return: true if all attributes were disabled.
     */
    public final boolean isAllDisabled () {
        return validSize() == 0;
    }

    /**
     * public final ArrayList<Attribute> validList ()
     * Returns a list with all valid attributes. The attribute do not contain
     * target.
     * @return: valid attributes list.
     */
    public final ArrayList<Attribute> validList () {
        final ArrayList<Attribute> validList = new ArrayList<Attribute>();
        // Do not count in target.
        for (int i = 0; i < targetIndex; i++) {
            if (validFlags.get(i)) {
                validList.add(aList.get(i));
            }
        }
        return validList;
    }

    /**
     * public final void enable (Attribute attr)
     * Virtually recover one attribute.
     */
    public final void enable (final Attribute attr) {
        final int index = indexOf(attr);
        // Should not enable or disable target.
        assert index != targetIndex;
        validFlags.set(index, true);
    }

    /**
     * public final void enableAll ()
     * Virtually recover all attributes.
     */
    public final void enableAll () {
        for (int i = 0; i < validFlags.size(); i++) {
            validFlags.set(i, true);
        }
    }

    /**
     * public final void disable (Attribute attr)
     * Virtually delete one attribute.
     */
    public final void disable (final Attribute attr) {
        final int index = indexOf(attr);
        // Should not enable or disable target.
        assert index != targetIndex;
        validFlags.set(index, false);
    }

    /**
     * public final String toString ()
     * Returns a String with only valid attributes. No disabled attributes, no
     * target.
     * @return: a String with only valid attributes
     */
    @Override
    public final String toString () {
        final StringBuffer sBuff = new StringBuffer();
        sBuff.append("[");
        for (Attribute attr : validList()) {
            sBuff.append(attr.toString());
        }
        sBuff.append("]");
        return sBuff.toString();
    }
}
