package ripperk;

/**
 * FileName: RuleCondition.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Sep 1, 2014 2:37:18 PM
 */
public class RuleCondition {
    public static final int OPT_EQ = 0;
    public static final int OPT_LE = 1;
    public static final int OPT_GE = 2;

    public final String name;
    public final String value;
    public final int opt;

    public RuleCondition(String name, String value, int opt) {
        this.name = name;
        this.value = value;
        this.opt = opt;
    }

    public boolean isSatisfied (final String v) {
        if (v == null) return false;
        
        boolean ret = false;
        double v1;
        double v2;
        
        switch (opt) {
            case OPT_EQ: // value == v.
                ret = value.equals(v);
                break;
            case OPT_LE: // value <= v.
                v1 = Double.parseDouble(value);
                v2 = Double.parseDouble(v);
                ret = (Double.compare(v1, v2) <= 0);
                break;
            case OPT_GE: // value >= v.
                v1 = Double.parseDouble(value);
                v2 = Double.parseDouble(v);
                ret = (Double.compare(v1, v2) >= 0);
                break;
        }

        return ret;
    }
    @Override
    public String toString(){
        final String ret;
        switch (opt) {
            case OPT_EQ: // value == v.
                ret = name +"=="+value;
                break;
            case OPT_LE: // value <= v.
                ret = name +"<="+value;
                break;
            default: // value >= v.
                ret = name +">="+value;
                break;
        }
        return ret;
    }
}
