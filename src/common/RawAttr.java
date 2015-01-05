package common;

import java.util.ArrayList;

/**
 * FileName: RawAttr.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Mar 12, 2014 3:29:43 PM
 */
public class RawAttr {
    public final String name;
    public boolean isContinuous;
    public final ArrayList<String> valueList;

    public RawAttr(final String nameIn) {
        this.name = nameIn;
        this.isContinuous = false;
        this.valueList = new ArrayList<String>();
    }

    public RawAttr(final String nameIn, final boolean isContinuous) {
        this.name = nameIn;
        this.isContinuous = isContinuous;
        this.valueList = new ArrayList<String>();
    }

    public RawAttr(final RawAttr attr) {
        this.name = attr.name;
        this.isContinuous = attr.isContinuous;
        this.valueList = new ArrayList<String>();
        this.valueList.addAll(attr.valueList);
    }
    
    @Override
    public String toString () {
        final StringBuffer sb = new StringBuffer();
        sb.append(name + " ");
        sb.append(isContinuous ? "continuous" : valueList.toString());
        return sb.toString();
    }
}
