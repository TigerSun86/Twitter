package common;

import java.util.ArrayList;

/**
 * FileName: RawExample.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Mar 13, 2014 12:55:25 AM
 */
public class RawExample {
    public ArrayList<String> xList = new ArrayList<String>();
    public String t = null;

    @Override
    public boolean equals(Object o){
        if (!(o instanceof RawExample)){
            return false;
        }
        final RawExample e = (RawExample)o;
        if (this.xList.size() != e.xList.size()) {
            return false;
        }
        for (int i = 0; i < xList.size(); i ++){
            if (!this.xList.get(i).equals(e.xList.get(i))){
                return false;
            }
        }
        if (this.t == null && e.t != null){
            return false;
        }
        if (!this.t.equals(e.t)){
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        final int p = 101;
        int hash = 5;
        for (String s: xList){
            hash = p * hash + (s != null ? s.hashCode() : 0);
        }
        hash = p * hash + (t != null ? t.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString () {
        final StringBuffer sb = new StringBuffer();
        //sb.append("X: ");
        for (String x : xList) {
            sb.append(x);
            sb.append(" ");
        }
        //sb.append("T: ");
        sb.append(t);
        return sb.toString();
    }

}
