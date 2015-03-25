package main;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
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

/**@deprecated*/
public class ExampleExtractor {
    public static final String Y = "Y";
    public static final String N = "N";

    private static Date TRAIN_START_DATE = null;
    private static Date TEST_START_DATE = null;
    private static Date TEST_END_DATE = null;
    static {
        try {
            TRAIN_START_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Thu Dec 25 15:45:28 EST 2014");
            TEST_START_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Thu Jan 01 18:33:54 EST 2015");
            TEST_END_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Thu Jan 08 18:33:54 EST 2015");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static final int POS_DAY = 1;
    private static final int LEAST_POS_NUM = 100;

    private static class TweetSorter implements Comparator<Status> {
        @Override
        public int compare (Status o1, Status o2) {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        }
    }
    // Sort from oldest to latest.
    private static final TweetSorter TWEET_SORTER = new TweetSorter();

    private final UserData authorData;

    public ExampleExtractor(UserData authorData) {
        this.authorData = authorData;
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

    public Exs getExsOfPairTest (long folId) {
        final UserData user = OReadWriter.getUserDate(folId);
        if (user == null || user.friendsIds == null) {
            // System.out.println("Cannot find user id " + folId);
            return null;
        }

        final PosAndNeg pan = getExamples(user, authorData);
        if (pan != null && pan.pos.size() > LEAST_POS_NUM) {
            final List<List<Status>> poss = splitByDate(pan.pos);
            final List<List<Status>> negs = splitByDate(pan.neg);
            final RawExampleList train =
                    getFeatures(poss.get(0), negs.get(0), user);
            final RawExampleList testM1 =
                    getFeatures(poss.get(1), negs.get(1), user);

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

    public Exs getExsOfGlobalTest (long folId) {
        final UserData user = OReadWriter.getUserDate(folId);
        if (user == null || user.friendsIds == null) {
            // System.out.println("Cannot find user id " + folId);
            return null;
        }

        final PosAndNeg pan = getExamples(user, authorData);
        if (pan != null && pan.pos.size() > LEAST_POS_NUM) {
            final List<List<Status>> poss = splitByDate(pan.pos);
            final List<List<Status>> negs = splitByDate(pan.neg);
            final RawExampleList train =
                    getFeatures(poss.get(0), negs.get(0), user);
            final RawExampleList testM1 =
                    getFeatures(poss.get(1), negs.get(1), user);
            // Take out key-author test data from all authors data
            final PosAndNeg authorTest =
                    getAuthorTest(poss.get(1), negs.get(1),
                            authorData.userProfile.getId());
            final RawExampleList testM2 =
                    getFeatures(authorTest.pos, authorTest.neg, user);

            final String followerAndExsInfo =
                    String.format("%s %d %d %d %d %d %d %d", user.userProfile
                            .getScreenName(), user.userProfile.getId(), poss
                            .get(0).size(), negs.get(0).size(), poss.get(1)
                            .size(), negs.get(1).size(), authorTest.pos.size(),
                            authorTest.neg.size());

            return new Exs(train, testM1, testM2, followerAndExsInfo);
        } else {
            return null;
        }
    }

    private static PosAndNeg getAuthorTest (List<Status> pos, List<Status> neg,
            long authorId) {
        final List<Status> np = new ArrayList<Status>();
        final List<Status> nn = new ArrayList<Status>();
        for (Status t : pos) {
            if (t.getUser().getId() == authorId) {
                np.add(t);
            }
        }
        for (Status t : neg) {
            if (t.getUser().getId() == authorId) {
                nn.add(t);
            }
        }
        return new PosAndNeg(np, nn);
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
        final ArrayList<String> fs = FeatureExtractorBackup.getFeatures(t, ud);
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
        public final List<Status> neg;

        public PosAndNeg(List<Status> pos, List<Status> neg) {
            this.pos = pos;
            this.neg = neg;
        }
    }

    private static PosAndNeg getExamples (final UserData user, UserData author) {
        // Get positive examples.
        final List<Status> pos = getPosExample(user);

        if (pos.isEmpty()) {
            return null; // No positive example.
        }
        pos2 = getPosExample2(user.tweets, author.userProfile.getId());
        // Get negative examples.
        final List<Status> neg = getNegExample(user, pos);

        return new PosAndNeg(pos, neg);
    }

    private static List<Status> getPosExample2 (ArrayList<Status> tweets,
            long authorId) {
        // Get positive examples.
        final List<Status> pos = new ArrayList<Status>();
        for (Status t : tweets) {
            if (t.isRetweet()) {
                Status t2 = t;
                while (t2.isRetweet()) { // find the original tweet of t.
                    t2 = t2.getRetweetedStatus();
                }
                // ot should within [TRAIN_START_DATE, TEST_END_DATE].
                if (t2.getCreatedAt().before(TRAIN_START_DATE)) {
                    break; // Too old.
                } else if (t2.getCreatedAt().after(TEST_END_DATE)) {
                    continue; // Too new.
                }
                // if (t2.getUser().getId() == authorId) {
                pos.add(t2);
                // }

            }
        } // for (Status t : user.tweets) {
          // Make the order from oldest to latest.
        Collections.sort(pos, TWEET_SORTER);
        return pos;
    }

    private static List<Status> getPosExample (final UserData f) {
        aposcount = 0;
        // Get positive examples.
        final List<Status> pos = new ArrayList<Status>();
        for (Status t : f.tweets) { // Order: latest to oldest.
            if (t.isRetweet()) {
                final Status ot = t.getRetweetedStatus();
                // ot should within [TRAIN_START_DATE, TEST_END_DATE].
                if (ot.getCreatedAt().before(TRAIN_START_DATE)) {
                    break; // Too old.
                } else if (ot.getCreatedAt().after(TEST_END_DATE)) {
                    continue; // Too new.
                }
                assert !ot.isRetweet(); // The original tweet of t.
                assert f.friendsIds != null; // f is a follower.
                // If the author of ot is the followee of f.
                if (f.friendsIds.contains(ot.getUser().getId())) {
                    final Date lastRetweetTime =
                            MyMath.getNewTime(ot.getCreatedAt(), POS_DAY,
                                    Calendar.DAY_OF_YEAR);
                    // It's been retweeted within the time range.
                    if (t.getCreatedAt().before(lastRetweetTime)) {
                        pos.add(ot);
                        if (ot.getUser().getId() == aid) {
                            aposcount++;
                        }
                    }
                }
            }
        } // for (Status t : user.tweets) {
        Collections.sort(pos, TWEET_SORTER);
        return pos;
    }

    private static List<Status> getNegExample (final UserData f,
            List<Status> pos) {
        anegcount = 0;
        final HashSet<Long> posSet = new HashSet<Long>();
        for (Status t : pos) {
            posSet.add(t.getId());
        }

        final List<Status> neg = new ArrayList<Status>();
        for (long authorId : f.friendsIds) { // All followees
            final UserData author = OReadWriter.getUserDate(authorId);
            if (author == null) {
                continue;
            }
            for (Status ot : author.tweets) { // All tweets.
                // ot should within [TRAIN_START_DATE, TEST_END_DATE].
                if (ot.getCreatedAt().before(TRAIN_START_DATE)) {
                    break; // Too old.
                } else if (ot.getCreatedAt().after(TEST_END_DATE)) {
                    continue; // Too new.
                }
                // Original tweet and not retweeted by f.
                if (!ot.isRetweet() && !posSet.contains(ot.getId())) {
                    neg.add(ot);
                    if (ot.getUser().getId() == aid) {
                        anegcount++;
                    }
                }
            }
        }
        // Make the order from oldest to latest.
        Collections.sort(neg, TWEET_SORTER);
        return neg;
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
        final RawExampleList exs = getFeatures(pan.pos, pan.neg, user);
        System.out.println(exs);
    }

    private static List<Status> pos2;
    private static int aposcount;
    private static int anegcount;
    private static long aid;

    private static void howManyPosRemain (UserData author) {
        if (author == null) {
            System.out.println("No such author");
            return;
        }
        aid = author.userProfile.getId();
        System.out.println("****************");
        System.out.printf("Author: %s, Id: %d%n",
                author.userProfile.getScreenName(), author.userProfile.getId());
        System.out.println("UserName UserId #Pos #AllPos #Neg "
                + "PosRemainRate PosNegRate #AuthorPos #AuthorNeg");
        assert author.followersIds != null;

        int userCount = 0;
        int poscount = 0;
        int pos2count = 0;
        int negcount = 0;
        double ppRate = 0;
        double pnRate = 0;
        int apc = 0;
        int anc = 0;
        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            if (i % (fols.length / 10) == 0) {
                // System.out.println(i + "/" + fols.length);
            }
            final Long folId = fols[i];
            final UserData follower = OReadWriter.getUserDate(folId);
            if (follower == null || follower.friendsIds == null) {
                continue;
            }
            final PosAndNeg pan = getExamples(follower, author);
            if (pan != null && pan.pos.size() > 10) {
                userCount++;
                poscount += pan.pos.size();
                pos2count += pos2.size();
                negcount += pan.neg.size();
                final double posRemainRate =
                        ((double) pan.pos.size()) / pos2.size();
                ppRate += posRemainRate;
                final double posNegRate =
                        ((double) pan.pos.size()) / pan.neg.size();
                pnRate += posNegRate;
                apc += aposcount;
                anc += anegcount;
                System.out.printf(
                        "User %s, Id %d, #Pos %d, #AllPos %d, #Neg %d, "
                                + "PosRemainRate %.4f, PosNegRate %.4f, "
                                + "#AuthorPos %d, #AuthorNeg %d %n",
                        follower.userProfile.getScreenName(), folId,
                        pan.pos.size(), pos2.size(), pan.neg.size(),
                        posRemainRate, posNegRate, aposcount, anegcount);
            }
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");
        System.out
                .println("Author Id #Pos #AllPos #Neg PosRemainRate "
                        + "AvgPosRemainRate PosNegRate AvgPosNegRate Avg#AuthorPos Avg#AuthorNeg");
        System.out.printf("%s %d %d %d %d %.4f %.4f %.4f %.4f %.4f %.4f%n",
                author.userProfile.getScreenName(), author.userProfile.getId(),
                poscount, pos2count, negcount, ((double) poscount) / pos2count,
                ppRate / userCount, ((double) poscount) / negcount, pnRate
                        / userCount, ((double) apc) / userCount, ((double) anc)
                        / userCount);
        System.out.println("****************");
    }

    public static void main (String[] args) {
        final OutputRedirection or = new OutputRedirection();
        // onePairExample();
        for (long authorId : DataCollector.AUTHOR_IDS) {
            // if (authorId == 497178013L) {
            final UserData author = OReadWriter.getUserDate(authorId);
            howManyPosRemain(author);
            // }
        }
        or.close();
    }
}
