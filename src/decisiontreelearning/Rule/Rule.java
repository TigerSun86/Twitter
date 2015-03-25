package decisiontreelearning.Rule;

/**
 * FileName: Rule.java
 * @Description: Rule structure, in the format of
 *               "IF precondition1 and precondition2 and ... THEN postcondition"
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;

import decisiontreelearning.DecisionTree.AttributeList;

public class Rule {
    public final ArrayList<RuleCondition> preconds;
    public RuleCondition postcond;
    public double posProb = -1;

    public Rule() {
        preconds = new ArrayList<RuleCondition>();
    }

    public Rule(Rule r) {
        preconds = new ArrayList<RuleCondition>(r.preconds);
        postcond = r.postcond;
    }

    public void setPostcond (final RuleCondition postcond) {
        this.postcond = postcond;
    }

    public RuleCondition getPostcond () {
        return postcond;
    }

    public void addPrecond (final RuleCondition precond) {
        preconds.add(precond);
    }

    /**
     * public void addPrecond(final int index, final RuleCondition precond)
     * 
     * Inserts the specified precondition at the specified position in the list.
     * Usually for restoring the removed preconditions.
     */
    public void addPrecond (final int index, final RuleCondition precond) {
        preconds.add(index, precond);
    }

    /**
     * public int removePrecond(final RuleCondition precond)
     * 
     * Removes the first occurrence of the specified element from this list.
     * Return the index of it, so it can be restored in future by method:
     * addPrecond(int index, RuleCondition precond)
     * 
     * @Returns: the index of the first occurrence of the specified element in
     *           this list, or -1 if this list does not contain the element
     */
    public int removePrecond (final RuleCondition precond) {
        final int index = preconds.indexOf(precond);
        preconds.remove(precond);
        return index;
    }

    public boolean isEmpty () {
        return preconds.isEmpty();
    }

    public boolean contain (RuleCondition precond) {
        return preconds.contains(precond);
    }

    public boolean match (ArrayList<String> values, AttributeList attrList) {
        // If the rule r has no precondition, it mean no matter what
        // value example has, it will be accepted by this rule.
        boolean accepted = true;
        for (RuleCondition precond : this.preconds) {
            final String attrName = precond.name;
            // Get value of the attribute from test example
            final int attrIndex = attrList.indexOf(attrList.get(attrName));
            final String value = values.get(attrIndex);
            if (!value.equals(precond.value)) {
                accepted = false; // Violated a precondition.
                break; // Check next rule.
            }
        } // End of for (RuleCondition precond : r.preconds) {
        return accepted;
    }

    public void setPosProb (double prob) {
        this.posProb = prob;
    }

    @Override
    public String toString () {
        final StringBuffer sb = new StringBuffer();
        sb.append(String.format("%2.2f%% ", posProb));
        sb.append("IF ");
        for (RuleCondition p : preconds) {
            sb.append("(");
            sb.append(p.toString());
            sb.append(")");
            sb.append("&&");
        }
        if (!preconds.isEmpty()) { // Delete redundant "&&" at the end.
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(" THEN ");
        sb.append("Class=");
        sb.append(postcond.value.toString());

        return sb.toString();
    }

}
