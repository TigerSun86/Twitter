package common;

import util.MyMath;

/**
 * FileName: Mapper.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 21, 2014 2:22:58 PM
 */
public class Mapper {

    /**
     * public static double valueToMapped (final double x, final Mappable m) {
     * 
     * Map actual value which is range from lowest to highest of attribute, to
     * the range from mappedMin to mappedMax.
     * */
    public static double valueToMapped (final double x, final Mappable m) {
        assert !Double.isNaN(x);
        final double valueMin = m.getValueMin();
        final double valueMax = m.getValueMax();
        final double mappedMin = m.getMappedMin();
        final double mappedMax = m.getMappedMax();
        assert !Double.isNaN(valueMin) && !Double.isNaN(valueMax)
                && !Double.isNaN(mappedMin) && !Double.isNaN(mappedMax);
        // Mapped min, max are given by programmer, should not be the same.
        assert Double.compare(mappedMin, mappedMax) != 0;

        if (Double.compare(valueMin, valueMax) == 0) {
            // If min and max are the same.
            // Value min, max are given by data, could be the same.
            return (mappedMax + mappedMin) / 2;
        }
        final double y =
                ((mappedMax - mappedMin) / (valueMax - valueMin))
                        * (x - valueMin) + mappedMin;
        assert !Double.isNaN(y);
        return y;
    }

    /**
     * public static double mappedToValue (final double y, final Mappable m) {
     * 
     * Map back mapped value which is range from mappedMin to mappedMax, to the
     * range from lowest to highest of attribute.
     * */
    public static double mappedToValue (final double y, final Mappable m) {
        assert !Double.isNaN(y);
        final double valueMin = m.getValueMin();
        final double valueMax = m.getValueMax();
        final double mappedMin = m.getMappedMin();
        final double mappedMax = m.getMappedMax();
        assert !Double.isNaN(valueMin) && !Double.isNaN(valueMax)
                && !Double.isNaN(mappedMin) && !Double.isNaN(mappedMax);
        // Mapped min, max are given by programmer, should not be the same.
        assert Double.compare(mappedMin, mappedMax) != 0;

        if (Double.compare(valueMin, valueMax) == 0) {
            // If min and max are the same.
            // Value min, max are given by data, could be the same.
            return valueMin;
        }
        final double x =
                ((valueMax - valueMin) / (mappedMax - mappedMin))
                        * (y - mappedMin) + valueMin;
        assert !Double.isNaN(x);
        final int flen = m.getFractionLength();
        // Keep 1 more bit fraction for potential value.
        final double ret = MyMath.doubleRound(x, flen + 1);
        return ret;
    }
}
