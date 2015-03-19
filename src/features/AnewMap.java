package features;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import features.AnewMap.Anew;

/**
 * FileName: AnewMap.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 19, 2015 2:26:48 PM
 */
public class AnewMap extends HashMap<String, Anew> {
    private static final long serialVersionUID = 1L;
    private static final double NEUTRAL = 5.0;

    public AnewMap() {
        super();
        String fileName =
                "file://localhost/C:/WorkSpace/Twitter/data/AnewTable.txt";
        try {
            BufferedReader file = new BufferedReader(new FileReader(fileName));
            while (true) {
                String str = file.readLine();
                if (str == null) {
                    break;
                }
                String[] str2 = str.split(" ");
                if (str2.length != 4) {
                    continue;
                }
                String s = str2[0];
                double v = Double.parseDouble(str2[1]);
                double a = Double.parseDouble(str2[2]);
                double d = Double.parseDouble(str2[3]);
                Anew anew = new Anew(s, v, a, d);
                this.put(s, anew);
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return average ANEW score of given lots of words. Score will be all
     *         neutral(5) if there is no word can be found in ANEW table.
     */
    public Anew score (String str) {
        String[] words = str.split(" ");
        Anew anew = new Anew("", 0, 0, 0);
        int count = 0;
        for (String w : words) {
            String w2 = Stemmer.stem(w);
            Anew an2 = this.get(w2);
            if (an2 != null) {
                anew.valence += an2.valence;
                anew.arousal += an2.arousal;
                anew.dominance += an2.dominance;
                count++;
            }
        }
        if (count > 0) {
            anew.valence /= count;
            anew.arousal /= count;
            anew.dominance /= count;
            return anew;
        } else {
            return new Anew("", NEUTRAL, NEUTRAL, NEUTRAL);
        }
    }

    public static class Anew {
        public String word;
        public double valence;
        public double arousal;
        public double dominance;

        public Anew(String s2, double v2, double a2, double d2) {
            this.word = s2;
            this.valence = v2;
            this.arousal = a2;
            this.dominance = d2;
        }

        @Override
        public String toString () {
            return String.format("%s %.2f %.2f %.2f", word, valence, arousal,
                    dominance);
        }
    }

}
