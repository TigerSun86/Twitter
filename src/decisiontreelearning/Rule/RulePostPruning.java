package decisiontreelearning.Rule;

/**
 * FileName: RulePostPruning.java
 * @Description: Rule post-pruning, prunes the rules converted from decision
 *               tree. First prune each rules, then sort rules based on the
 *               accuracy in validation set.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.TreeMap;

import util.Dbg;
import decisiontreelearning.DecisionTree.AttributeList;
import decisiontreelearning.DecisionTree.Example;
import decisiontreelearning.DecisionTree.ExampleSet;

public class RulePostPruning {
    public static final String MODULE = "RPP";
    public static final boolean DBG = true;

    public static double evalRule (final RuleList rl, final ExampleSet testSet,
            final AttributeList attributes) {
        int hit = 0;
        for (Example example : testSet.getExampleSet()) {
            for (Rule r : rl.list) {
                // If the rule r has no precondition, it mean no matter what
                // value example has, it will be accepted by this rule.
                boolean accepted = true;
                for (RuleCondition precond : r.preconds) {
                    final String attrName = precond.name;
                    // Get value of the attribute from test example
                    final int attrIndex =
                            attributes.indexOf(attributes.get(attrName));
                    final String value = example.get(attrIndex);
                    if (!value.equals(precond.value)) {
                        accepted = false; // Violated a precondition.
                        break; // Check next rule.
                    }
                } // End of for (RuleCondition precond : r.preconds) {
                if (accepted) {
                    final String classOfExam =
                            example.get(attributes.indexOfTarget());
                    if (r.postcond.value.equals(classOfExam)) {
                        hit++; // Predict correctly.
                    }
                    break; // Check next example.
                }
            } // End of for (Rule r : rl.list) {
        } // End of for (Example example : testSet.getExampleSet()) {
        final int sum = testSet.size();
        return ((double) hit) / sum;
    }

    /**
     * public static double evalOneRule(Rule r, ExampleSet testSet,
     * AttributeList attributes)
     * 
     * @return The accuracy of the rule in the example set; Double.NaN, if there
     *         is no example in set can fit the rule.
     */
    public static double evalOneRule (final Rule r, final ExampleSet testSet,
            final AttributeList attributes) {
        int predictCorrect = 0;
        int predictWrong = 0;
        for (Example example : testSet.getExampleSet()) {
            // If the rule r has no precondition, it mean no matter what
            // value example has, it will be accepted by this rule.
            boolean accepted = true;
            for (RuleCondition precond : r.preconds) {
                final String attrName = precond.name;
                // Get value of the attribute from test example
                final int attrIndex =
                        attributes.indexOf(attributes.get(attrName));
                final String value = example.get(attrIndex);
                if (!value.equals(precond.value)) {
                    accepted = false; // Violated a precondition.
                    break; // Check next example.
                }
            } // End of for (RuleCondition precond : r.preconds) {
            if (accepted) {
                final String classOfExam =
                        example.get(attributes.indexOfTarget());
                if (r.postcond.value.equals(classOfExam)) {
                    predictCorrect++; // Predict correctly.
                } else {
                    predictWrong++;
                }
            }
        } // End of for (Example example : testSet.getExampleSet()) {
        final int sum = predictCorrect + predictWrong;
        if (sum == 0) { // No example fits the rule.
            return Double.NaN;
        } else {
            return ((double) predictCorrect) / (predictCorrect + predictWrong);
        }
    }

    public static RuleList prune (final RuleList rl, final ExampleSet valSet,
            final AttributeList attributes) {
        final TreeMap<Double, ArrayList<Rule>> prunedRules =
                pruneRuleList(rl, valSet, attributes);
        final RuleList sortedRuleList = getSortedRuleList(prunedRules);
        return sortedRuleList;
    }

    private static TreeMap<Double, ArrayList<Rule>> pruneRuleList (
            final RuleList rlist, final ExampleSet valSet,
            final AttributeList attributes) {
        // Sort pruned rule with descending TreeMap.
        final TreeMap<Double, ArrayList<Rule>> treeMap =
                new TreeMap<Double, ArrayList<Rule>>(Collections.reverseOrder());
        for (Rule r : rlist.list) {
            final ruleAndAccur prunedRule = pruneOneRule(r, valSet, attributes);
            // Store the rule and accuracy in descending tree map
            ArrayList<Rule> al = treeMap.get(prunedRule.accuracy);
            if (al == null) { // Many rules may have the same accuracy.
                al = new ArrayList<Rule>();
                al.add(prunedRule.r);
                treeMap.put(prunedRule.accuracy, al);
            } else {
                al.add(prunedRule.r);
            }
        } // End of for (Rule r : rlist.list) {
        return treeMap;
    }

    private static final String DBG_FMT_ACCU =
            "Rule after sorting: %s Accuracy:  %.3f";

    private static RuleList getSortedRuleList (
            final TreeMap<Double, ArrayList<Rule>> treeMap) {
        // Get all rules out of tree map in order into new Rule list.
        final RuleList sortedRuleList = new RuleList();
        for (Entry<Double, ArrayList<Rule>> e : treeMap.entrySet()) {
            final double accuracy = e.getKey();
            final ArrayList<Rule> al = e.getValue();
            for (Rule r : al) {
                sortedRuleList.list.add(r);
                String dbgStr = String.format(DBG_FMT_ACCU, r, accuracy);
                Dbg.print(DBG, MODULE, dbgStr);
            }
        }
        return sortedRuleList;
    }

    private static class ruleAndAccur {
        public Rule r;
        public double accuracy;

        public ruleAndAccur(Rule r, double accuracy) {
            this.r = r;
            this.accuracy = accuracy;
        }
    }

    private static final String DBG_FMT_PRUNED =
            "Pruned: %s from rule: %s, for the accuracy improved from %.3f to %.3f";

    private static ruleAndAccur pruneOneRule (final Rule r,
            final ExampleSet valSet, final AttributeList attributes) {
        // I'm going to remove preconds from r, so backup it first.
        final Rule rBackup = new Rule(r);
        // Calculate initial accuracy in valset before pruning.
        double accuracy = evalOneRule(r, valSet, attributes);
        if (Double.isNaN(accuracy)) {
            // The rule fits no example in validation set, so don'tList prune it,
            // and regard its accuracy as 0.
            return new ruleAndAccur(rBackup, 0);
        }
        boolean needMorePruned = true;
        while (needMorePruned) {
            // Stop when last "for loop" has no precondition been pruned.
            needMorePruned = false;
            for (int i = r.preconds.size() - 1; i >= 0; i--) {
                // Traversal preconditions from last to first, because the later
                // one is less important.
                final RuleCondition precond = r.preconds.get(i);
                // Prune the precond.
                final int index = rBackup.removePrecond(precond);
                if (index == -1) { // There's no such precondition.
                    continue; // Next precondition.
                }
                // Calculate the accuracy in validation set.
                final double newAccuracy =
                        evalOneRule(rBackup, valSet, attributes);
                if (Double.compare(newAccuracy, accuracy) > 0) {
                    // if new accuracy greater than before, keep the
                    // pruning.
                    String dbgStr =
                            String.format(DBG_FMT_PRUNED, precond, r, accuracy,
                                    newAccuracy);
                    Dbg.print(DBG, MODULE, dbgStr);
                    accuracy = newAccuracy; // Update new accuracy.
                    // Need more prune, because there may be more
                    // preconditions need to be pruned if we try from back
                    // again.
                    needMorePruned = true;
                } else { // else restore the prune
                    rBackup.addPrecond(index, precond);
                }
            } // End of for (RuleCondition precond : rBackup.preconds) {
        } // End of while (needMorePruned) {

        return new ruleAndAccur(rBackup, accuracy);
    }
}
