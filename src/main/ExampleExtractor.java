package main;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import test.DataCollector;
import test.UserData;
import twitter4j.Status;
import util.MyMath;
import util.OReadWriter;
import util.OutputRedirection;

import common.RawExample;
import common.RawExampleList;

/**
 * FileName: ExampleExtractor.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Oct 30, 2014 5:04:46 PM
 */
public class ExampleExtractor {
    public static final String Y = "Y";
    public static final String N = "N";

    private static Date TEST_START_DATE = null;
    private static Date TEST_END_DATE = null;
    static {
        try {
            TEST_START_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Thu Nov 27 22:22:22 EDT 2014");
            TEST_END_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Wed Dec 10 11:14:47 EST 2014");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private final UserData authorData;
    // For global test, will be initialized at first time usage.
    private List<Status> authorTestTweets = null;

    public ExampleExtractor(UserData authorData) {
        this.authorData = authorData;
    }

    public static class ExsOfGlobalTest {
        public final RawExampleList train;
        public final RawExampleList testM1;
        public final RawExampleList testM2;
        public final String followerAndExsInfo;

        public ExsOfGlobalTest(RawExampleList train, RawExampleList testM1,
                RawExampleList testM2, String followerAndExsInfo) {
            this.train = train;
            this.testM1 = testM1;
            this.testM2 = testM2;
            this.followerAndExsInfo = followerAndExsInfo;
        }
    }

    public ExsOfGlobalTest getExsOfGlobalTest (long folId) {
        final UserData user = OReadWriter.getUserDate(folId);
        if (user == null) {
            // System.out.println("Cannot find user id " + folId);
            return null;
        }

        final PosAndNeg pan = getExamples(user, authorData);
        if (pan != null && pan.pos.size() > 100) {
            Collections.sort(pan.pos, TWEET_SORTER);
            Collections.sort(pan.neg, TWEET_SORTER);

            final List<List<Status>> poss = splitByDate(pan.pos);
            final List<List<Status>> negs = splitByDate(pan.neg);
            final RawExampleList train =
                    getFeatures(poss.get(0), negs.get(0), user);
            final RawExampleList testM1 =
                    getFeatures(poss.get(1), negs.get(1), user);

            if (authorTestTweets == null) { // Get when first time need it.
                authorTestTweets = getAuthorTestTweets(authorData.tweets);
            }

            final RawExampleList testM2 =
                    getAuthorExs(poss.get(1), authorTestTweets, user);

            final String followerAndExsInfo =
                    String.format("%s %d %d %d %d %d %d %d %d %d",
                            user.userProfile.getScreenName(),
                            user.userProfile.getId(), poss.get(0), negs.get(0),
                            poss.get(1), negs.get(1), 0, 0);

            return new ExsOfGlobalTest(train, testM1, testM2,
                    followerAndExsInfo);
        } else {
            return null;
        }
    }

    private static class TweetSorter implements Comparator<Status> {
        @Override
        public int compare (Status o1, Status o2) {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        }
    }

    private static final TweetSorter TWEET_SORTER = new TweetSorter();

    private static void howManyPosLess (UserData author) {
        if (author == null) {
            System.out.println("No such author");
            return;
        }
        System.out.println("****************");
        System.out.println("AuthorName AuthorId FolName FolId PosSize NegSize "
                + "PruneTrainAcc PruneTestAcc FeatureTime TrainTime");
        // System.out.println("Author is " + author.userProfile.getScreenName()
        // + " id:" + author.userProfile.getId());
        assert author.followersIds != null;

        int userCount = 0;
        int poscount = 0;
        int pos2count = 0;
        double rate = 0;
        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            if (i % (fols.length / 10) == 0) {
                // System.out.println(i + "/" + fols.length);
            }
            final Long folId = fols[i];
            final UserData user = OReadWriter.getUserDate(folId);
            if (user == null) {
                // System.out.println("Cannot find user id " + folId);
                continue;
            }
            final PosAndNeg pan = getExamples(user, author);
            if (pan != null && pan.pos.size() > 100) {
                System.out.println("User:" + user.userProfile.getScreenName()
                        + " id:" + folId + " pos:" + pan.pos.size() + " pos2:"
                        + pan.pos2.size() + " neg:" + pan.neg.size());
                // testOne(pan.pos, pan.neg, author, user);
                // testOne2(pan.pos, pan.neg, author, user, userCount,
                // authorTestTweets);
                userCount++;
                poscount += pan.pos.size();
                pos2count += pan.pos2.size();
                rate += ((double) pan.pos2.size()) / pan.pos.size();
            }
        } // for (Long folId : author.followersIds) {
        System.out.printf("Pos %d, Pos2 %d, PRate %.4f, indiRate %.4f %n",
                poscount, pos2count, ((double) pos2count) / poscount, rate
                        / userCount);
    }

    private static List<List<Status>> splitByDate (List<Status> exs) {
        final List<Status> train = new ArrayList<Status>();
        final List<Status> test = new ArrayList<Status>();
        for (Status t : exs) {
            if (t.getCreatedAt().before(TEST_START_DATE)) {
                train.add(t);
            } else if (t.getCreatedAt().before(TEST_END_DATE)) {
                test.add(t);
            } // else discard.
        }
        final List<List<Status>> ret = new ArrayList<List<Status>>();
        ret.add(train);
        ret.add(test);
        return ret;
    }

    private static List<Status> getAuthorTestTweets (List<Status> atweets) {
        final List<Status> ret = new ArrayList<Status>();
        for (Status t : atweets) {
            if (!t.isRetweet() && t.getCreatedAt().after(TEST_START_DATE)
                    && t.getCreatedAt().before(TEST_END_DATE)) {
                ret.add(t); // Is original tweet and within the test range.
            }
        }
        // Make the order from oldest to latest.
        Collections.sort(ret, TWEET_SORTER);
        return ret;
    }

    private static RawExampleList getAuthorExs (List<Status> pos,
            List<Status> authorTest, UserData user) {
        List<Status> pos2 = new ArrayList<Status>();
        List<Status> neg2 = new ArrayList<Status>();
        for (Status t : authorTest) {
            boolean isPos = false;
            for (Status pt : pos) {
                if (t.getId() == pt.getId()) {
                    // t is a positive example.
                    isPos = true;
                    break;
                }
            }
            if (isPos) {
                pos2.add(t);
            } else {
                neg2.add(t);
            }
        }
        return getFeatures(pos2, neg2, user);
    }

    private static void onePairExample () {
        final long aId = 2551981338L;
        final long fId = 407374096L;

        final UserData author = OReadWriter.getUserDate(aId);
        if (author == null) {
            // System.out.println("Cannot find user id " + folId);
            return;
        }

        final UserData user = OReadWriter.getUserDate(fId);
        if (user == null) {
            // System.out.println("Cannot find user id " + folId);
            return;
        }
        final PosAndNeg pan = getExamples(user, author);

        System.out.println("User:" + user.userProfile.getScreenName() + " id:"
                + user.userProfile.getId() + " pos:" + pan.pos.size() + " neg:"
                + pan.neg.size());
        // Make the order from oldest to latest.
        Collections.sort(pan.pos, TWEET_SORTER);
        Collections.sort(pan.neg, TWEET_SORTER);
        final RawExampleList exs = getFeatures(pan.pos, pan.neg, user);
        System.out.println(exs);
    }

    private static RawExampleList getFeatures (List<Status> pos,
            List<Status> neg, UserData ud) {
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
            final RawExample e = processOneTweet(t, ud, isPos);
            // System.out.println(e.toString() +" "+ t.getCreatedAt());
            exs.add(e);
        }
        return exs;
    }

