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
        //final RawExampleList wrongExs = new RawExampleList();
        for (int i = 0; i < dataSet.size();i++){
            final RawExample ex = dataSet.get(i);
            final String predict = h.predict(ex.xList);
            final String target = ex.t;
            if (target.equals(predict)) {
                count++;
                //System.out.println("true "+target);
            } else {
                //System.out.println("false "+target);
                //wrongExs.add(ex);
            }
            Dbg.print(DBG, MODULE,"Ex."+ (i+1)+
                    ", predict: " + predict + ", target: " + target + ", result: "
                            + Boolean.toString(target.equals(predict)));
        }
        //System.out.println(wrongExs);
        return (((double) count) / dataSet.size());
    }
}
