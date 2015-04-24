package features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import main.ExampleGetter;
import twitter4j.Status;
import weka.core.Stopwords;
import common.RawAttr;
import datacollection.Database;
import datacollection.UserInfo;
import features.FeatureExtractor.FeatureGetter;
import features.FeatureExtractor.Fword;

/**
 * FileName: WordFeature.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 23, 2015 5:20:16 PM
 */
public class WordFeature {
    private static final int TOP_WORDS = 10;
    private static final boolean DISCARD_STOP_WORD = true;

    public enum Mode {
        NO, SUM, AVG, IDF, ENTROPY, DF
    }

    public static final String FEATURE_PRIFIX = "WORD_";

    public void setWordFeature (FeatureExtractor featureGetters,
            List<Status> tweets, Mode mode) {
        // First remove all word features.
        Iterator<FeatureGetter> iter =
                featureGetters.getterListOfPreNum.iterator();
        while (iter.hasNext()) {
            FeatureGetter f = iter.next();
            RawAttr attr = f.getAttr();
            if (attr.name.startsWith(FEATURE_PRIFIX)) {
                iter.remove();
            }
        }
        if (mode == Mode.NO) {
            return;
        }
        List<String> topWords = getTopWords(tweets, mode);
        for (String word : topWords) {
            featureGetters.getterListOfPreNum.add(new Fword(word));
        }
    }

    public List<String> getTopWords (List<Status> tweets, Mode mode) {
        if (mode == Mode.NO) {
            return new ArrayList<String>();
        }
        // Convert tweets to words.
        List<List<String>> wordsInTweets = new ArrayList<List<String>>();
        for (Status t : tweets) {
            List<String> words = splitIntoWords(t.getText());
            wordsInTweets.add(words);
        }
        // Count document frequence.
        final HashMap<String, Integer> wordToIdx =
                new HashMap<String, Integer>();
        final ArrayList<String> idxToWord = new ArrayList<String>();
        final ArrayList<Integer> idxToDf = new ArrayList<Integer>();
        for (List<String> words : wordsInTweets) {
            final HashSet<String> wordsInThisArt = new HashSet<String>();
            for (String word : words) {
                Integer idx = wordToIdx.get(word);
                if (idx == null) { // A new word hasn't occurred in anywhere.
                    wordToIdx.put(word, wordToIdx.size());
                    idxToWord.add(word);
                    idxToDf.add(1);
                    wordsInThisArt.add(word);
                } else {
                    if (!wordsInThisArt.contains(word)) {
                        // A word has occurred in other article before, but
                        // hasn't occurred in this one.
                        idxToDf.set(idx, idxToDf.get(idx) + 1);
                        wordsInThisArt.add(word);
                    }
                }
            }
        }
        // Get score for each word.
        double[] idxToScore = new double[wordToIdx.size()];
        if (mode == Mode.DF) {
            for (int idx = 0; idx < idxToWord.size(); idx++) {
                double df = idxToDf.get(idx);
                idxToScore[idx] = df;
            }
        } else {
            // MODE_SUM
            for (int i = 0; i < tweets.size(); i++) {
                double numOfRt = tweets.get(i).getRetweetCount();
                numOfRt = Math.log(numOfRt);
                // For all words in this tweet.
                for (String word : wordsInTweets.get(i)) {
                    int wordIdx = wordToIdx.get(word);
                    // Sum up numOfRt in all tweets for this word.
                    idxToScore[wordIdx] += numOfRt;

                }
            }
            if (mode != Mode.SUM) {
                double logD = Math.log(tweets.size());
                for (int idx = 0; idx < idxToWord.size(); idx++) {
                    double df = idxToDf.get(idx);
                    double idf = logD - Math.log(df);
                    if (mode == Mode.IDF) {
                        idxToScore[idx] *= idf;
                    } else { // MODE_AVG or MODE_ENTROPY.
                        idxToScore[idx] /= df;
                    }
                    if (mode == Mode.ENTROPY) {
                        double prob = df / tweets.size();
                        double entropy = getEntropy(prob);
                        idxToScore[idx] *= entropy;
                    }
                }
            }
        }

        List<WordAndScore> was = new ArrayList<WordAndScore>();
        for (int idx = 0; idx < idxToWord.size(); idx++) {
            String w = idxToWord.get(idx);
            double s = idxToScore[idx];
            was.add(new WordAndScore(w, s));
        }

        Collections.sort(was);

        List<String> topWords = new ArrayList<String>();
        for (int i = 0; i < Math.min(TOP_WORDS, was.size()); i++) {
            topWords.add(was.get(i).w);
            // System.out.println(was.get(i).toString());
        }
        return topWords;
    }

    private double getEntropy (double prob) {
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
            return w + " " + s;
        }
    }

    public static List<String> splitIntoWords (String s) {
        List<String> words = new ArrayList<String>();
        for (String word : s.split("\\p{Blank}")) {
            String filtered = filterWord(word);
            if (!filtered.isEmpty()) {
                if (DISCARD_STOP_WORD && Stopwords.isStopword(filtered)) {
                    continue;// Check stop word before and after stemmed.
                }
                // It will be converted to lower case in the stemmer.
                String stemmed = Stemmer.stem(filtered);
                if (DISCARD_STOP_WORD && Stopwords.isStopword(stemmed)) {
                    continue;// Skip this word.
                }
                words.add(stemmed);
            }
        }
        return words;
    }

    private static String filterWord (String w) {
        StringBuilder sb = new StringBuilder();
        for (char c : w.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(c);
            } else if (c == '\'') {
                // If w is "John's" just return "John".
                break;
            } else if (Character.isDigit(c) || NOT_WORD_CHARS.contains(c)) {
                // It's not a word if it's a url or hashtag, or just a number.
                // So return empty string to discard w.
                sb = new StringBuilder();
                break;
            } // else just ignore others like ,.?
        }
        return sb.toString();
    }

    private static final HashSet<Character> NOT_WORD_CHARS =
            new HashSet<Character>();
    static {
        NOT_WORD_CHARS.add('\\');
        NOT_WORD_CHARS.add('/');
        NOT_WORD_CHARS.add('@');
        NOT_WORD_CHARS.add('#');
    }

    // For test.
    final Database db = Database.getInstance();

    private List<Status> getAuthorTweets (long authorId, Date fromDate,
            Date toDate) {
        final List<Status> auTweets =
                db.getOriginalTweetListInTimeRange(authorId, fromDate, toDate);
        Iterator<Status> iter = auTweets.iterator();
        while (iter.hasNext()) {
            Status t = iter.next();
            if (t.getRetweetCount() == 0) {
                iter.remove();
            }
        }
        Collections.sort(auTweets, ExampleGetter.TWEET_SORTER);
        return auTweets;
    }

    public static void main (String[] args) {
        WordFeature a = new WordFeature();
        for (long id : UserInfo.KEY_AUTHORS) {
            final List<Status> auTweets =
                    a.getAuthorTweets(id, ExampleGetter.TRAIN_START_DATE,
                            ExampleGetter.TEST_END_DATE);
            System.out.println(id);
            for (Mode m : Mode.values()) {
                System.out.println(m);
                a.getTopWords(auTweets, m);
                System.out.println();
            }
            System.out.println("****");
        }
    }
}
