package decisiontreelearning.Rule;

/**
 * FileName: TreeToRule.java
 * @Description: Converts decision tree to rule list.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.ArrayDeque;

import util.Dbg;
import decisiontreelearning.DecisionTree.DecisionTree;

public class TreeToRule {
    public static final String MODULE = "TTR";
    public static final boolean DBG = false;

    public static RuleList convert (final DecisionTree root) {
        final RuleList list = new RuleList();
        final ArrayDeque<DecisionTree> stack = new ArrayDeque<DecisionTree>();
        stack.push(root);
        while (!stack.isEmpty()) {
            final DecisionTree tree = stack.pop();
            if (tree.isLeaf()) {
                // Convert the path to rule.
                final Rule r = pathToRule(tree);
                Dbg.print(DBG, MODULE, "Converted rule:" + r.toString());
                list.list.add(r);
            } else {
                // Expand branches.
                for (DecisionTree subTree : tree.subTreeSet()) {
                    stack.push(subTree);
                }
            }
        }

        return list;
    }

    private static Rule pathToRule (final DecisionTree tree) {
        final Rule r = new Rule();
        r.setPostcond(new RuleCondition(tree.getRoot()));
        // Need "Root to Leaf" order of preconditions here, so use stack to get
        // inverse order.
        final ArrayDeque<RuleCondition> stack = new ArrayDeque<RuleCondition>();
        DecisionTree parent = tree;
        while (parent.hasParent()) { // Leaf to Root backtracking.
            final String valueOfParent = parent.getValueOfParent();
            parent = parent.getParent();
            final RuleCondition precond =
                    new RuleCondition(parent.getRoot(), valueOfParent);
            stack.push(precond);
        }
        while (!stack.isEmpty()) {
            r.addPrecond(stack.pop());
        }
        return r;
    }
}