    private static RawExample processOneTweet (Status t, UserData ud,
            boolean isPos) {
        final ArrayList<String> fs = FeatureExtractor.getFeatures(t, ud);
        final RawExample e = new RawExample();
        e.xList = fs;
        if (isPos) {
            e.t = Y;
        } else {
            e.t = N;
        }
        return e;
    }

    private static class PosAndNeg {
        public final List<Status> pos;
        public List<Status> pos2 = null;
        public final List<Status> neg;

        public PosAndNeg(List<Status> pos, List<Status> neg) {
            this.pos = pos;
            this.neg = neg;
        }

        public PosAndNeg(List<Status> pos, List<Status> pos2, List<Status> neg) {
            this.pos = pos;
            this.pos2 = pos2;
            this.neg = neg;
        }
    }

    private static PosAndNeg getExamples (final UserData user, UserData author) {
        // Get positive examples.
        final List<Status> pos =
                getPosExample(user.tweets, author.userProfile.getId());

        if (pos.isEmpty()) {
            return null; // No positive example.
        }
        // final List<Status> pos2 =
        // getPosExample2(user.tweets, author.userProfile.getId());
        // Get negative examples.
        final List<Status> neg = getNegExample(author.tweets, user.tweets, pos);

        return new PosAndNeg(pos, neg);
    }

