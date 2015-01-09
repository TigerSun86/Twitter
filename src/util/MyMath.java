package util;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
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
        final int[] ret;
        if (m <= n) {
            ret = new int[m];
        } else { // Special case.
            ret = new int[n];
        }

        final HashSet<Integer> selected = new HashSet<Integer>();
        final Random ran = new Random();
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
}
