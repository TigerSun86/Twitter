package common;

import util.Dbg;

/**
 * FileName: Evaluator.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Mar 17, 2014 9:09:56 PM
 */
public class Evaluator {
    public static final String MODULE = "EVA";
    public static boolean DBG = false;

    public static double evaluate (final Hypothesis h,
            final RawExampleList dataSet) {
        int count = 0;
        // final RawExampleList wrongExs = new RawExampleList();
        for (int i = 0; i < dataSet.size(); i++) {
            final RawExample ex = dataSet.get(i);
            final String predict = h.predict(ex.xList);
            final String target = ex.t;
            if (target.equals(predict)) {
                count++;
                // System.out.println("true "+target);
            } else {
                // System.out.println("false "+target);
                // wrongExs.add(ex);
            }
            Dbg.print(
                    DBG,
                    MODULE,
                    "Ex." + (i + 1) + ", predict: " + predict + ", target: "
                            + target + ", result: "
                            + Boolean.toString(target.equals(predict)));
        }
        // System.out.println(wrongExs);
        return (((double) count) / dataSet.size());
    }

    public static class FMeasureResult {
        public final double accuracy;
        public final double precision;
        public final double recall;
        public final double falsePositive;
        public final double fmeasure;
        public final int numActPos;
        public final int numPrePos;

        public FMeasureResult(double accuracy, double precision, double recall,
                double falsePositive, double fmeasure, int numActPos,
                int numPrePos) {
            super();
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.falsePositive = falsePositive;
            this.fmeasure = fmeasure;
            this.numActPos = numActPos;
            this.numPrePos = numPrePos;
        }
    }

    /** Only work for 2 classification problem */
    public static FMeasureResult evaluateFMeasure (final Hypothesis h,
            final RawExampleList dataSet, final String posClass) {
        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;
        for (int i = 0; i < dataSet.size(); i++) {
            final RawExample ex = dataSet.get(i);
            final boolean a = posClass.equals(ex.t);
            final boolean p = posClass.equals(h.predict(ex.xList));
            if (a && p) {
                tp++;
            } else if (a && !p) {
                fn++;
            } else if (!a && p) {
                fp++;
            } else {// !a && !p
                tn++;
            }
        }

        final double accuracy = ((double) tp + tn) / (tp + tn + fp + fn);
        final double precision;
        final double recall;

        if (tp == 0) {
            precision = 0;
            recall = 0;
        } else {
            precision = ((double) tp) / (tp + fp);
            recall = ((double) tp) / (tp + fn);
        }
        final double falsePositive;
        if (fp == 0) {
            falsePositive = 0;
        } else {
            falsePositive = ((double) fp) / (fp + tn);
        }
        final double fmeasure;
        if (precision == 0 || recall == 0) {
            fmeasure = 0;
        } else {
            fmeasure = (2 * precision * recall) / (precision + recall);
        }
        final int numActPos = tp + fn;
        final int numPrePos = tp + fp;
        return new FMeasureResult(accuracy, precision, recall, falsePositive,
                fmeasure, numActPos, numPrePos);
    }
}
