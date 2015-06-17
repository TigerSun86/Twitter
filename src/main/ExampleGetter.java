package main;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import learners.MeToWeka;
import twitter4j.Status;
import twitter4j.User;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import common.MappedAttrList;
import common.RawAttrList;
import common.RawExample;
import common.RawExampleList;
import common.TrainTestSplitter;

import datacollection.Database;
import datacollection.UserInfo;
import features.FeatureExtractor;

/**
 * FileName: ExampleGetter.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Feb 5, 2015 2:20:41 PM
 */
public class ExampleGetter {
    private static final RawAttrList RAW_ATTR = new RawAttrList(
            ModelExecuter.ATTR);
    public static final int POS_DAY = 1;
    public static final long DAY_IN_MILLISECONDS = TimeUnit.MILLISECONDS
            .convert(POS_DAY, TimeUnit.DAYS);
    public static final String Y = "Y";
    public static final String N = "N";

    public static class PosAndNeg {
        public final List<Status> pos;
        public final List<Status> neg;

        public PosAndNeg(List<Status> pos, List<Status> neg) {
            this.pos = pos;
            this.neg = neg;
        }
    }

    public static class Exs {
        public final RawExampleList train;
        public final RawExampleList testM1;
        public final RawExampleList testM2;
        public final String followerAndExsInfo;

        public Exs(RawExampleList train, RawExampleList testM1,
                RawExampleList testM2, String followerAndExsInfo) {
            this.train = train;
            this.testM1 = testM1;
            this.testM2 = testM2;
            this.followerAndExsInfo = followerAndExsInfo;
        }
    }

    public static class ExsForWeka {
        public final Instances train;
        public final Instances test;

        public ExsForWeka(Instances train, Instances test) {
            this.train = train;
            this.test = test;
        }
    }

