package features;

import java.util.Arrays;

import uk.ac.wlv.sentistrength.SentiStrength;

/**
 * FileName: MySentiStrength.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 19, 2015 5:03:11 PM
 */
public class MySentiStrength {
    private static final String ssthInitialisation[] = { "sentidata",
            "C:/WorkSpace/Twitter/data/SentStrength_Data_Sept2011/" };
    private final SentiStrength sentiStrength;

    public MySentiStrength() {
        sentiStrength = new SentiStrength();
        sentiStrength.initialise(ssthInitialisation); // Initialise
    }

    /**
     * @return SentiStrength score,
     *         int[0] is positive sentiment score range [1,5],
     *         int[1] is negative sentiment score range [1,5],
     *         Unlike original SentiStrength score, the negative is range [1,5]
     *         instead of [-1,-5].
     */
    public int[] score (String str) {
        String[] result = sentiStrength.computeSentimentScores(str).split(" ");
        int[] score = new int[2];
        score[0] = Integer.parseInt(result[0]);
        // Make negative emotion score range [1,5].
        score[1] = -Integer.parseInt(result[1]);
        return score;
    }

    static String[] test = { "I hate dog",
        "I like dog",
        "I hate dog",
        "I like dog",
        "I`m currently watching this on tv for like the tenth time..so beautifully twisted!#AmericanMary ",
        "GIP want to expand London City Airport and Gatwick to add value and sell. It's really very simple. They don't care about anything else.",
        "Here we learn how #SahajaYoga works & are guided through a short #meditation.https://www.youtube.com/watch?v=qW3XIIkGIr4 бн#Yoga #eclipse2015 #SolarEclipse" };

    public static void main (String[] args) {
        MySentiStrength senti = new MySentiStrength();
        for (String str : test) {
            System.out.println(Arrays.toString(senti.score(str)));
        }
        System.exit(0);
        SentiStrength sentiStrength = new SentiStrength();
        // Create an array of command line parameters to send (not text or file
        // to process)
        String ssthInitialisation[] =
                {
                        "sentidata",
                        "C:/WorkSpace/Twitter/data/SentStrength_Data_Sept2011/",
                        "explain" };
        sentiStrength.initialise(ssthInitialisation); // Initialise
        // can now calculate sentiment scores quickly without having to
        // initialise again
        System.out.println(sentiStrength
                .computeSentimentScores("I hate frogs."));
    }
}