    private static List<Status> getPosExample (ArrayList<Status> tweets,
            long authorId) {
        // Get positive examples.
        final List<Status> pos = new ArrayList<Status>();
        for (Status t : tweets) {
            if (t.isRetweet()) {
                Status t2 = t;
                while (t2.isRetweet()) { // find the original tweet of t.
                    t2 = t2.getRetweetedStatus();
                }
                // if (t2.getUser().getId() == authorId) {
                pos.add(t2);
                // }

            }
        } // for (Status t : user.tweets) {
        return pos;
    }

    private static final int POS_DAY = 1;

    private static List<Status> getPosExample2 (ArrayList<Status> tweets,
            long authorId) {
        // Get positive examples.
        final List<Status> pos = new ArrayList<Status>();
        for (Status t : tweets) {
            if (t.isRetweet()) {
                Status ot = t.getRetweetedStatus();
                assert !ot.isRetweet(); // The original tweet of t.
                final Date lastRetweetTime =
                        MyMath.getNewTime(ot.getCreatedAt(), POS_DAY,
                                Calendar.DAY_OF_YEAR);
                if (t.getCreatedAt().before(lastRetweetTime)) {
                    pos.add(ot);
                }
            }
        } // for (Status t : user.tweets) {
        return pos;
    }

    private static List<Status> getNegExample (ArrayList<Status> atweets,
            ArrayList<Status> ftweets, List<Status> pos) {
        // Get negative examples.
        // Negative example is the tweet t from author wasn't retweeted by
        // follower, and in the time interval [1 hour before t, 3 hour after t]
        // the follower has some activity.
        final List<Status> neg = new ArrayList<Status>();
        int i = 0;
        int j = 0;
        while (i < atweets.size() && j < ftweets.size()) {
            final Status tA = atweets.get(i);
            if (pos.contains(tA.getId()) || tA.isRetweet()) {
                i++; // It's a positive example or it's not a original tweet.
                continue;
            }

            final Date al =
                    MyMath.getNewTime(tA.getCreatedAt(), -1, Calendar.HOUR);
            final Date ah =
                    MyMath.getNewTime(tA.getCreatedAt(), 3, Calendar.HOUR);
            Date f = ftweets.get(j).getCreatedAt();
            while (f.after(ah) && j < ftweets.size() - 1) {
                // The tweet of follower is too late.
                j++; // Next follower t.
                f = ftweets.get(j).getCreatedAt();
            }

            if (f.after(al) && f.before(ah)) {
                // The tweet of follower is just in the interval.
                neg.add(tA);
            }
            i++; // Next author t
        } // while (i < author.tweets.size() && j < user.tweets.size()) {
        return neg;
    }

    public static void main (String[] args) {
        final OutputRedirection or = new OutputRedirection();
        // onePairExample();
        for (long authorId : DataCollector.AUTHOR_IDS) {
            // if (authorId == 497178013L) {
            final UserData author = OReadWriter.getUserDate(authorId);
            howManyPosLess(author);
            // }
        }
        or.close();
    }
}