    public static Date TRAIN_START_DATE = null;
    public static Date TEST_START_DATE = null;
    public static Date TEST_END_DATE = null;
    static {
        try {
            TRAIN_START_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Tue Jan 27 22:34:47 EST 2015");
            TEST_START_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Fri May 15 14:44:54 EDT 2015");
            TEST_END_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Mon Jun 15 00:42:00 EDT 2015");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static class TweetSorter implements Comparator<Status> {
        @Override
        public int compare (Status o1, Status o2) {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        }
    }

    // Sort from oldest to latest.
    public static final TweetSorter TWEET_SORTER = new TweetSorter();

    public static final int LEAST_POS_NUM = 20;

    private final Database db;
    public final List<Status> auTweets;
    public final List<Status> auTweetsM2;
    private final FeatureExtractor featureGetters;

    public ExampleGetter(Database db, List<Status> auTweets,
            List<Status> auTweetsM2, FeatureExtractor featureGetters) {
        this.db = db;
        this.auTweets = auTweets;
        this.auTweetsM2 = auTweetsM2;
        this.featureGetters = featureGetters;
    }

    private static final boolean NEED_NORMALIZE = true;

    public ExsForWeka getExsInWekaForPredictNum () {
        double avgRt = 0;
        int minRt = Integer.MAX_VALUE;
        int maxRt = Integer.MIN_VALUE;

        MeToWeka w =
                new MeToWeka(this.featureGetters.getAttrListOfPredictNum());
        Instances train = new Instances("Train", w.attributes, auTweets.size());
        train.setClassIndex(train.numAttributes() - 1);
        Instances test = new Instances("Test", w.attributes, auTweetsM2.size());
        test.setClassIndex(test.numAttributes() - 1);
        for (Status t : auTweets) {
            int rtCount = t.getRetweetCount();
            ArrayList<String> features =
                    featureGetters.getFeaturesOfPredictNum(t, null, null);
            RawExample e = new RawExample();
            e.xList = features;
            e.t = Double.toString(Math.log(rtCount + 1));
            train.add(w.convertInstance(e));
            avgRt += rtCount;
            if (minRt > rtCount) {
                minRt = rtCount;
            } else if (maxRt < rtCount) {
                maxRt = rtCount;
            }
        }
        for (Status t : auTweetsM2) {
            int rtCount = t.getRetweetCount();
            ArrayList<String> features =
                    featureGetters.getFeaturesOfPredictNum(t, null, null);
            RawExample e = new RawExample();
            e.xList = features;
            e.t = Double.toString(Math.log(rtCount + 1));
            test.add(w.convertInstance(e));
            avgRt += rtCount;
            if (minRt > rtCount) {
                minRt = rtCount;
            } else if (maxRt < rtCount) {
                maxRt = rtCount;
            }
        }

        if (NEED_NORMALIZE) {
            try { // Normalize training data
                Normalize norm = new Normalize();
                norm.setInputFormat(train);
                train = Filter.useFilter(train, norm);
                test = Filter.useFilter(test, norm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ExsForWeka ret = new ExsForWeka(train, test);

        String authorName = auTweets.get(0).getUser().getScreenName();
        avgRt /= (auTweets.size() + auTweetsM2.size());
        System.out.println("AuthorName, Instance#InTrainingSet, "
                + "Instance#InTestSet, AvgRt, MinRt, MaxRt");
        System.out.printf("%s,%d,%d,%.2f,%d,%d%n", authorName, auTweets.size(),
                auTweetsM2.size(), avgRt, minRt, maxRt);
        return ret;
    }

    public Exs getExsForPredictNum () {
        RawExampleList train = new RawExampleList();
        for (Status t : auTweets) {
            ArrayList<String> features =
                    featureGetters.getFeaturesOfPredictNum(t, null, null);
            RawExample e = new RawExample();
            e.xList = features;
            e.t = Double.toString(Math.log(t.getRetweetCount()));
            train.add(e);
        }
        RawExampleList testM2 = new RawExampleList();
        for (Status t : auTweetsM2) {
            ArrayList<String> features =
                    featureGetters.getFeaturesOfPredictNum(t, null, null);
            RawExample e = new RawExample();
            e.xList = features;
            e.t = Double.toString(Math.log(t.getRetweetCount()));
            testM2.add(e);
        }
        // Map all attributes in range 0 to 1.
        RawAttrList attrs = featureGetters.getAttrListOfPredictNum();
        final MappedAttrList mAttr = new MappedAttrList(train, attrs);
        // Rescale (map) all data in range 0 to 1.
        train = mAttr.mapExs(train, attrs);
        testM2 = mAttr.mapExs(testM2, attrs);
        Exs ret = new Exs(train, null, testM2, "");
        return ret;
    }

    public Exs getExs (long folId, boolean isGlobal) {
        final UserInfo user = db.getUser(folId);
        if (user == null) {
            return null;
        }
        final List<Status> folTweets = db.getTweetList(folId);
        Collections.sort(folTweets, TWEET_SORTER);
        if (folTweets.isEmpty()) {
            return null; // No tweets for this follower.
        }

        final PosAndNeg pan = db.getPosAndNeg(folId, auTweets);
        if (pan == null || pan.pos.size() < LEAST_POS_NUM) {
            return null;
        }

        // Sort them again because order might be changed by user retweets
        // new tweet first.
        Collections.sort(pan.pos, TWEET_SORTER);
        Collections.sort(pan.neg, TWEET_SORTER);
        RawExampleList all =
                getFeatures(pan.pos, pan.neg, user.userProfile, folTweets);
        // Discard all neg examples before the first pos example.
        RawExampleList tmp = new RawExampleList();
        boolean firstPos = false;
        for (RawExample e : all) {
            if (e.t.equals(Y)) {
                firstPos = true;
                tmp.add(e);
            } else { // Neg example.
                if (firstPos) {
                    tmp.add(e);
                } // else discard negs before the first pos.
            }
        }
        all = tmp;

        // Map all attributes in range 0 to 1.
        final MappedAttrList mAttr = new MappedAttrList(all, RAW_ATTR);
        // Rescale (map) all data in range 0 to 1.
        all = mAttr.mapExs(all, RAW_ATTR);

        // Train and test will split into same pos rate.
        final RawExampleList[] exs2 =
                TrainTestSplitter.split(all, RAW_ATTR,
                        TrainTestSplitter.DEFAULT_RATIO);
        final RawExampleList train = exs2[0];
        final RawExampleList test = exs2[1];

        String trainPosAndNeg = countPosAndNeg(train);
        String testPosAndNeg = countPosAndNeg(test);
        final String followerAndExsInfo =
                String.format("%s %d %s %s", user.userProfile.getScreenName(),
                        user.userProfile.getId(), trainPosAndNeg, testPosAndNeg);

        RawExampleList testM2 = null;
        if (isGlobal) {
            assert auTweetsM2 != null;
            PosAndNeg panM2 = db.getPosAndNeg(folId, auTweetsM2);
            Collections.sort(panM2.pos, TWEET_SORTER);
            Collections.sort(panM2.neg, TWEET_SORTER);
            testM2 =
                    getFeatures(panM2.pos, panM2.neg, user.userProfile,
                            folTweets);
            // Rescale (map) all data in range 0 to 1.
            testM2 = mAttr.mapExs(testM2, RAW_ATTR);
        }

        return new Exs(train, test, testM2, followerAndExsInfo);
    }

    private static String countPosAndNeg (RawExampleList dataSet) {
        // Count np and nn.
        int np = 0;
        int nn = 0;
        for (int i = 0; i < dataSet.size(); i++) {
            if (dataSet.get(i).t.equals(ExampleGetter.Y)) {
                np++;
            } else {
                nn++;
            }
        }
        return np + " " + nn;
    }

    private RawExampleList getFeatures (List<Status> pos, List<Status> neg,
            User userProfile, List<Status> userTweets) {
        final RawExampleList exs = new RawExampleList();

        // Start from the oldest tweet to the latest tweet.
        int i = 0;
        int j = 0;
        while (i < pos.size() || j < neg.size()) {
            final Status t;
            final boolean isPos;
            if (i == pos.size()) {
                t = neg.get(j);
                isPos = false;
                j++;
            } else if (j == neg.size()) {
                t = pos.get(i);
                isPos = true;
                i++;
            } else {
                final Status pt = pos.get(i);
                final Status nt = neg.get(j);
                if (pt.getCreatedAt().before(nt.getCreatedAt())) {
                    t = pt;
                    isPos = true;
                    i++;
                } else {
                    t = nt;
                    isPos = false;
                    j++;
                }
            }
            final RawExample e =
                    processOneTweet(t, userProfile, userTweets, isPos);
            // System.out.println(e.toString() +" "+ t.getCreatedAt());
            exs.add(e);
        }
        return exs;
    }

    private RawExample processOneTweet (Status t, User userProfile,
            List<Status> userTweets, boolean isPos) {
        final ArrayList<String> fs =
                featureGetters.getFeaturesOfModel1(t, userProfile, userTweets);
        final RawExample e = new RawExample();
        e.xList = fs;
        if (isPos) {
            e.t = Y;
        } else {
            e.t = N;
        }
        return e;
    }

    @SuppressWarnings("unused")
    private static void showNumberOfRetweets () {
        final Database db = Database.getInstance();
        System.out.println("Author #ofReteet CreatedAt");
        for (Long keyAu : UserInfo.KEY_AUTHORS) {
            final UserInfo au = db.getUser(keyAu);
            final List<Status> auTweets =
                    db.getOriginalTweetListInTimeRange(keyAu, TRAIN_START_DATE,
                            TEST_START_DATE);
            Date last = new Date(0L);
            for (Status t : auTweets) {
                System.out.printf("%s, %d, %s%n", au.userProfile.getName(),
                        t.getRetweetCount(), t.getCreatedAt().toString());
                assert last.before(t.getCreatedAt());
                last = t.getCreatedAt();
            }
        }
    }

    private static void showNumOfPosAndNegs () {
        System.out.printf(
                "Begin at: %s, train start at: %s, test end at: %s.%n",
                new Date().toString(), TRAIN_START_DATE.toString(),
                TEST_START_DATE.toString());
        final Database db = Database.getInstance();
        for (Long keyAu : UserInfo.KEY_AUTHORS) {
            final UserInfo au = db.getUser(keyAu);
            final List<Status> auTweets =
                    db.getOriginalTweetListInTimeRange(keyAu, TRAIN_START_DATE,
                            TEST_START_DATE);
            // Sort auTweets.
            Collections.sort(auTweets, TWEET_SORTER);

            System.out.printf("Author: %s, id: %d, #fols: %d, #exs: %d.%n",
                    au.userProfile.getScreenName(), au.userId,
                    au.followersIds.size(), auTweets.size());
            System.out.println("******************************");

            System.out.println("AuthorId fId #pos #neg #userTweets");
            for (long fId : au.followersIds) {
                final PosAndNeg pan = db.getPosAndNeg(fId, auTweets);
                if (pan == null) {
                    continue; // Didn't find the user.
                }
                System.out.printf("%d %d %d %d %d%n", au.userId, fId,
                        pan.pos.size(), pan.neg.size(), db.getTweetsCount(fId));
            }
            System.out.println("******************************");
        }
        System.out.println("End at: " + new Date().toString());
    }

    @SuppressWarnings("unused")
    private static void showValidFollowersInfo () {
        System.out.println("Begin at: " + new Date().toString());
        final Database db = Database.getInstance();
        for (Long keyAu : UserInfo.KEY_AUTHORS) {
            final UserInfo au = db.getUser(keyAu);
            final List<Status> auTweets =
                    db.getOriginalTweetListInTimeRange(keyAu, TRAIN_START_DATE,
                            TEST_START_DATE);
            // Sort auTweets.
            Collections.sort(auTweets, TWEET_SORTER);

            int testStartIdx = 0;
            while (testStartIdx < auTweets.size()) {
                if (!auTweets.get(testStartIdx).getCreatedAt()
                        .before(TEST_START_DATE)) {
                    break;
                }
                testStartIdx++;
            }

            int nTrain = testStartIdx * 2 / 3;
            int nVal = testStartIdx - nTrain;
            int nTest = auTweets.size() - testStartIdx;
            System.out.printf("Author: %s, id: %d, #fols: %d, "
                    + "trainStart: %s, testStart: %s, testEnd: %s.%n",
                    au.userProfile.getScreenName(), au.userId,
                    au.followersIds.size(), TRAIN_START_DATE.toString(),
                    TEST_START_DATE.toString(), TEST_START_DATE.toString());
            System.out.printf(
                    "Total#exs: %d, train#: %d, val#: %d, test#: %d.%n",
                    auTweets.size(), nTrain, nVal, nTest);
            System.out.println("******************************");
            if (nTrain < LEAST_POS_NUM || nTest < LEAST_POS_NUM) {
                continue; // Skip the author for few examples.
            }
            System.out
                    .println("AuthorId fId #pos #neg train#p train#n val#p val#n test#p test#n");
            for (long fId : au.followersIds) {
                final PosAndNeg pan = db.getPosAndNeg(fId, auTweets);
                if (pan == null || pan.pos.isEmpty() || pan.neg.isEmpty()) {
                    continue;
                }
                HashSet<Status> pset = new HashSet<Status>();
                pset.addAll(pan.pos);
                String trainPan =
                        countPosAndNegInRange(auTweets, pset, 0, nTrain);
                String valPan =
                        countPosAndNegInRange(auTweets, pset, nTrain,
                                testStartIdx);
                String testPan =
                        countPosAndNegInRange(auTweets, pset, testStartIdx,
                                auTweets.size());
                if (Integer.parseInt(trainPan.split(" ")[0]) >= LEAST_POS_NUM
                        && Integer.parseInt(testPan.split(" ")[0]) >= LEAST_POS_NUM) {
                    // Only print when there are enough #pos in both train and
                    // test set.
                    System.out.printf("%d %d %d %d %s %s %s%n", au.userId, fId,
                            pan.pos.size(), pan.neg.size(), trainPan, valPan,
                            testPan);
                }

            }
            System.out.println("******************************");
        }
        System.out.println("End at: " + new Date().toString());
    }

    /**
     * @param int from: inclusive
     *        int to: exclusive
     */
    private static String countPosAndNegInRange (List<Status> ts,
            HashSet<Status> pset, int from, int to) {
        int p = 0;
        int n = 0;
        for (int i = from; i < Math.min(ts.size(), to); i++) {
            if (pset.contains(ts.get(i))) {
                p++;
            } else {
                n++;
            }
        }
        return p + " " + n;
    }

    public static void main (String[] args) {
        showNumOfPosAndNegs();
    }
}
