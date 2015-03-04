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

import twitter4j.Status;
import twitter4j.User;

import common.RawExample;
import common.RawExampleList;

import datacollection.Database;
import datacollection.UserInfo;

/**
 * FileName: ExampleGetter.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Feb 5, 2015 2:20:41 PM
 */
public class ExampleGetter {
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

    public static Date TRAIN_START_DATE = null;
    public static Date TEST_START_DATE = null;
    public static Date TEST_END_DATE = null;
    static {
        try {
            TRAIN_START_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Sat Jan 31 22:34:47 EST 2015");
            TEST_START_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Sat Feb 21 22:34:47 EST 2015");
            TEST_END_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Sat Feb 28 11:14:36 EST 2015");
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

    private static final int LEAST_POS_NUM = 10;

    private final Database db;
    private final List<Status> auTweets;

    public ExampleGetter(Database db, List<Status> auTweets) {
        this.db = db;
        this.auTweets = auTweets;
    }

    public Exs getExsOfPairTest (long folId) {
        final UserInfo user = db.getUser(folId);
        if (user == null) {
            // System.out.println("Cannot find user id " + folId);
            return null;
        }
        final List<Status> folTweets = db.getTweetList(folId);
        Collections.sort(folTweets, TWEET_SORTER);

        if (folTweets.isEmpty()) {
            return null; // No tweets for this follower.
        }

        final PosAndNeg pan = db.getPosAndNeg(folId, auTweets);
        if (pan != null && pan.pos.size() > LEAST_POS_NUM) {
            // Sort them again because order might be changed by user retweets
            // new tweet first.
            Collections.sort(pan.pos, TWEET_SORTER);
            Collections.sort(pan.neg, TWEET_SORTER);
            final List<List<Status>> poss = splitByDate(pan.pos);
            final List<List<Status>> negs = splitByDate(pan.neg);
            final RawExampleList train =
                    getFeatures(poss.get(0), negs.get(0), user.userProfile,
                            folTweets);
            final RawExampleList testM1 =
                    getFeatures(poss.get(1), negs.get(1), user.userProfile,
                            folTweets);

            final String followerAndExsInfo =
                    String.format("%s %d %d %d %d %d", user.userProfile
                            .getScreenName(), user.userProfile.getId(), poss
                            .get(0).size(), negs.get(0).size(), poss.get(1)
                            .size(), negs.get(1).size());

            return new Exs(train, testM1, null, followerAndExsInfo);
        } else {
            return null;
        }
    }

    private static List<List<Status>> splitByDate (List<Status> exs) {
        final List<Status> train = new ArrayList<Status>();
        final List<Status> test = new ArrayList<Status>();
        for (Status t : exs) {
            if (t.getCreatedAt().after(TRAIN_START_DATE)) {
                if (t.getCreatedAt().before(TEST_START_DATE)) {
                    train.add(t);
                } else if (t.getCreatedAt().before(TEST_END_DATE)) {
                    test.add(t);
                } // else discard.
            }
        }
        final List<List<Status>> ret = new ArrayList<List<Status>>();
        ret.add(train);
        ret.add(test);
        return ret;
    }

    private static RawExampleList getFeatures (List<Status> pos,
            List<Status> neg, User userProfile, List<Status> userTweets) {
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

    private static RawExample processOneTweet (Status t, User userProfile,
            List<Status> userTweets, boolean isPos) {
        final ArrayList<String> fs =
                FeatureExtractor.getFeatures(t, userProfile, userTweets);
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
                            TEST_END_DATE);
            Date last = new Date(0L);
            for (Status t : auTweets) {
                System.out.printf("%s, %d, %s%n", au.userProfile.getName(),
                        t.getRetweetCount(), t.getCreatedAt().toString());
                assert last.before(t.getCreatedAt());
                last = t.getCreatedAt();
            }
        }
    }

    @SuppressWarnings("unused")
    private static void showNumOfPosAndNegs () {
        final Database db = Database.getInstance();
        for (Long keyAu : UserInfo.KEY_AUTHORS) {
            final UserInfo au = db.getUser(keyAu);
            final List<Status> auTweets =
                    db.getOriginalTweetListInTimeRange(keyAu, TRAIN_START_DATE,
                            TEST_END_DATE);
            System.out
                    .printf("Author: %s, id: %d, #followers: %d, #examples: %d, from: %s, to: %s%n",
                            au.userProfile.getScreenName(), au.userId,
                            au.followersIds.size(), auTweets.size(),
                            TRAIN_START_DATE.toString(),
                            TEST_END_DATE.toString());
            // Sort auTweets.
            Collections.sort(auTweets, TWEET_SORTER);
            for (long fId : au.followersIds) {
                final PosAndNeg pan = db.getPosAndNeg(fId, auTweets);
                System.out.printf("FollowerId: %d, #pos: %d, #neg: %d%n", fId,
                        pan.pos.size(), pan.neg.size());
                for (Status t : auTweets) {
                    final String cls = pan.pos.contains(t) ? "Y" : "N";
                    System.out.printf("Id: %d, date: %s, class: %s%n",
                            t.getId(), t.getCreatedAt().toString(), cls);
                }
            }
            System.out.println("******************************");
        }
    }

    private static void showValidFollowersInfo () {
        System.out.println("Begin at: " + new Date().toString());
        final Database db = Database.getInstance();
        for (Long keyAu : UserInfo.KEY_AUTHORS) {
            final UserInfo au = db.getUser(keyAu);
            final List<Status> auTweets =
                    db.getOriginalTweetListInTimeRange(keyAu, TRAIN_START_DATE,
                            TEST_END_DATE);
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
                    TEST_START_DATE.toString(), TEST_END_DATE.toString());
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
        showValidFollowersInfo();
    }
}
