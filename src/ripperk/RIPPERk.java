package ripperk;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import util.Dbg;
import common.Learner;
import common.RawAttr;
import common.RawAttrList;
import common.RawExample;
import common.RawExampleList;
import common.TrainTestSplitter;

/**
 * FileName: RIPPERk.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Sep 1, 2014 2:23:04 PM
 */
public class RIPPERk implements Learner {
    public static final String MODULE = "RIP";
    public static final boolean DBG = true;

    private final boolean needPrune;
    private final int k;

    public RIPPERk(final boolean needPrune, final int k) {
        this.needPrune = needPrune;
        this.k = k;
    }

    public RIPPERk() {
        this.needPrune = true;
        this.k = 1;
    }

    private static class Node implements Comparable<Node> {
        public final RawExampleList exs;

        public Node(RawExampleList exs) {
            this.exs = exs;
        }

        @Override
        public int compareTo (Node o) { // Ascending order by size.
            return this.exs.size() - o.exs.size();
        }

    }

    public RuleList learn (RawExampleList exs, final RawAttrList attrs) {
        return learn(exs, attrs, this.needPrune, this.k);
    }

    public static RuleList learn (RawExampleList exs, final RawAttrList attrs,
            final boolean needPrune, final int k) {
        final RawExampleList[] subSets =
                TrainTestSplitter.splitSetbyClass(exs, attrs);
        final Node[] ascendingSets = new Node[subSets.length];
        for (int i = 0; i < subSets.length; i++) {
            ascendingSets[i] = new Node(subSets[i]);
        }
        Arrays.sort(ascendingSets);

        // The class has highest # of instances is the default class.
        final String def = ascendingSets[ascendingSets.length - 1].exs.get(0).t;
        final RuleList ruleList = new RuleList(def, attrs);

        // Create rule set for each class, except for last class (default).
        for (int i = 0; i < ascendingSets.length - 1; i++) {
            final RawExampleList pos = new RawExampleList();
            pos.addAll(ascendingSets[i].exs);
            // All remaining classes are neg set.
            final RawExampleList neg = new RawExampleList();
            for (int j = i + 1; j < ascendingSets.length; j++) {
                neg.addAll(ascendingSets[j].exs);
            }
            RuleList subRuleList = learnTwoClass(pos, neg, attrs, needPrune);
            for (int j = 0; j < k; j++) {
                Dbg.print(DBG, MODULE, "Optimization stage " + j);
                // Optimize.
                optimize(subRuleList, pos, neg, attrs, needPrune);
            }

            ruleList.addAll(subRuleList); // Add result to total rule list.
        }

        return ruleList;
    }

    /**
     * ruleList is changed as an output parameter.
     * */
    private static void optimize (RuleList ruleList, RawExampleList pos,
            RawExampleList neg, RawAttrList attrs, final boolean needPrune) {
        // final RuleList ruleList = new RuleList(ruleListIn);
        for (int i = 0; i < ruleList.size(); i++) {
            final Rule original = ruleList.get(i);
            final double dl0 = getDl(ruleList, pos, neg, attrs);
            Dbg.print(DBG, MODULE, "Original is " + original + ", DL:" + dl0);
            learnRuleInWholeSet(ruleList, i, REPLACEMENT, pos, neg, attrs,
                    needPrune);
            final Rule replacement = ruleList.get(i);
            final double dl1 = getDl(ruleList, pos, neg, attrs);
            Dbg.print(DBG, MODULE, "Replacement is " + replacement + ", DL:"
                    + dl1);
            ruleList.set(i, original);
            learnRuleInWholeSet(ruleList, i, REVISION, pos, neg, attrs,
                    needPrune);
            final Rule revision = ruleList.get(i);
            final double dl2 = getDl(ruleList, pos, neg, attrs);
            Dbg.print(DBG, MODULE, "Revision is " + revision + ", DL:" + dl2);

            final double minDl = Math.min(Math.min(dl0, dl1), dl2);
            if (Double.compare(dl0, minDl) == 0) {
                ruleList.set(i, original);
            } else if (Double.compare(dl1, minDl) == 0) {
                ruleList.set(i, replacement);
            } // else {ruleList is already revision one.}
        }

        // Remove examples covered by rule set from pos/neg.
        final RawExampleList posUncovered =
                getUncoveredExs(ruleList, pos, attrs);
        final RawExampleList negUncovered =
                getUncoveredExs(ruleList, neg, attrs);
        // Learn new rules for uncovered examples.
        learnTwoClass(ruleList, posUncovered, negUncovered, attrs, needPrune);

        // Delete any rule if deletion can decrease DL of rule set.
        double dl = getDl(ruleList, pos, neg, attrs);
        for (int i = ruleList.size() - 1; i >= 0; i--) {
            if (ruleList.size() >= 2) {
                final Rule r = ruleList.remove(i);
                double newDl = getDl(ruleList, pos, neg, attrs);
                if (Double.compare(newDl, dl) >= 0) { // No improvement.
                    ruleList.add(i, r); // Recover it.
                } // else {keep the deletion}.
            }
        }
    }

