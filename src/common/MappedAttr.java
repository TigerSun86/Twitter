package common;


/**
 * FileName: MappedAttr.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 22, 2014 11:11:05 AM
 */
public class MappedAttr implements Mappable {
    public static final double HIGH_VALUE = 0.9;
    public static final double MID_VALUE = 0.5;
    public static final double LOW_VALUE = 0.1;
    
    private final String name;
    private final double valueMax;
    private final double valueMin;
    private final double mappedMax = HIGH_VALUE;
    private final double mappedMin = LOW_VALUE;
    private final int fractionLen;

    public MappedAttr(String name) {
        // For discrete attribute.
        this.name = name;
        this.valueMax = HIGH_VALUE;
        this.valueMin = LOW_VALUE;
        this.fractionLen = 1;
    }

    public MappedAttr(String name, double valueMax, double valueMin,
            int fractionLen) {
        // For continuous attribute.
        this.name = name;
        this.valueMax = valueMax;
        this.valueMin = valueMin;
        this.fractionLen = fractionLen;
    }

    @Override
    public double getValueMax () {
        return valueMax;
    }

    @Override
    public double getValueMin () {
        return valueMin;
    }

    @Override
    public double getMappedMax () {
        return mappedMax;
    }

    @Override
    public double getMappedMin () {
        return mappedMin;
    }

    @Override
    public int getFractionLength () {
        return fractionLen;
    }

    @Override
    public String toString () {
        final String m =
                String.format("%%s max: %%.%df min: %%.%df", fractionLen);
        return String.format(m, name, valueMax, valueMin);
    }
}
