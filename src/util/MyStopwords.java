package util;

import java.util.HashSet;

/**
 * FileName: MyStopwords.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jun 22, 2015 5:38:26 PM
 */
public class MyStopwords {
    private static final HashSet<String> WORDS = new HashSet<String>();
    static {
        WORDS.add("amn");
        WORDS.add("isn");
        WORDS.add("aren");
        WORDS.add("don");
        WORDS.add("doesn");
        WORDS.add("wasn");
        WORDS.add("weren");
        WORDS.add("didn");
        WORDS.add("haven");
        WORDS.add("hasn");
        WORDS.add("won");
        WORDS.add("wouldn");
        WORDS.add("couldn");
        WORDS.add("shan");
        WORDS.add("shouldn");
        WORDS.add("mustn");
    }

    public static boolean isStopword (String str) {
        return WORDS.contains(str.toLowerCase());
    }
}
