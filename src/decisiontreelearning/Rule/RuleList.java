package decisiontreelearning.Rule;

/**
 * FileName: RuleList.java
 * @Description: RuleList structure, a set of rules.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;

import util.Dbg;
import common.Hypothesis;
import decisiontreelearning.DecisionTree.Attribute;
import decisiontreelearning.DecisionTree.AttributeList;
import decisiontreelearning.DecisionTree.ContinuousAttr;
import decisiontreelearning.DecisionTree.Discretizor;

public class RuleList implements Hypothesis{
    public final ArrayList<Rule> list;

    public AttributeList oldAttrList; 
    public AttributeList newAttrList; 
    public Discretizor discretizor;
    public String defaultPre = null;
    
    public RuleList() {
        list = new ArrayList<Rule>();
    }

    public RuleList(RuleList rlist) {
        // New list need new copy of each rule. Because the element inside the
        // rule might be changed later, so there will be problem if just
        // keep the copy of address of the rule.
        list = new ArrayList<Rule>();
        for (Rule r : rlist.list) {
            list.add(new Rule(r));
        }
    }

    @Override
    public String toString () {
        final StringBuffer sb = new StringBuffer();
        for (Rule r : list) {
            sb.append(r.toString());
            sb.append(Dbg.NEW_LINE);
        }
        return sb.toString();
    }

    @Override
    public String predict (ArrayList<String> attrs) {
        final ArrayList<String> newValues = new ArrayList<String>();
        // Convert each attribute value into several discrete attributes.
        for (int index = 0; index < oldAttrList.size()-1; index++) {
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
        
        for (Rule r : this.list) {
            // If the rule r has no precondition, it mean no matter what
            // value example has, it will be accepted by this rule.
            boolean accepted = true;
            for (RuleCondition precond : r.preconds) {
                final String attrName = precond.name;
                // Get value of the attribute from test example
                final int attrIndex =
                        newAttrList.indexOf(newAttrList.get(attrName));
                final String value = newValues.get(attrIndex);
                if (!value.equals(precond.value)) {
                    accepted = false; // Violated a precondition.
                    break; // Check next rule.
                }
            } // End of for (RuleCondition precond : r.preconds) {
            if (accepted) {
                return r.postcond.value;
            }
        } // End of for (Rule r : rl.list) {
        assert false;
        return defaultPre;
    }

}
