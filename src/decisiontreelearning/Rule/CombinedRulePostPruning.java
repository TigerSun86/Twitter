package decisiontreelearning.Rule;

/**
 * FileName: CombinedRulePostPruning.java
 * @Description: First implements Rule post-pruning, then prunes rule based on
 *               evaluating accuracy of whole rule list.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import util.Dbg;
import decisiontreelearning.DecisionTree.AttributeList;
import decisiontreelearning.DecisionTree.ExampleSet;

public class CombinedRulePostPruning {
    public static final String MODULE = "CRPP";
    public static final boolean DBG = true;

    public static RuleList prune (final RuleList rl, final ExampleSet valSet,
            final AttributeList attributes) {
        // Prune rule first time.
        final RuleList prunedFirst =
                RulePostPruning.prune(rl, valSet, attributes);
        // Prune rule Second time.
        final RuleList prunedSecond =
                pruneRuleList(prunedFirst, valSet, attributes);
        Dbg.print(DBG, MODULE, "Pruned rule list:" + Dbg.NEW_LINE
                + prunedSecond.toString());
        return prunedSecond;
    }

    private static final String DBG_FMT_PRUNED =
            "Pruned: %s from rule: %s, for the accuracy improved from %.3f to %.3f";

    private static RuleList pruneRuleList (final RuleList rlist,
            final ExampleSet valSet, final AttributeList attributes) {
        final RuleList retRL = new RuleList(rlist);
        // Calculate initial whole rule list accuracy before pruning.
        double accuracy = RulePostPruning.evalRule(retRL, valSet, attributes);
        for (Rule r : retRL.list) {
            final Rule rBackup = new Rule(r);
            boolean needMorePruned = true;
            while (needMorePruned) {
                // Stop when last "for loop" has no precondition been pruned.
                needMorePruned = false;
                for (int i = rBackup.preconds.size() - 1; i >= 0; i--) {
                    // Traversal preconditions from last to first, because the
                    // later one is less important.
                    final RuleCondition precond = rBackup.preconds.get(i);
                    // Prune the precond.
                    final int index = r.removePrecond(precond);
                    if (index == -1) { // There's no such precondition.
                        continue; // Next precondition.
                    }
                    // Calculate the whole rule list accuracy.
                    final double newAccuracy =
                            RulePostPruning.evalRule(retRL, valSet, attributes);
                    if (Double.compare(newAccuracy, accuracy) > 0) {
                        // If new accuracy greater than before, keep the
                        // pruning.
                        String dbgStr =
                                String.format(DBG_FMT_PRUNED, precond, rBackup,
                                        accuracy, newAccuracy);
                        Dbg.print(DBG, MODULE, dbgStr);
                        accuracy = newAccuracy; // Update new accuracy.
                        // Need more prune, because there may be more
                        // preconditions need to be pruned if we try from back
                        // again.
                        needMorePruned = true;
                    } else { // else restore the prune
                        r.addPrecond(index, precond);
                    }
                } // End of for (RuleCondition precond : rBackup.preconds) {
            } // End of while (needMorePruned) {
        } // End of for (Rule r : retRL.list) {
        return retRL;
    }
}
