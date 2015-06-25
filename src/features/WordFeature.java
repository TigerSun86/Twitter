package features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;
import util.Dbg;
import util.MyStopwords;
import weka.core.Stopwords;
import weka.core.stemmers.IteratedLovinsStemmer;
import weka.core.stemmers.Stemmer;
import weka.core.tokenizers.AlphabeticTokenizer;
import weka.core.tokenizers.Tokenizer;

/**
 * FileName: WordFeature.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 23, 2015 5:20:16 PM
 */
public class WordFeature {
    private static final Tokenizer TOKENIZER = new AlphabeticTokenizer();
    private static final Stemmer STEMMER = new IteratedLovinsStemmer();

    public enum WordSelectingMode {
        SUM, AVG, IDF, ENTROPY, DF, SUMDEV, DEV, DFDEV, DEVHIGH
    }

    public static List<String> getTopEntities (WordStatisDoc doc) {
        WordSelectingMode mode = doc.para.selectingMode;
        // Get score for each word.
        List<WordAndScore> was = new ArrayList<WordAndScore>();
        if (mode == WordSelectingMode.DEV) {
            for (String w : doc.wordList) {
                // WordStatisDoc should guarantee word appearing at least twice.
                assert doc.getDf(w) >= 2;
                double negWordDev = -doc.getLogRtDev(w); // Prefer low dev.
                was.add(new WordAndScore(w, negWordDev));
            }
        } else if (mode == WordSelectingMode.DEVHIGH) {
            for (String w : doc.wordList) {
                // WordStatisDoc should guarantee word appearing at least twice.
                assert doc.getDf(w) >= 2;
                double wordDev = doc.getLogRtDev(w);
                was.add(new WordAndScore(w, wordDev));

            }
        } else if (mode == WordSelectingMode.DF) {
            for (String w : doc.wordList) {
                was.add(new WordAndScore(w, doc.getDf(w)));
            }
        } else if (mode == WordSelectingMode.DFDEV) {
            double dev = doc.getLogRtDev();
            for (String w : doc.wordList) {
                int df = doc.getDf(w);
                // WordStatisDoc should guarantee word appearing at least twice.
                assert df >= 2;
                // The dev lower than average.
                if (doc.getLogRtDev(w) < dev) {
                    was.add(new WordAndScore(w, df));
                }
            }
        } else if (mode == WordSelectingMode.SUM) {
            for (String w : doc.wordList) {
                was.add(new WordAndScore(w, doc.getLogRtSum(w)));
            }
        } else if (mode == WordSelectingMode.SUMDEV) {
            double dev = doc.getLogRtDev();
            for (String w : doc.wordList) {
                // WordStatisDoc should guarantee word appearing at least twice.
                assert doc.getDf(w) >= 2;
                // The dev lower than average.
                if (doc.getLogRtDev(w) < dev) {
                    was.add(new WordAndScore(w, doc.getLogRtSum(w)));
                }
            }
        } else if (mode == WordSelectingMode.AVG) {
            for (String w : doc.wordList) {
                was.add(new WordAndScore(w, doc.getLogRtSum(w) / doc.getDf(w)));
            }
        } else if (mode == WordSelectingMode.IDF) {
            double logD = Math.log(doc.getDf());
            for (String w : doc.wordList) {
                double idf = logD - Math.log(doc.getDf(w));
                was.add(new WordAndScore(w, doc.getLogRtSum(w) * idf));
            }
        } else {// if (mode == WordSelectingMode.ENTROPY) {
            assert mode == WordSelectingMode.ENTROPY;
            for (String w : doc.wordList) {
                double prob = ((double) doc.getDf(w)) / doc.getDf();
                was.add(new WordAndScore(w, doc.getLogRtSum(w) * getEntropy(prob)
                        / doc.getDf(w)));
            }
        }
        // Sort from largest to smallest.
        Collections.sort(was);
        if (Dbg.dbg) {
            System.out.println("***** Top entities " + doc.para.toString());
        }
        assert doc.para.numOfWords > 0;
        List<String> topWords = new ArrayList<String>();
        for (int i = 0; i < Math.min(doc.para.numOfWords, was.size()); i++) {
            topWords.add(was.get(i).w);
            if (Dbg.dbg) {
                System.out.println(was.get(i).toString());
            }
        }
        return topWords;
    }

    private static double getEntropy (double prob) {
        if (prob == 0 || prob == 1) {
            return 0;
        }
        double result =
                -prob * Math.log(prob) - (1 - prob) * Math.log(1 - prob);
        return result;
    }

    private static class WordAndScore implements Comparable<WordAndScore> {
        String w;
        double s;

        public WordAndScore(String w, double s) {
            this.w = w;
            this.s = s;
        }

        @Override
        public int compareTo (WordAndScore o) {
            if (this.s != o.s) {
                return -Double.compare(this.s, o.s); // Larger the better.
            } else {
                return this.w.compareTo(o.w);
            }
        }

        @Override
        public String toString () {
            return String.format("%s %.2f", w, s);
        }
    }

    public static List<String> splitIntoWords (Status t,
            boolean discardStopWords, boolean needStemming) {
        List<String> words = new ArrayList<String>();

        String s = getTextOfTweet(t);
        // Get tokenizer
        TOKENIZER.tokenize(s);
        // Iterate through tokens, perform stemming, and remove stopwords
        // (if required)
        while (TOKENIZER.hasMoreElements()) {
            String word = ((String) TOKENIZER.nextElement()).intern();
            word = word.toLowerCase();
            if (discardStopWords
                    && (Stopwords.isStopword(word) || MyStopwords
                            .isStopword(word))) {
                continue;// Check stop word before stemmed.
            }
            if (needStemming) {
                word = STEMMER.stem(word);
            }
            words.add(word);
        }
        return words;
    }

    public static String getTextOfTweet (Status t) {
        String s = t.getText();
        for (URLEntity entity : t.getURLEntities()) {
            s = s.replace(entity.getText(), "");
        }
        for (HashtagEntity entity : t.getHashtagEntities()) {
            s = s.replace("#" + entity.getText(), "");
        }
        for (UserMentionEntity entity : t.getUserMentionEntities()) {
            s = s.replace("@" + entity.getText(), "");
        }
        for (MediaEntity entity : t.getMediaEntities()) {
            s = s.replace(entity.getText(), "");
        }
        return s;
    }
}
