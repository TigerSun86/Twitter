package decisiontreelearning.DecisionTree;

/**
 * FileName: DecisionTree.java
 * @Description: A tree structure for decision tree learning.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import util.Dbg;

import common.ProbPredictor;

public class DecisionTree extends ProbPredictor {
    public AttributeList oldAttrList;
    public AttributeList newAttrList;
    public Discretizor discretizor;

    // Root is an attribute or a class.
    private String root;
    private double posProb;
    private DecisionTree parent;
    private String valueOfParent;

    // The first is a key, "a value of one attribute".
    // The second is a subtree.
    private HashMap<String, DecisionTree> branches;

    /** For non-leaf node */
    public DecisionTree(final String root2) {
        this.root = root2;
        this.posProb = -1;
        // Default parent is null. Call setParent() if needed.
        this.parent = null;
        this.valueOfParent = null;
        this.branches = new HashMap<String, DecisionTree>();
    }

    /** For leaf node */
    public DecisionTree(final String className, double prob) {
        this.root = className;
        this.posProb = prob;
        // Default parent is null. Call setParent() if needed.
        this.parent = null;
        this.valueOfParent = null;
        this.branches = new HashMap<String, DecisionTree>();
    }

    private ArrayList<String> discretizeValues (ArrayList<String> attrs) {
        final ArrayList<String> newValues = new ArrayList<String>();
        // Convert each attribute value into several discrete attributes.
        for (int index = 0; index < oldAttrList.size() - 1; index++) {

            final Attribute attr = oldAttrList.get(index);
            final String name = attr.getName();
            final ContinuousAttr ca = discretizor.get(name);
            if (ca == null) {
                // This attribute is not continuous, just keep it.
                // Target will be in this case.
                newValues.add(attrs.get(index));
            } else { // Continuous attribute.
                final double value = Double.parseDouble(attrs.get(index));
                for (double threshold : ca.thresholds) {
                    if (Double.compare(value, threshold) > 0) {
                        // Bigger than threshold, value is true.
                        newValues.add(Discretizor.VALUET);
                    } else {// Otherwise is false.
                        newValues.add(Discretizor.VALUEF);
                    }
                }
            }
        } // End of for (int index = 0; index < attrList.size(); index++) {
        return newValues;
    }

    private DecisionTree findLeaf (ArrayList<String> newValues) {
        DecisionTree subTree = this;
        // Hit the leaf of decision tree according to the example.
        while (!subTree.isLeaf()) {
            // Get current attribute from tree.
            final String attrName = subTree.getRoot();
            // Get value of the attribute from test example
            final int attrIndex =
                    newAttrList.indexOf(newAttrList.get(attrName));
            final String value = newValues.get(attrIndex);
            // Get the sub tree of the tree according to the value.
            subTree = subTree.getSubTree(value);
        }
        // The sub tree is a leaf.
        return subTree;
    }

    @Override
    public double predictPosProb (ArrayList<String> attrs) {
        final ArrayList<String> newValues = discretizeValues(attrs);
        DecisionTree leaf = findLeaf(newValues);
        return leaf.posProb;
    }

    public final String getRoot () {
        return root;
    }

    public final void setRoot (final String root2) {
        this.root = root2;
    }

    public final double getPosProb () {
        return this.posProb;
    }

    public final void setPosProb (final double prob) {
        this.posProb = prob;
    }

    public final DecisionTree getParent () {
        return parent;
    }

    public final boolean hasParent () {
        return parent != null;
    }

    public final void setParent (final DecisionTree parent2) {
        this.parent = parent2;
    }

    public String getValueOfParent () {
        return valueOfParent;
    }

    public void setValueOfParent (final String valueOfParent2) {
        this.valueOfParent = valueOfParent2;
    }

    public final boolean isLeaf () {
        return branches.isEmpty();
    }

    public final Set<Entry<String, DecisionTree>> branchSet () {
        return branches.entrySet();
    }

    /**
     * Here "value" means "the value of the attribute", but not the HashMap
     * value."
     */
    public final Set<String> valueSet () {
        return branches.keySet();
    }

    public final Collection<DecisionTree> subTreeSet () {
        return branches.values();
    }

    /**
     * Here "value" means "the value of the attribute", but not the HashMap
     * value."
     */
    public final DecisionTree getSubTree (final String value) {
        return branches.get(value);
    }

    /**
     * Here "value" means "the value of the attribute", but not the HashMap
     * value."
     */
    public final void
            addBranch (final String value, final DecisionTree subTree) {
        branches.put(value, subTree);
    }

    @Override
    public final String toString () {
        return print("", true, 10);
    }

    private String print (String prefix, boolean isTail, int d) {
        if (d == 0) {
            return "";
        }
        final StringBuffer sb = new StringBuffer();
        final String probStr =
                (this.isLeaf() ? String.format(" %2.2f%%", posProb) : "");
        sb.append(prefix + (isTail ? "+-- " : "+-- ") + root + probStr
                + Dbg.NEW_LINE);
        final Collection<Entry<String, DecisionTree>> branches = branchSet();
        int counter = 0;
        for (Entry<String, DecisionTree> childBranch : branches) {
            sb.append(prefix + (isTail ? "    " : "|   ") + "|"
                    + childBranch.getKey() + Dbg.NEW_LINE);
            counter++;
            if (counter < branches.size()) {
                sb.append(childBranch.getValue().print(
                        prefix + (isTail ? "    " : "|   "), false, d - 1));
            } else { // Last child.
                sb.append(childBranch.getValue().print(
                        prefix + (isTail ? "    " : "|   "), true, d - 1));
            }
        }
        return sb.toString();
    }
}