    private static final int REPLACEMENT = 1;
    private static final int REVISION = 2;

    /**
     * ruleList is changed as an output parameter.
     * */
    private static void learnRuleInWholeSet (RuleList ruleList, final int i,
            final int mode, RawExampleList posIn, RawExampleList negIn,
            RawAttrList attrs, final boolean needPrune) {
        final Rule original = ruleList.get(i);
        if (mode == REPLACEMENT) {
            ruleList.remove(i);
        }

        RawExampleList pos = getUncoveredExs(ruleList, posIn, attrs);
        RawExampleList neg = getUncoveredExs(ruleList, negIn, attrs);

        if (mode == REPLACEMENT) {
            ruleList.add(i, original);
        }

        if (!pos.isEmpty()) {
            final RawExampleList[] subPos =
                    TrainTestSplitter.split(pos, attrs, GROW_RATE);
            final RawExampleList growPos = subPos[0];
            final RawExampleList prunePos = subPos[1];
            final RawExampleList[] subNeg =
                    TrainTestSplitter.split(neg, attrs, GROW_RATE);
            final RawExampleList growNeg = subNeg[0];
            final RawExampleList pruneNeg = subNeg[1];

            Rule r;
            if (mode == REPLACEMENT) {
                r = new Rule(original.prediction); // Empty rule.
            } else {
                r = original;
            }

            r = growRule(r, growPos, growNeg, attrs);
            ruleList.set(i, r); // Set it back for pruning.
            if (needPrune) {
                pruneRule2(ruleList, i, prunePos, pruneNeg, attrs);
            }
        }
    }

    private static final double GROW_RATE = 2.0 / 3;

    private static RuleList learnTwoClass (final RawExampleList posIn,
            final RawExampleList negIn, final RawAttrList attrs,
            final boolean needPrune) {
        // The default class setting here is useless, just for format. The upper
        // method will assign default class. Here just using the class of first
        // example in neg set.
        final String def = negIn.get(0).t;
        final RuleList ruleList = new RuleList(def, attrs);
        return learnTwoClass(ruleList, posIn, negIn, attrs, needPrune);
    }

    /**
     * Assume # of pos is less than # of neg.
     * ruleList is changed.
     */
    private static RuleList learnTwoClass (final RuleList ruleList,
            final RawExampleList posIn, final RawExampleList negIn,
            final RawAttrList attrs, final boolean needPrune) {
        RawExampleList pos = new RawExampleList();
        pos.addAll(posIn);
        RawExampleList neg = new RawExampleList();
        neg.addAll(negIn);

        double minMdl = Double.POSITIVE_INFINITY;
        boolean isRunning = true;
        while (!pos.isEmpty() && isRunning) {
            final RawExampleList[] subPos =
                    TrainTestSplitter.split(pos, attrs, GROW_RATE);
            final RawExampleList growPos = subPos[0];
            final RawExampleList prunePos = subPos[1];
            final RawExampleList[] subNeg =
                    TrainTestSplitter.split(neg, attrs, GROW_RATE);
            final RawExampleList growNeg = subNeg[0];
            final RawExampleList pruneNeg = subNeg[1];

            Rule r = growRule(growPos, growNeg, attrs);
            if (needPrune) {
                r = pruneRule(r, prunePos, pruneNeg, attrs);
            }

            ruleList.add(r);

            final double dl = getDl(ruleList, posIn, negIn, attrs);

            if (Double.compare(dl, minMdl) < 0) {
                minMdl = dl;
            }

            if (Double.compare(dl, minMdl + 64) > 0) {
                isRunning = false;
                ruleList.removeLast(); // Remove last rule which has high dl.
            } else {
                // Remove examples covered by rule from pos/neg.
                pos = getUncoveredExs(r, pos, attrs);
                neg = getUncoveredExs(r, neg, attrs);
            }
        }

        return ruleList;
    }

