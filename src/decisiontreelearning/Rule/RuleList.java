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
import java.util.Iterator;

import main.ExampleGetter;
import util.Dbg;

import common.ProbPredictor;

import decisiontreelearning.DecisionTree.Attribute;
import decisiontreelearning.DecisionTree.AttributeList;
import decisiontreelearning.DecisionTree.ContinuousAttr;
import decisiontreelearning.DecisionTree.Discretizor;
import decisiontreelearning.DecisionTree.Example;
import decisiontreelearning.DecisionTree.ExampleSet;

public class RuleList extends ProbPredictor {
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

    public void setPosProb (ExampleSet dataSet) {
        ExampleSet exs = new ExampleSet();
        for (Example e : dataSet.getExampleSet()) {
            exs.add(e);
        }
        exs.initPriorPosProb();
        double priorProb = exs.priorPosProb;

        for (Rule r : list) {
            int numOfMatched = 0;
            int numOfPos = 0;
            Iterator<Example> iter = exs.getExampleSet().iterator();
            while (iter.hasNext()) {
                Example e = iter.next();
                boolean matched = r.match(e.getExample(), newAttrList);
                if (matched) { // Matched rule.
                    numOfMatched++;
                    if (e.get(e.size() - 1).equals(ExampleGetter.Y)) {
                        numOfPos++; // Is pos example.
                    }
                    iter.remove();// Remove example when the rule matched it.
                }
            } // while (iter.hasNext()) {

            // Pos prob by m-estimate.
            double prob = (numOfPos + priorProb) / (numOfMatched + 1);
            r.setPosProb(prob);
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
    public double predictPosProb (ArrayList<String> attrs) {
        final ArrayList<String> newValues = discretizeValues(attrs);
        for (Rule r : this.list) {
            if (r.match(newValues, newAttrList)) {
                return r.posProb;
            }
        } // End of for (Rule r : rl.list) {
        assert false;
        return 0;
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
}
