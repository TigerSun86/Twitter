package datacollection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.Status;
import twitter4j.User;
import util.OutputRedirection;
import datacollection.Database.StatusAndCheckedTime;

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
    // The checking times for a tweets, how many times to check within one day.
    // The value is updated from database when every usage.
    private int freq = 1;

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
            StatusAndCheckedTime tandc = db.peekWaitingTweet();
            if (tandc != null) {
                waitUnilTimeToCheck(tandc.tweet.getCreatedAt(), tandc.date);
                // Peek the tweet again to prevent it's been deleted while last
                // waiting.
                tandc = db.peekWaitingTweet();
                if (tandc != null) {
                    checkTweet(tandc.tweet);
                }
            } else { // There is no waiting tweets.
                try { // Sleep for a while.
                    System.out
                            .println("No waiting tweets, let me sleep for a while.");
                    Thread.sleep(DAY_IN_MILLISECONDS / 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } // if (cursor.hasNext()) {
        } // while (true) {
    }

    private long getNextCheckPoint (Date createAt, Date lastCheckedTime) {
        final long timeInterval = DAY_IN_MILLISECONDS / freq;
        if (lastCheckedTime == null) { // Haven't check once.
            return createAt.getTime() + timeInterval;
        } else {
            // Find the first check point later than last checked time.
            long checkPoint = createAt.getTime();
            for (int i = 0; i < freq; i++) {
                checkPoint += timeInterval;
                if (checkPoint > lastCheckedTime.getTime()) {
                    break;
                }
            }
            return checkPoint;
        }
    }

    private void waitUnilTimeToCheck (Date createAt, Date lastCheckedTime) {
        // Update frequence.
        freq = db.getWaitingTweetsCheckingFrequence();
        assert freq >= 1;
        final long checkPoint = getNextCheckPoint(createAt, lastCheckedTime);
        final long now = new Date().getTime();
        if (now < checkPoint) { // Sleep until the time to check.
            try {
                Thread.sleep(checkPoint - now);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkTweet (Status t) {
        // Add followers
        final boolean suc = checkRetweetsAndAddFollowers(t);
        // Firstly, remove this tweet from the peek of the queue.
        // t2 for the method TwitterObjectFactory.getRawJSON(status)
        // inside db.putWaitingTweet().
        final String tjson = db.pollWaitingTweetInJson();
        if (suc && needMoreCheck(t.getCreatedAt())) {
            // If the tweet needs more check, push it into the queue
            // and check later.
            db.putWaitingTweet(tjson, new Date());
        } else if (suc) { // If the last check succeed.
            // Update the tweet from key author, for the number of
            // retweets in the field of tweet.
            final Status updatedT = tapi.showStatus(t.getId());
            db.updateTweet(updatedT);
        }
    }

    private boolean needMoreCheck (Date createAt) {
        // Need more check when now is not the final check, which is one day
        // after the created time.
        return new Date().getTime() < createAt.getTime() + DAY_IN_MILLISECONDS;
    }

    private boolean checkRetweetsAndAddFollowers (Status t) {
        assert t != null;
        final long timeInterval = DAY_IN_MILLISECONDS / freq;
        int times =
                (int) ((new Date().getTime() - t.getCreatedAt().getTime()) / timeInterval);
        times = Math.min(times, freq);
        System.out
                .printf("Checking tweet: %d, createdAt: %s, currentTime: %s, times is %d/%d. ",
                        t.getId(), t.getCreatedAt().toString(),
                        new Date().toString(), times, freq);
        final List<Status> retweets = tapi.getRetweets(t.getId());
        if (retweets == null) {
            System.out.println("Cannot get retweet.");
            return false; // t has already been deleted.
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
        return true;
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
        db.putUser(user);
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