    private static double getDl (RuleList ruleList, RawExampleList pos,
            RawExampleList neg, final RawAttrList attrs) {
        final int tpOfList = getNumOfCovered(pos, attrs, ruleList);
        final int fpOfList = getNumOfCovered(neg, attrs, ruleList);
        final int cover = tpOfList + fpOfList;
        final int uncover = pos.size() + neg.size() - cover;
        final int fnOfList = pos.size() - tpOfList;

        final int fpOfRules = getSumFpForEachRule(neg, attrs, ruleList);
        final int fnOfRules = getSumFnForEachRule(pos, attrs, ruleList);
        final double s1 = s(cover, fpOfRules, ((double) fpOfList) / cover);
        final double s2 = s(uncover, fnOfRules, ((double) fnOfList) / uncover);
        final double log = Math.log(cover + uncover + 1) / LOG_2;
        final double dl = log + s1 + s2;
        assert !Double.isNaN(dl);
        Dbg.print(DBG, MODULE, "DL is " + dl);
        Dbg.print(DBG, MODULE, "For rule set: " + ruleList);

        return dl;
    }

    private static int getSumFpForEachRule (RawExampleList neg,
            RawAttrList attrs, RuleList ruleList) {
        int count = 0;
        for (Rule r : ruleList) {
            final int fp = getNumOfCovered(neg, attrs, r);
            count += fp;
        }
        return count;
    }

    private static int getSumFnForEachRule (RawExampleList pos,
            RawAttrList attrs, RuleList ruleList) {
        int count = 0;
        for (Rule r : ruleList) {
            final int tp = getNumOfCovered(pos, attrs, r);
            count += pos.size() - tp;
        }
        return count;
    }

    private static double s (double n, double k, double p) {
        final double part1 = (k == 0) ? 0 : k * (Math.log(1 / p) / LOG_2);
        final double part2 =
                (n - k == 0) ? 0 : (n - k) * (Math.log(1 / (1 - p)) / LOG_2);
        return part1 + part2;
    }

    private static RawExampleList getUncoveredExs (final Rule r,
            final RawExampleList exs, final RawAttrList attrs) {
        final RawExampleList newExs = new RawExampleList();
        for (int i = 0; i < exs.size(); i++) {
            final String prediction = r.rulePredict(exs.get(i).xList, attrs);
            if (prediction == null) {
                newExs.add(exs.get(i));
            }
        }
        return newExs;
    }

    private static RawExampleList getUncoveredExs (RuleList ruleList,
            RawExampleList exs, RawAttrList attrs) {
        final RawExampleList newExs = new RawExampleList();
        for (int i = 0; i < exs.size(); i++) {
            final String prediction = ruleList.predict(exs.get(i).xList);
            if (prediction.equals(ruleList.defaultPrediction)) {
                newExs.add(exs.get(i)); // Not covered by rule.
            }
        }
        return newExs;
    }

    private static Rule pruneRule (Rule rIn, RawExampleList prunePos,
            RawExampleList pruneNeg, final RawAttrList attrs) {
        Rule lastR = new Rule(rIn);
        double lastV = ruleValueMetric(lastR, prunePos, pruneNeg, attrs);
        if (Double.isNaN(lastV)) { // Cover neither pos nor neg examples.
            return rIn; // It should not be pruned.
        }

        boolean isRunning = true;
        while (isRunning) { // Repeat until no deletion improves the value of v.
            // Delete any final condition sequences.
            // To find the rule with the best value.
            final Rule r = new Rule(lastR);
            double maxV = Double.NEGATIVE_INFINITY;
            Rule bestR = null;
            while (r.size() > 0) { // Do not allow empty rule.
                final double value =
                        ruleValueMetric(r, prunePos, pruneNeg, attrs);
                if (Double.compare(maxV, value) < 0) {
                    maxV = value;
                    bestR = new Rule(lastR);
                }
                r.removeLast();
            }
            assert !Double.isInfinite(maxV);
            if (Double.compare(maxV, lastV) > 0) {
                // The rule after this run has higher value than the rule after
                // last run.
                lastV = maxV;
                lastR = bestR;
            } else { // No improvement in this run.
                isRunning = false;
            }
        }
        Dbg.print(DBG, MODULE, "Rule after pruning " + lastR);
        return lastR;
    }

