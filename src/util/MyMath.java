package util;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * FileName: MyMath.java
 * @Description: Math methods.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
public class MyMath {
    public static double doubleRound (final double n, final int maxFLen) {
        final int RoundFactor = (int) Math.pow(10, maxFLen);
        return Math.round(n * RoundFactor) / (double) RoundFactor;
    }

    public static int getFractionLength (final String s) {
        int dotPos = s.indexOf('.');
        if (dotPos == -1) {
            return 0; // No fraction.
        }
        final String result = s.substring(dotPos + 1, s.length());
        assert (result.length() != 0);
        return result.length();
    }

    public static int[] mOutofN (final int m, final int n) {
        return mOutofN(m, n, new Random().nextLong());
    }

    public static int[]
            mOutofN (final int m, final int n, final long randomSeed) {
        final int[] ret;
        if (m <= n) {
            ret = new int[m];
        } else { // Special case.
            ret = new int[n];
        }

        final HashSet<Integer> selected = new HashSet<Integer>();
        final Random ran = new Random(randomSeed);
        for (int i = 0; i < ret.length; i++) {
            while (true) {
                final int r = ran.nextInt(n);
                if (!selected.contains(r)) {
                    ret[i] = r;
                    selected.add(r);
                    break;
                }
            }
        }
        return ret;
    }

    public static int selectByProb (final double[] probDistribute) {
        final double[] prob = new double[probDistribute.length + 1];
        prob[0] = 0;
        for (int i = 0; i < probDistribute.length; i++) {
            prob[i + 1] = prob[i] + probDistribute[i];
        }

        final double ran = new Random().nextDouble();

        int index = Arrays.binarySearch(prob, ran);
        if (index < 0) { // Didn't find the value equals with ran.
            // Make index back to 'insertion point'.
            index = -index;
            index -= 1;
            if (index == prob.length) { // Ran greater than all elements in prob
                index -= 1; // Back 'insertion point' to last element.
            }
            // The selected index is 'insertion point' - 1;
            index -= 1;
        } else if (index == prob.length - 1) {
            // Ran just equals with the last value.
            index -= 1;
        }
        return index;
    }

    public static double
            randomDoubleBetween (final double min, final double max) {
        return (Math.random() * (max - min)) + min;
    }

    public static double distance (Point2D.Double p1, Point2D.Double p2) {
        return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
                * (p1.y - p2.y));
    }

    public static String getExtentionOfFileName (final String fileName) {
        String extension = "";
        final int i = fileName.lastIndexOf('.');
        final int p =
                Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if (i > p) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    public static String getFileNameWithoutExt (final String fileName) {
        String name = "";
        final int i = fileName.lastIndexOf('.');
        final int p =
                Math.max(
                        0,
                        Math.max(fileName.lastIndexOf('/'),
                                fileName.lastIndexOf('\\')));
        if (i > p) {
            name = fileName.substring(p, i);
        }
        return name;
    }

    public static Date getNewTime (Date time, int dif, int field) {
        final Calendar c = Calendar.getInstance();
        c.setTime(time);
        final int h = c.get(field);
        c.set(field, h + dif);
        return c.getTime();
    }

    /** http://www.dzone.com/snippets/calculate-pearsons-correlation */
    public static double getPearsonCorrelation (double[] scores1,
            double[] scores2) {
        double result = 0;
        double sum_sq_x = 0;
        double sum_sq_y = 0;
        double sum_coproduct = 0;
        double mean_x = scores1[0];
        double mean_y = scores2[0];
        for (int i = 2; i < scores1.length + 1; i += 1) {
            double sweep = Double.valueOf(i - 1) / i;
            double delta_x = scores1[i - 1] - mean_x;
            double delta_y = scores2[i - 1] - mean_y;
            sum_sq_x += delta_x * delta_x * sweep;
            sum_sq_y += delta_y * delta_y * sweep;
            sum_coproduct += delta_x * delta_y * sweep;
            mean_x += delta_x / i;
            mean_y += delta_y / i;
        }
        double pop_sd_x = (double) Math.sqrt(sum_sq_x / scores1.length);
        double pop_sd_y = (double) Math.sqrt(sum_sq_y / scores1.length);
        double cov_x_y = sum_coproduct / scores1.length;
        result = cov_x_y / (pop_sd_x * pop_sd_y);
        return result;
    }

    public static double getRootMeanSquareError (double[] scores1,
            double[] scores2) {
        if (scores1.length == 0) {
            return 0;
        }
        double result = 0;
        for (int i = 0; i < scores1.length; i++) {
            double delta = scores1[i] - scores2[i];
            result += delta * delta;
        }
        result /= scores1.length;
        result = Math.sqrt(result);
        return result;
    }

    /**
     * Return k subsets by splitting idxesIn randomly, each set will have same
     * (as possible) amount of idxes.
     * If k <=1, it will be only 1 subset with all idxes.
     * If k > size of idxesIn, it will still return k subsets, each the first
     * sets have 1 idx and other sets have no idx.
     * */
    public static ArrayList<HashSet<Integer>> splitToKSets (
            List<Integer> idxesIn, int k, long randomSeed) {
        ArrayList<HashSet<Integer>> ret = new ArrayList<HashSet<Integer>>();
        if (k <= 1) { // Special case.
            HashSet<Integer> set = new HashSet<Integer>();
            set.addAll(idxesIn);
            ret.add(set);
            return ret;
        }

        Random ran = new Random(randomSeed);
        ArrayList<Integer> idxes = new ArrayList<Integer>();
        idxes.addAll(idxesIn);
        int endIdx = idxes.size() - 1; // 0 to endIdx are all available.
        for (int kremain = Math.max(1, k); kremain > 0; kremain--) {
            int size = getSize(endIdx + 1, kremain);
            HashSet<Integer> selected = new HashSet<Integer>();
            while (selected.size() < size) {
                int r = ran.nextInt(endIdx + 1);
                int sel = idxes.get(r);
                selected.add(sel);
                swap(idxes, r, endIdx);
                endIdx--;
            }
            ret.add(selected);
        }
        return ret;
    }

    private static int getSize (int idxRemain, int kRemain) {
        int size;
        if (idxRemain > 0) {
            size = (int) Math.round(((double) idxRemain) / kRemain);
            if (size == 0) {
                // if idx =1 but k =3 will lead to size = 0, but the last idx
                // should be used.
                size = 1;
            }
        } else { // There is no idx remain.
            size = 0;
        }
        return size;
    }

    private static void swap (ArrayList<Integer> idxes, int a, int b) {
        if (a == b) {
            return;
        }
        assert !(a < 0 || b < 0 || a >= idxes.size() || b >= idxes.size());
        int tmp = idxes.get(a);
        idxes.set(a, idxes.get(b));
        idxes.set(b, tmp);
        return;
    }
}
