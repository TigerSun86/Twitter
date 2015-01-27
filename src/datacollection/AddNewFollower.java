package datacollection;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

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

    private DBCollection wtColl = null;
    private DBCollection userInfosColl = null;
    private TwitterApi tapi = null;
    private HashMap<Long, UserInfo> keyAus = null;

    private boolean connectDB () {
        boolean suc = true;
        try {
            MongoClient mongoClient = new MongoClient();
            DB otherDb = mongoClient.getDB("other");
            wtColl = otherDb.getCollection("waitingTweets");
            userInfosColl = otherDb.getCollection("userInfos");
        } catch (UnknownHostException e) {
            suc = false;
            e.printStackTrace();
        }
        return suc;
    }

    private boolean init () {
        if (!connectDB()) {
            return false;
        }
        tapi = new TwitterApi();
        keyAus = new HashMap<Long, UserInfo>();
        for (long id : UserInfo.KEY_AUTHORS) {
            final UserInfo user = UserInfo.getUser(id, userInfosColl);
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
            // Find the first inserted tweet.
            final DBCursor cursor =
                    wtColl.find().sort(new BasicDBObject("_id", 1)).limit(1);
            if (cursor.hasNext()) {
                final DBObject dbObject = cursor.next();
                // Remove from waiting list
                wtColl.remove(dbObject);
                dbObject.removeField("_id");
                final String json = dbObject.toString();
                Status t = null;
                try {
                    t = TwitterObjectFactory.createStatus(json);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
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

        final List<Status> retweets = tapi.getRetweets(t.getId());
        if (retweets == null) {
            return; // t has already been deleted.
        }
        final long auId = t.getUser().getId();
        final UserInfo au = keyAus.get(auId);
        assert au != null;
        for (Status r : retweets) {
            final long userId = r.getUser().getId();
            if (!au.followersIds.contains(userId)) { // Haven't added the user.
                final boolean suc = crawlAndFollowAndStoreUser(userId);
                if (suc) {// If false means user is private, just skip.
                    // Add friendship between key author and the follower.
                    UserInfo.addFriendOrFollower(userId, auId, true,
                            userInfosColl);
                    UserInfo.addFriendOrFollower(auId, userId, false,
                            userInfosColl);
                    // Also update global variable.
                    au.followersIds.add(userId);
                } // if (suc) {
            } // if (!au.followersIds.contains(userId)) {
        } // for (Status r : retweets) {
    }

    /** @return true, success; false, failed by user is private. */
    private boolean crawlAndFollowAndStoreUser (long userId) {
        if (UserInfo.getUser(userId, userInfosColl) != null) {
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
        UserInfo.storeUser(user, userInfosColl); // Store user in data base.
        return true;
    }

    public static void main (String[] args) {
        new AddNewFollower().run();
    }
}
