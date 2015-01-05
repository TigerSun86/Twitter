package main;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import test.DataCollector;
import test.UserData;
import twitter4j.Status;
import util.OReadWriter;
import util.SysUtil;
import common.RawAttrList;
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

    public static void main (String[] args) {
        // onePairExample();

        for (long authorId : DataCollector.AUTHOR_IDS) {
            //if (authorId == 497178013L) {
                final UserData author = getUserDate(authorId);
                checkAuthor(author);
            //}
        }
    }

    private static void checkAuthor (UserData author) {
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

        List<Status> authorTest = getAuthorTestTweets(author.tweets);
        resultTable = new ResultTable(authorTest.size());

        int userCount = 0;
        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            if (i % (fols.length / 10) == 0) {
                // System.out.println(i + "/" + fols.length);
            }
            final Long folId = fols[i];
            final UserData user = getUserDate(folId);
            if (user == null) {
                // System.out.println("Cannot find user id " + folId);
                continue;
            }
            final PosAndNeg pan = getExamples(user, author);
            if (pan != null && pan.pos.size() > 100) {
                // System.out.println("User:" + user.userProfile.getScreenName()
                // + " id:" + folId + " pos:" + pan.pos.size() + " neg:"
                // + pan.neg.size());
                // testOne(pan.pos, pan.neg, author, user);
                testOne2(pan.pos, pan.neg, author, user, userCount, authorTest);
                userCount++;
            }
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");

        System.out
                .println("Tweet Actual#Ret Pre#Ret ErrorRate Accuracy Precision Recall FMeasure");
        for (int t = 0; t < resultTable.a.size(); t++) {
            final BitSet arow = resultTable.a.get(t);
            final BitSet prow = resultTable.p.get(t);
            int tp = 0;
            int tn = 0;
            int fp = 0;
            int fn = 0;
            for (int f = 0; f < userCount; f++) {
                final boolean a = arow.get(f);
                final boolean p = prow.get(f);
                if (a && p) {
                    tp++;
                } else if (a && !p) {
                    fn++;
                } else if (!a && p) {
                    fp++;
                } else {// !a && !p
                    tn++;
                }
            }
            final int actualRet = tp + fn;
            final int preRet = tp + fp;
            final double errorRate =
                    ((double) Math.abs(actualRet - preRet))
                            / (tp + tn + fp + fn);
            final double accuracy = ((double) tp + tn) / (tp + tn + fp + fn);
            final double precision = ((double) tp) / (tp + fp);
            final double recall = ((double) tp) / (tp + fn);
            final double fmeasure =
                    (2 * precision * recall) / (precision + recall);
            System.out.printf("t%d %d %d %.4f %.4f %.4f %.4f %.4f%n", t,
                    actualRet, preRet, errorRate, accuracy, precision, recall,
                    fmeasure);
        }
    }

    private static final RawAttrList rawAttr = new RawAttrList(
            ModelExecuter.ATTR);

    private static class ResultTable {
        public final ArrayList<BitSet> a = new ArrayList<BitSet>();
        public final ArrayList<BitSet> p = new ArrayList<BitSet>();

        public ResultTable(int numT) {
            for (int i = 0; i < numT; i++) {
                a.add(new BitSet());
                p.add(new BitSet());
            }
        }
    }

    private static ResultTable resultTable = null;

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

    private static void testOne2 (List<Status> pos, List<Status> neg,
            UserData author, UserData user, int userCount,
            List<Status> authorTest) {
        Collections.sort(pos, TWEET_SORTER);
        Collections.sort(neg, TWEET_SORTER);

        final List<List<Status>> poss = splitByDate(pos);
        final List<List<Status>> negs = splitByDate(neg);

        final long time1 = SysUtil.getCpuTime();
        final RawExampleList train =
                getFeatures(poss.get(0), negs.get(0), user);
        final RawExampleList testM1 =
                getFeatures(poss.get(1), negs.get(1), user);
        final RawExampleList testM2 =
                getAuthorExs(poss.get(1), authorTest, user);

        final long time2 = SysUtil.getCpuTime();
        final String s = ModelExecuter.run2(train, testM1, testM2, rawAttr);
        final long time3 = SysUtil.getCpuTime();

        final String[] ret = s.split("-");

        System.out.printf("%s %d %s %d %d %d %s %d %d%n",
                author.userProfile.getScreenName(), author.userProfile.getId(),
                user.userProfile.getScreenName(), user.userProfile.getId(),
                pos.size(), neg.size(), ret[0], time2 - time1, time3 - time2);

        final String[] actuals = ret[1].split(" ");
        final String[] predicts = ret[2].split(" ");

        for (int t = 0; t < actuals.length; t++) {
            resultTable.a.get(t).set(userCount, (actuals[t].equals(Y)));
            resultTable.p.get(t).set(userCount, (predicts[t].equals(Y)));
        }

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

    private static void testOne (List<Status> pos, List<Status> neg,
            UserData author, UserData user) {
        Collections.sort(pos, TWEET_SORTER);
        Collections.sort(neg, TWEET_SORTER);
        final long time1 = SysUtil.getCpuTime();

        final RawExampleList exs = getFeatures(pos, neg, user);
        final long time2 = SysUtil.getCpuTime();
        final String s = ModelExecuter.run(exs, rawAttr);
        final long time3 = SysUtil.getCpuTime();

        System.out.printf("%s %d %s %d %d %d %s %d %d%n",
                author.userProfile.getScreenName(), author.userProfile.getId(),
                user.userProfile.getScreenName(), user.userProfile.getId(),
                pos.size(), neg.size(), s, time2 - time1, time3 - time2);
    }

    private static class TweetSorter implements Comparator<Status> {
        @Override
        public int compare (Status o1, Status o2) {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        }
    }

    private static TweetSorter TWEET_SORTER = new TweetSorter();

    private static void onePairExample () {
        final long aId = 2551981338L;
        final long fId = 407374096L;

        final UserData author = getUserDate(aId);
        if (author == null) {
            // System.out.println("Cannot find user id " + folId);
            return;
        }

        final UserData user = getUserDate(fId);
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

    private static final String Y = "Y";
    private static final String N = "N";

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
        public final List<Status> neg;

        public PosAndNeg(List<Status> pos, List<Status> neg) {
            this.pos = pos;
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

            final Date al = getNewTime(tA.getCreatedAt(), -1);
            final Date ah = getNewTime(tA.getCreatedAt(), 3);
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

    private static Date getNewTime (Date time, int dif) {
        final Calendar c = Calendar.getInstance();
        c.setTime(time);
        final int h = c.get(Calendar.HOUR);
        c.set(Calendar.HOUR, h + dif);
        return c.getTime();
    }

    private static UserData getUserDate (Long id) {
        final String fullPath =
                OReadWriter.PATH + id.toString() + OReadWriter.EXT;
        final UserData ud = (UserData) OReadWriter.read(fullPath);
        return ud;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private static void writeIdToFile () {
        final HashMap<Long, String> idToFile = new HashMap<Long, String>();

        for (int count = 1; count <= 62; count++) {
            final String fileName =
                    OReadWriter.FILE_NAME + count + OReadWriter.EXT;
            final String fullPath = OReadWriter.PATH + fileName;
            final HashMap<Long, UserData> idToUser =
                    (HashMap<Long, UserData>) OReadWriter.read(fullPath);
            if (idToUser != null) {
                for (Long id : idToUser.keySet()) {
                    // Map user ids to the file storing them.
                    idToFile.put(id, fileName);
                }
            } else {
                System.out.println("Cannot read file" + fileName);
            }
        }
        // Save file indexes to file
        OReadWriter.write(idToFile, OReadWriter.PATH
                + OReadWriter.ID2FILE_FILENAME);
    }
}