    /**
     * ruleList is changed as an output parameter.
     * */
    private static void pruneRule2 (RuleList ruleList, int index,
            RawExampleList prunePos, RawExampleList pruneNeg,
            final RawAttrList attrs) {
        Rule lastR = ruleList.get(index);
        if (prunePos.size() == 0 && pruneNeg.size() == 0) {
            return;
        }
        double lastV = ruleValueMetric(ruleList, prunePos, pruneNeg, attrs);

        boolean isRunning = true;
        while (isRunning) { // Repeat until no deletion improves the value of v.
            // Delete any final condition sequences.
            // To find the rule with the best value.
            final Rule r = new Rule(lastR);
            // Will evaluate whole rule set containing this r.
            ruleList.set(index, r);
            double maxV = Double.NEGATIVE_INFINITY;
            Rule bestR = null;
            while (r.size() > 0) { // Do not allow empty rule.
                final double value =
                        ruleValueMetric(ruleList, prunePos, pruneNeg, attrs);
                if (Double.compare(maxV, value) < 0) {
                    maxV = value;
                    bestR = new Rule(lastR);
                }
                r.removeLast();
            }
            assert !Double.isInfinite(maxV);
            if (Double.compare(maxV, lastV) > 0) {
                // The rule after this run has higher value than the rule after
                // last run.
                lastV = maxV;
                lastR = bestR;
            } else { // No improvement in this run.
                isRunning = false;
            }
        }
        Dbg.print(DBG, MODULE, "Rule after pruning " + lastR);
        ruleList.set(index, lastR);
    }

    private static double ruleValueMetric (Rule r, RawExampleList pos,
            RawExampleList neg, final RawAttrList attrs) {
        final int p = getNumOfCovered(pos, attrs, r);
        final int n = getNumOfCovered(neg, attrs, r);
        return ((double) (p - n)) / (p + n);
    }

    private static double ruleValueMetric (RuleList ruleList,
            RawExampleList pos, RawExampleList neg, final RawAttrList attrs) {
        final int tp = getNumOfCovered(pos, attrs, ruleList);
        final int fp = getNumOfCovered(neg, attrs, ruleList);
        return ((double) (tp + neg.size() - fp)) / (pos.size() + neg.size());
    }

    /**
     * For discrete attribute, conditions are a = v1, a = v2... a = vn.
     * Here v1, v2,... vn are discrete values of attribute.
     * 
     * For continuous attribute, conditions are a<=v1, a>=v1, a<=v2, a>=v2,...
     * a<=vn, a>=vn. Here v1, v2,... vn are actual values appear in data set,
     * and v1 < v2 <... <vn. Ordering is convenient for disable them later.
     * */
    private static RuleCondition[][] getAllConditions (
            final RawExampleList pos, final RawExampleList neg,
            final RawAttrList attrs) {
        // Get all possible conditions.
        final RuleCondition[][] conds = new RuleCondition[attrs.xList.size()][];
        for (int i = 0; i < attrs.xList.size(); i++) {
            final RawAttr attr = attrs.xList.get(i);
            if (attr.isContinuous) {
                // Here use TreeMap because: 1, Need to delete duplicated value.
                // 2, Need to sort value ascendingly.
                final TreeMap<Double, String> valueSet =
                        new TreeMap<Double, String>();
                for (RawExample e : pos) {
                    final String value = e.xList.get(i);
                    valueSet.put(Double.parseDouble(value), value);
                }
                for (RawExample e : neg) {
                    final String value = e.xList.get(i);
                    valueSet.put(Double.parseDouble(value), value);
                }

                conds[i] = new RuleCondition[valueSet.size() * 2];
                int j = 0;
                for (Entry<Double, String> entry : valueSet.entrySet()) {
                    final String value = entry.getValue();
                    // Each value has 2 possible conditions: a <= v, a >= v.
                    conds[i][j * 2] =
                            new RuleCondition(attr.name, value,
                                    RuleCondition.OPT_LE);
                    conds[i][j * 2 + 1] =
                            new RuleCondition(attr.name, value,
                                    RuleCondition.OPT_GE);
                    j++;
                }
            } else { // Discrete attribute.
                conds[i] = new RuleCondition[attr.valueList.size()];
                // All possible conditions for this discrete attribute.
                for (int j = 0; j < attr.valueList.size(); j++) {
                    conds[i][j] =
                            new RuleCondition(attr.name, attr.valueList.get(j),
                                    RuleCondition.OPT_EQ);
                }
            }
        }
        return conds;
    }

