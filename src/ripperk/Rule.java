package ripperk;

import java.util.ArrayList;
import java.util.LinkedList;

import common.RawAttrList;

/**
 * FileName: Rule.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Sep 1, 2014 2:31:33 PM
 */
public class Rule extends LinkedList<RuleCondition> {
    private static final long serialVersionUID = 1L;
    public final String prediction;
    public double posProb = -1;

    public Rule(String prediction) {
        this.prediction = prediction;
    }

    /**
     * For clone rule.
     */
    public Rule(final Rule r) {
        super(r);
        this.prediction = r.prediction;
    }

    /**
     * @return Prediction of rule, if all conditions are satisfied; null,
     *         otherwise.
     * */
    public String rulePredict (ArrayList<String> in, RawAttrList attrs) {
        String ret = prediction;
        for (RuleCondition c : this) {
            final String name = c.name;
            final int index = attrs.indexOf(name);
            assert index != -1;
            final String value = in.get(index);
            if (!c.isSatisfied(value)) {
                ret = null; // return null;
                break;
            }
        }

        return ret;
    }

    public void setPosProb (double prob) {
        this.posProb = prob;
    }

    @Override
    public String toString () {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("%2.2f%% ", posProb));
        sb.append("IF ");
        for (RuleCondition p : this) {
            sb.append("(");
            sb.append(p.toString());
            sb.append(")");
            sb.append("&&");
        }
        if (!this.isEmpty()) { // Delete redundant "&&" at the end.
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
        } else {
            sb.append("anything");
        }
        sb.append(" THEN ");
        sb.append("Class=");
        sb.append(prediction);

        return sb.toString();
    }
}
