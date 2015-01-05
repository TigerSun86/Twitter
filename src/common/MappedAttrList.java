package common;

import java.util.ArrayList;

import util.MyMath;

/**
 * FileName: MappedAttrList.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 15, 2014 9:16:09 PM
 */
public class MappedAttrList {
    public final ArrayList<MappedAttr> xList;
    public final MappedAttr t;

    public MappedAttrList(final RawExampleList exs, final RawAttrList attrs) {
        final double[][] maxMinLen = getMaxMin(exs, attrs);
        final double[] max = maxMinLen[0];
        final double[] min = maxMinLen[1];
        final double[] len = maxMinLen[2];

        // Make max and min value a little extended, because value in test
        // set maybe different with train set.
        for (int i = 0; i < max.length; i++) {
            final double extra = (max[i] - min[i]) * 0.1;
            max[i] = max[i] + extra;
            min[i] = min[i] - extra;
        }

        xList = new ArrayList<MappedAttr>();
        for (int i = 0; i < attrs.xList.size(); i++) {
            xList.add(new MappedAttr(attrs.xList.get(i).name, max[i], min[i],
                    (int) Math.round(len[i])));
        }
        
        t =
                new MappedAttr(attrs.t.name, max[max.length - 1],
                        min[min.length - 1],
                        (int) Math.round(len[len.length - 1]));
    }

    private static double[][] getMaxMin (final RawExampleList exs,
            final RawAttrList attrs) {
        // Initialize default maxValue with negative infinite.
        // length = attributes+target.
        final double[] maxValue = new double[attrs.xList.size() + 1];
        for (int i = 0; i < maxValue.length; i++) {
            maxValue[i] = Double.NEGATIVE_INFINITY;
        }
        // Initialize default minValue with positive infinite.
        final double[] minValue = new double[attrs.xList.size() + 1];
        for (int i = 0; i < minValue.length; i++) {
            minValue[i] = Double.POSITIVE_INFINITY;
        }
        // Initialize default minValue with 0.
        final double[] flen = new double[attrs.xList.size() + 1];

        for (RawExample ex : exs) {
            // Check each attribute x in example.
            for (int i = 0; i < attrs.xList.size(); i++) {
                if (!attrs.xList.get(i).isContinuous) {
                    continue; // Skip the discrete attribute.
                }
                final String x = ex.xList.get(i);
                final double value = Double.valueOf(x);
                // Set max value of this attribute.
                if (Double.compare(maxValue[i], value) < 0) {
                    maxValue[i] = value;
                }
                // Set min value of this attribute.
                if (Double.compare(minValue[i], value) > 0) {
                    minValue[i] = value;
                }
                final int len = MyMath.getFractionLength(x);
                // Set fraction length of this attribute.
                if (Double.compare(flen[i], (double) len) < 0) {
                    flen[i] = (double) len;
                }
            }
            // Check target.
            if (attrs.t.isContinuous) {
                final String x = ex.t;
                final double value = Double.valueOf(x);
                final int tarIndex = maxValue.length - 1;
                // Set max value of this attribute.
                if (Double.compare(maxValue[tarIndex], value) < 0) {
                    maxValue[tarIndex] = value;
                }
                // Set min value of this attribute.
                if (Double.compare(minValue[tarIndex], value) > 0) {
                    minValue[tarIndex] = value;
                }
                final int len = MyMath.getFractionLength(x);
                // Set fraction length of this attribute.
                if (Double.compare(flen[tarIndex], (double) len) < 0) {
                    flen[tarIndex] = (double) len;
                }
            }
        }
        return new double[][] { maxValue, minValue, flen };
    }

    private ArrayList<String> mapValues (final ArrayList<String> values,
            final ArrayList<RawAttr> rAttrs) {
        assert values.size() == rAttrs.size();
        final ArrayList<String> newV = new ArrayList<String>();
        for (int index = 0; index < values.size(); index++) {
            final String value = values.get(index); // Value in raw example.
            // Raw Attribute of the value.
            final RawAttr rAttr = rAttrs.get(index);
            final MappedAttr m = xList.get(index);
            newV.add(mapOneValue(value, rAttr, m));
        }
        return newV;
    }

    private static String mapOneValue (final String value, final RawAttr rAttr,
            final MappedAttr m) {
        if (rAttr.isContinuous) {
            final double x = Double.parseDouble(value);
            final double y = Mapper.valueToMapped(x, m);
            return Double.toString(y);
        } else {
            return value;
        }
    }

    public RawExampleList mapExs (final RawExampleList exs,
            final RawAttrList attrs) {
        final RawExampleList nExs = new RawExampleList();
        for (RawExample ex : exs) {
            final ArrayList<String> nVs = mapValues(ex.xList, attrs.xList);
            final String nT =
                    mapOneValue(ex.t, attrs.t, t);
            final RawExample nEx = new RawExample();
            nEx.xList = nVs;
            nEx.t = nT;
            nExs.add(nEx);
        }
        return nExs;
    }

    private ArrayList<String> backValues (final ArrayList<String> values,
            final ArrayList<RawAttr> rAttrs) {
        assert values.size() == rAttrs.size();
        final ArrayList<String> newV = new ArrayList<String>();
        for (int index = 0; index < values.size(); index++) {
            final String value = values.get(index); // Value in raw example.
            // Raw Attribute of the value.
            final RawAttr rAttr = rAttrs.get(index);
            final MappedAttr m = xList.get(index);
            newV.add(backOneValue(value, rAttr, m));
        }
        return newV;
    }

    private static String backOneValue (final String value,
            final RawAttr rAttr, final MappedAttr m) {
        if (rAttr.isContinuous) {
            final double y = Double.parseDouble(value);
            final double x = Mapper.mappedToValue(y, m);
            return Double.toString(x);
        } else {
            return value;
        }
    }

    public RawExampleList backExs (final RawExampleList exs,
            final RawAttrList attrs) {
        final RawExampleList nExs = new RawExampleList();
        for (RawExample ex : exs) {
            final ArrayList<String> nVs = backValues(ex.xList, attrs.xList);
            final String nT =
                    backOneValue(ex.t, attrs.t, t);
            final RawExample nEx = new RawExample();
            nEx.xList = nVs;
            nEx.t = nT;
            nExs.add(nEx);
        }
        return nExs;
    }
}