    private static BitSet[] getAvailConds (final RawExampleList pos,
            final RawExampleList neg, final RuleCondition[][] conds) {
        // Use bit set to record whether the condition is available or not.
        // Each BitSet represents one attribute. Each bit in one BitSet
        // represents one condition of the attribute.
        final BitSet[] availConds = new BitSet[conds.length];
        for (int i = 0; i < availConds.length; i++) {
            availConds[i] = new BitSet(conds[i].length);
        }

        for (RawExample e : pos) {
            for (int i = 0; i < e.xList.size(); i++) {
                final String value = e.xList.get(i);
                for (int j = 0; j < conds[i].length; j++) {
                    // For each sub condition of each attribute.
                    if (conds[i][j].isSatisfied(value)) {
                        availConds[i].set(j);
                    }
                }
            }
        }

        for (RawExample e : neg) {
            for (int i = 0; i < e.xList.size(); i++) {
                final String value = e.xList.get(i);
                for (int j = 0; j < conds[i].length; j++) {
                    // For each sub condition of each attribute.
                    if (conds[i][j].isSatisfied(value)) {
                        availConds[i].set(j);
                    }
                }
            }
        }
        return availConds;
    }

    private static Rule growRule (final RawExampleList growPos,
            final RawExampleList growNeg, final RawAttrList attrs) {
        // If anything => Positive.
        final Rule rule = new Rule(growPos.get(0).t);
        return growRule(rule, growPos, growNeg, attrs);
    }

    private static Rule growRule (final Rule rule,
            final RawExampleList growPos, final RawExampleList growNeg,
            final RawAttrList attrs) {
        Dbg.print(DBG, MODULE, "Growing rule based on: " + rule);
        // Get all possible conditions.
        final RuleCondition[][] conds =
                getAllConditions(growPos, growNeg, attrs);
        // Use bit set to record whether the condition is available or not.
        final BitSet[] availConds = getAvailConds(growPos, growNeg, conds);

        // For m-estimate in FOIL-Gain.
        final double priorProb =
                ((double) growPos.size()) / (growPos.size() + growNeg.size());
        assert priorProb != 0;

        int numOfNegCovered = getNumOfCovered(growNeg, attrs, rule);
        // Loop until rule covers no negative examples.
        while (numOfNegCovered > 0) {
            // Measure FOIL for each condition, choose the one with highest.
            final RuleCondition bestCond =
                    getBestCondition(growPos, growNeg, attrs, conds,
                            availConds, rule, priorProb);

            rule.add(bestCond); // Add condition to rule.
            numOfNegCovered = getNumOfCovered(growNeg, attrs, rule);
            Dbg.print(DBG, MODULE, "Uncovered # of neg-examples is "
                    + numOfNegCovered);
        }
        Dbg.print(DBG, MODULE, "Grew a rule: " + rule.toString());
        return rule;
    }

