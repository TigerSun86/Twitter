package datacollection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.Status;
import twitter4j.User;
import util.OutputRedirection;

/**
 * FileName: AddNewFollower.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 22, 2015 8:43:34 PM
 */
public class AddNewFollower {
    private static final int POS_DAY = 1;
    private static final long DAY_IN_MILLISECONDS = TimeUnit.MILLISECONDS
            .convert(POS_DAY, TimeUnit.DAYS);

    private Database db = null;
    private TwitterApi tapi = null;
    private HashMap<Long, UserInfo> keyAus = null;

    private boolean init () {
        db = Database.getInstance();
        if (db == null) {
            return false;
        }

        tapi = new TwitterApi();
        keyAus = new HashMap<Long, UserInfo>();
        for (long id : UserInfo.KEY_AUTHORS) {
            final UserInfo user = db.getUser(id);
            keyAus.put(id, user);
        }
        return true;
    }

    private void run () {
        if (!init()) {
            System.out.println("Data base problem.");
            return;
        }

        while (true) {
            final Status t = db.pollFirstWaitingTweet();
            if (t != null) {
                // Use long won't change the original Date object.
                final long targetTime =
                        t.getCreatedAt().getTime() + DAY_IN_MILLISECONDS;
                long curTime = new Date().getTime();
                while (targetTime > curTime) { // Sleep until it's target time.
                    try {
                        Thread.sleep(targetTime - curTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    curTime = new Date().getTime();
                }

                // Add followers
                checkRetweetsAndAddFollowers(t);
            } else { // There is no waiting tweets.
                try { // Sleep a whole day.
                    System.out
                            .println("No waiting tweets, let me sleep a whole day.");
                    Thread.sleep(TimeUnit.MILLISECONDS.convert(POS_DAY,
                            TimeUnit.DAYS));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } // if (cursor.hasNext()) {
        } // while (true) {
    }

    private void checkRetweetsAndAddFollowers (Status t) {
        assert t != null;
        System.out.printf(
                "Checking tweet: %d, createdAt: %s, currentTime: %s. ",
                t.getId(), t.getCreatedAt().toString(), new Date().toString());
        final List<Status> retweets = tapi.getRetweets(t.getId());
        if (retweets == null) {
            System.out.println("Cannot get retweet.");
            return; // t has already been deleted.
        }
        System.out.println("Num of retweets: " + retweets.size() + ".");
        final long auId = t.getUser().getId();
        final UserInfo au = keyAus.get(auId);
        assert au != null;
        for (Status r : retweets) {
            final long userId = r.getUser().getId();
            if (!au.followersIds.contains(userId)) { // Haven't added the user.
                final boolean suc = crawlAndFollowAndStoreUser(userId);
                if (suc) {// If false means user is private, just skip.
                    // Add friendship between key author and the follower.
                    db.addFriendOrFollower(userId, auId, true);
                    db.addFriendOrFollower(auId, userId, false);
                    // Also update global variable.
                    au.followersIds.add(userId);
                    System.out.println("Author "
                            + au.userProfile.getScreenName()
                            + " had a new follower: " + userId);
                } // if (suc) {
            } // if (!au.followersIds.contains(userId)) {
        } // for (Status r : retweets) {
    }

    /** @return true, success; false, failed by user is private. */
    private boolean crawlAndFollowAndStoreUser (long userId) {
        if (db.getUser(userId) != null) {
            return true; // Already has the user.
        }
        // Crawl user profile.
        final User userProfile = tapi.getUserProfile(userId);
        if (userProfile == null) { // Private user.
            return false;
        }
        // Follow this user.
        if (!tapi.createFriendship(userId)) {
            return false; // Failed.
        }
        final UserInfo user = new UserInfo(userId, userProfile, new Date());
        // Store user in data base.
        db.storeUser(user);
        System.out.printf("Followed id: %d, screen name: %s, time: %s.%n",
                user.userId, user.userProfile.getScreenName(),
                new Date().toString());
        return true;
    }

    public static void main (String[] args) {
        final String curTime =
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        final String file =
                "D:/TwitterDB/stream/waitingTweetsLog_" + curTime + ".txt";
        final OutputRedirection or = new OutputRedirection(file);
        new AddNewFollower().run();
        or.close();
    }
}
