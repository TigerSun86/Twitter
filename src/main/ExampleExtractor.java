package main;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import test.DataCollector;
import test.UserData;
import twitter4j.Status;
import util.Cache;
import util.OReadWriter;

/**
 * FileName: ExampleExtractor.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Oct 30, 2014 5:04:46 PM
 */
public class ExampleExtractor {

    @SuppressWarnings("unchecked")
    public static void main (String[] args) {
        final HashMap<Long, String> idToFile =
                (HashMap<Long, String>) OReadWriter.read(OReadWriter.PATH
                        + OReadWriter.ID2FILE_FILENAME);
        final Cache<HashMap<Long, UserData>> cache =
                new Cache<HashMap<Long, UserData>>(10);
        for (long authorId : DataCollector.AUTHOR_IDS) {
            final UserData author = getUserDate(authorId, idToFile, cache);
            checkAuthor(author, idToFile, cache);
        }

    }

    private static void
            checkAuthor (UserData author, HashMap<Long, String> idToFile,
                    Cache<HashMap<Long, UserData>> cache) {
        if (author == null) {
            System.out.println("No such author");
            return;
        }
        System.out.println("Author is " + author.userProfile.getScreenName()
                + " id:" + author.userProfile.getId());
        assert author.followersIds != null;
        final ArrayList<Example> exs = new ArrayList<Example>();
        final Long[] fols = author.followersIds.toArray(new Long[0]);
        for (int i = 0; i < fols.length; i++) {
            if (i % (fols.length / 10) == 0) {
                System.out.println(i + "/" + fols.length);
            }
            final Long folId = fols[i];
            final UserData user = getUserDate(folId, idToFile, cache);
            if (user == null) {
                // System.out.println("Cannot find user id " + folId);
                continue; // No such user.
            }

            // Get positive examples.
            final HashMap<Long, Status> pos =
                    getPosExample(user.tweets, author.userProfile.getId());

            if (pos.isEmpty()) {
                continue; // No positive example.
            }
            // Get negative examples.
            final HashMap<Long, Status> neg =
                    getNegExample(author.tweets, user.tweets, pos);

            if (pos.size() != 0 && neg.size() != 0) {
                final Example e =
                        new Example(user.userProfile.getScreenName(),
                                user.userProfile.getId(), pos.size(),
                                neg.size());

                exs.add(e);
            }
        } // for (Long folId : author.followersIds) {
        System.out.println("****************");
        System.out.println("Author is " + author.userProfile.getScreenName()
                + " id:" + author.userProfile.getId());
        for (Example e : exs) {
            System.out.println(e);
        }
        System.out.println("****************");
    }

    private static class Example {
        String name;
        long id;
        int pos;
        int neg;

        public Example(String name, long id, int pos, int neg) {
            super();
            this.name = name;
            this.id = id;
            this.pos = pos;
            this.neg = neg;
        }

        @Override
        public String toString () {
            return "User:" + name + " id:" + id + " pos:" + pos + " neg:" + neg;
        }
    }

    private static HashMap<Long, Status> getPosExample (
            ArrayList<Status> tweets, long authorId) {
        // Get positive examples.
        final HashMap<Long, Status> pos = new HashMap<Long, Status>();
        for (Status t : tweets) {
            if (t.isRetweet()) {
                Status t2 = t;
                while (t2.isRetweet()) { // find the original tweet of t.
                    t2 = t2.getRetweetedStatus();
                }
                if (t2.getUser().getId() == authorId) {
                    // The author of t is AUTHOR_ID.
                    // Add positive example t2.
                    pos.put(t2.getId(), t2);
                }
            }
        } // for (Status t : user.tweets) {
        return pos;
    }

    private static HashMap<Long, Status> getNegExample (
            ArrayList<Status> atweets, ArrayList<Status> ftweets,
            HashMap<Long, Status> pos) {
        // Get negative examples.
        // Negative example is the tweet t from author wasn't retweeted by
        // follower, and in the time interval [1 hour before t, 3 hour after t]
        // the follower has some activity.
        final HashMap<Long, Status> neg = new HashMap<Long, Status>();
        int i = 0;
        int j = 0;
        while (i < atweets.size() && j < ftweets.size()) {
            final Status tA = atweets.get(i);
            if (pos.containsKey(tA.getId()) || tA.isRetweet()) {
                i++; // It's a positive example or it's not a original tweet.
                continue;
            }
            final Status tF = ftweets.get(j);
            final Date al = getNewTime(tA.getCreatedAt(), -1);
            final Date ah = getNewTime(tA.getCreatedAt(), 3);
            final Date f = tF.getCreatedAt();
            if (f.before(al)) { // The tweet of follower is too early.
                j++; // Next follower t.
            } else if (f.after(ah)) { // The tweet of follower is too late.
                i++; // Next author t.
            } else { // The tweet of follower is just in the interval.
                neg.put(tA.getId(), tA);
                i++; // Next author t.
            }
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

    @SuppressWarnings("unchecked")
    private static UserData
            getUserDate (Long id, HashMap<Long, String> idToFile,
                    Cache<HashMap<Long, UserData>> cache) {
        final String fileName = idToFile.get(id);
        if (fileName == null) {
            return null;  // No such id.
        }
        HashMap<Long, UserData> idToUser = cache.get(fileName);
        if (idToUser == null) { // Not in the cache.
            final String fullPath = OReadWriter.PATH + fileName;
            idToUser = (HashMap<Long, UserData>) OReadWriter.read(fullPath);
            if (idToUser == null) {
                return null; // No such file.
            } else {
                cache.put(fileName, idToUser); // Put into cache for future use.
            }
        }

        final UserData user = idToUser.get(id);
        assert user != null;
        return user;
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