    /**
     * Measure FOIL_Gain for each available condition, return condition with the
     * highest gain.
     * @param priorProb
     * */
    private static RuleCondition getBestCondition (final RawExampleList pos,
            final RawExampleList neg, final RawAttrList attrs,
            final RuleCondition[][] conds, final BitSet[] availConds,
            final Rule rule, final double priorProb) {
        final int p0 = getNumOfCovered(pos, attrs, rule);
        final int n0 = getNumOfCovered(neg, attrs, rule);
        final double infoBitsBefore = getInformationBits(p0, n0, priorProb);
        // The old rule should cover some instances.
        assert !Double.isInfinite(infoBitsBefore);

        double maxFoilGain = Double.NEGATIVE_INFINITY;
        int bestI = -1;
        int bestJ = -1;
        for (int i = 0; i < conds.length; i++) {
            for (int j = 0; j < conds[i].length; j++) {
                if (availConds[i].get(j)) {
                    final RuleCondition cond = conds[i][j];
                    final Rule newRule = new Rule(rule); // A clone of rule.s
                    newRule.add(cond);
                    final int p1 = getNumOfCovered(pos, attrs, newRule);
                    final int n1 = getNumOfCovered(neg, attrs, newRule);
                    final double infoBitsAfter =
                            getInformationBits(p1, n1, priorProb);
                    // The order here likes inverse from book, because here
                    // is using -log2(p/(p+n)) not positive one.
                    final double foilGain =
                            p1 * (infoBitsBefore - infoBitsAfter);

                    if (Double.compare(maxFoilGain, foilGain) < 0) {
                        maxFoilGain = foilGain;
                        bestI = i;
                        bestJ = j;
                    }
                } // if (availConds[i].get(j)) {
            } // for (int j = 0; j < conds[i].length; j++) {
        } // for (int i = 0; i < conds.length; i++) {
        assert bestI != -1;

        // Disable conditions related to the chosen condition (No need to check
        // them later).
        if (attrs.xList.get(bestI).isContinuous) {
            // a<=v1, a>=v1, a<=v2, a>=v2,...a<=vi, a>=vi,... a<=vn, a>=vn.
            // If selected condition is a<=vi, so all conditions after a>=vi
            // (excluded) should be disabled; if selected condition is a>=vi, so
            // all conditions before a<=vi (excluded) should be disabled;
            availConds[bestI].clear(bestJ);
            if (conds[bestI][bestJ].opt == RuleCondition.OPT_LE) {
                if (bestJ + 2 < conds[bestI].length) {
                    availConds[bestI].clear(bestJ + 2, conds[bestI].length);
                }
            } else { // == RuleCondition.OPT_GE
                if (bestJ - 2 >= 0) {
                    availConds[bestI].clear(0, bestJ - 2 + 1);
                }
            }
        } else { // Discrete attribute.
            // Disable all conditions of this attribute.
            availConds[bestI].clear();
        }

        Dbg.print(DBG, MODULE,
                "Best condition: " + conds[bestI][bestJ].toString()
                        + ", max foil gain is: " + maxFoilGain);
        return conds[bestI][bestJ];
    }

    private static int getNumOfCovered (final RawExampleList exs,
            final RawAttrList attrs, final Rule rule) {
        int count = 0;
        for (RawExample e : exs) {
            if (rule.rulePredict(e.xList, attrs) != null) {
                count++;
            }
        }
        return count;
    }

    private static int getNumOfCovered (final RawExampleList exs,
            final RawAttrList attrs, final RuleList ruleList) {
        int count = 0;
        for (RawExample e : exs) {
            if (!ruleList.predict(e.xList).equals(ruleList.defaultPrediction)) {
                count++;
            }
        }
        return count;
    }

    private static final double LOG_2 = Math.log(2);
    // To make log calculation faster.
    private static final HashMap<Double, Double> LOG_CACHE =
            new HashMap<Double, Double>();
    private static final int M = 1; // m-estimate.

    /**
     * Get the minimum number of bits needed to encode the classification of an
     * arbitrary positive binding.
     * -log2(p/(p+n))
     * return Double.POSITIVE_INFINITE if p is 0.
     * @param priorProb
     * */
    private static double getInformationBits (final int p, final int n,
            final double priorProb) {
        // m-estimate.
        final double x = (p + (M * priorProb)) / (p + n + M);

        Double logX = LOG_CACHE.get(x);
        if (logX == null) {
            logX = Math.log(x);
            LOG_CACHE.put(new Double(x), logX);
        }

        return -logX / LOG_2;
    }
}
