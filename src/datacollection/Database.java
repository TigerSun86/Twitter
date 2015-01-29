package datacollection;

import java.net.UnknownHostException;
import java.util.Date;

import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

/**
 * FileName: Database.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 27, 2015 7:48:16 PM
 */
public class Database {
    private static final String DB_TWEETS = "tweets";
    private static final String DB_OTHER = "other";
    private static final String COLL_USERINFOS = "userInfos";
    private static final String COLL_WAITINGTWEETS = "waitingTweets";
    private static final String COLL_TRENDS = "trends";
    private static final String COLL_PARAMETERS = "parameters";
    // UserInfo fields.
    private static final String FEILD_ID = "UserId";
    private static final String FEILD_UP = "UserProfile";
    private static final String FEILD_TIME = "CrawledAtTime";
    private static final String FEILD_FRI = "FriendsIds";
    private static final String FEILD_FOL = "FollowersIds";
    // Trends fields.
    private static final String FEILD_TRENDS_DATE = "TrendsDate";
    private static final String FEILD_TRENDS_INFO = "TrendsInfo";
    // Tweet fields.
    private static final String FEILD_TWEET_ID = "id";
    // WaitingTweets fields
    private static final String FEILD_WT_DATE = "LastCheckedDate";
    // Parameter fields
    private static final String FEILD_PARA_MODEL = "Model";
    private static final String PARA_MODEL_WT = "waitingTweets";
    private static final String FEILD_PARA_WT_FREQ = "Frequence";

    private static final String COLL_NAME_PREFIX = "u";

    private MongoClient mongoClient = null;
    private DB tweetsDb = null;

    private DBCollection userInfosColl = null;
    private DBCollection wtColl = null;
    private DBCollection trendsColl = null;
    private DBCollection parametersColl = null;

    private Database() {
    }

    public static Database getInstance () {
        final Database db = new Database();
        try {
            db.mongoClient = new MongoClient();
        } catch (UnknownHostException e) {
            db.mongoClient = null;
            e.printStackTrace();
        }
        if (db.mongoClient != null) {
            return db;
        } else {
            return null;
        }
    }

    public DB getTweetsDb () {
        if (tweetsDb == null) {
            tweetsDb = mongoClient.getDB(DB_TWEETS);
        }
        return tweetsDb;
    }

    public DBCollection getUserInfosColl () {
        if (userInfosColl == null) {
            userInfosColl =
                    mongoClient.getDB(DB_OTHER).getCollection(COLL_USERINFOS);
        }
        return userInfosColl;
    }

    public DBCollection getWaitTweetsColl () {
        if (wtColl == null) {
            wtColl =
                    mongoClient.getDB(DB_OTHER).getCollection(
                            COLL_WAITINGTWEETS);
        }
        return wtColl;
    }

    public DBCollection getTrendsColl () {
        if (trendsColl == null) {
            trendsColl = mongoClient.getDB(DB_OTHER).getCollection(COLL_TRENDS);
        }
        return trendsColl;
    }

    public DBCollection getParametersColl () {
        if (parametersColl == null) {
            parametersColl =
                    mongoClient.getDB(DB_OTHER).getCollection(COLL_PARAMETERS);
        }
        return parametersColl;
    }

    /* UserInfo methods begin ********************* */
    public UserInfo getUser (long userId) {
        final DBCollection coll = this.getUserInfosColl();
        // Search user.
        final BasicDBObject query = new BasicDBObject(FEILD_ID, userId);
        final DBCursor cursor = coll.find(query);
        if (!cursor.hasNext()) { // No such user.
            return null;
        }
        // doc is the user info stored in data base.
        final DBObject doc = cursor.next();
        User userProfile = null;
        final String json = doc.get(FEILD_UP).toString();
        try {
            userProfile = TwitterObjectFactory.createUser(json);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        final Date date = (Date) doc.get(FEILD_TIME);

        final UserInfo user = new UserInfo(userId, userProfile, date);

        for (Object id : (BasicDBList) doc.get(FEILD_FRI)) {
            user.friendsIds.add((Long) id);
        }

        for (Object id : (BasicDBList) doc.get(FEILD_FOL)) {
            user.followersIds.add((Long) id);
        }
        return user;
    }

    public void putUser (UserInfo user) {
        final DBCollection coll = this.getUserInfosColl();
        // make a document and insert it
        final BasicDBObject doc =
                new BasicDBObject(FEILD_ID, user.userId)
                        .append(FEILD_UP,
                                TwitterObjectFactory
                                        .getRawJSON(user.userProfile))
                        .append(FEILD_TIME, user.crawledAt)
                        .append(FEILD_FRI, user.friendsIds)
                        .append(FEILD_FOL, user.followersIds);
        coll.insert(doc);
    }

    /**
     * @param boolean addFriend: true, add friend; false, add follower.
     * @return true, success; false, no such user.
     */

    public boolean addFriendOrFollower (long userId, long idToAdd,
            boolean addFriend) {
        final DBCollection coll = this.getUserInfosColl();
        final String friendOrFollower = (addFriend ? FEILD_FRI : FEILD_FOL);
        final BasicDBObject findQuery = new BasicDBObject(FEILD_ID, userId);
        final DBCursor cursor = coll.find(findQuery);
        if (!cursor.hasNext()) { // No such user.
            return false;
        }
        final DBObject doc = cursor.next();
        for (Object id : (BasicDBList) doc.get(friendOrFollower)) {
            if ((long) id == idToAdd) {
                return true; // Already has it.
            }
        }

        final BasicDBObject newItem =
                new BasicDBObject(friendOrFollower, idToAdd);
        final BasicDBObject updateQuery = new BasicDBObject("$push", newItem);
        coll.update(findQuery, updateQuery);
        return true;
    }

    public void updateTweet (Status status) {
        final String str = TwitterObjectFactory.getRawJSON(status);
        final DBObject dbObject = (DBObject) JSON.parse(str);
        final long userId = status.getUser().getId();
        final DBCollection coll =
                this.getTweetsDb().getCollection(COLL_NAME_PREFIX + userId);
        // Search tweet by Id.
        final BasicDBObject queryDoc =
                new BasicDBObject(FEILD_TWEET_ID, status.getId());
        coll.update(queryDoc, dbObject);
    }

    /* UserInfo methods end ********************* */

    /* MyUserStream methods begin ************** */
    public void putTweet (Status status) {
        final String str = TwitterObjectFactory.getRawJSON(status);
        final DBObject dbObject = (DBObject) JSON.parse(str);
        final long userId = status.getUser().getId();
        final DBCollection coll =
                this.getTweetsDb().getCollection(COLL_NAME_PREFIX + userId);
        coll.insert(dbObject);
    }

    public void putWaitingTweet (Status status) {
        final String str = TwitterObjectFactory.getRawJSON(status);
        final DBObject dbObject = (DBObject) JSON.parse(str);
        this.getWaitTweetsColl().insert(dbObject);
    }

    /**
     * For AddNewFollower
     * Use TwitterObjectFactory.getRawJSON(status) here will return null, so
     * just use the original json.
     */
    public void putWaitingTweet (String json, Date lastCheckedDate) {
        final DBObject dbObject = (DBObject) JSON.parse(json);
        dbObject.put(FEILD_WT_DATE, lastCheckedDate);
        this.getWaitTweetsColl().insert(dbObject);
    }

    public void removeTweet (long userId, long tweetId) {
        // Remove tweet in tweets DB.
        final DBCollection coll =
                this.getTweetsDb().getCollection(COLL_NAME_PREFIX + userId);
        coll.remove(new BasicDBObject(FEILD_TWEET_ID, tweetId));
        // Also remove it from waiting collection (if there is).
        if (UserInfo.KEY_AUTHORS.contains(userId)) {
            this.getWaitTweetsColl().remove(
                    new BasicDBObject(FEILD_TWEET_ID, tweetId));
        }
    }

    /* MyUserStream methods end ************** */

    /* AddNewFollower begin ******************** */
    public static class StatusAndCheckedTime {
        public final Status tweet;
        public final Date date;

        public StatusAndCheckedTime(Status tweet, Date date) {
            super();
            this.tweet = tweet;
            this.date = date;
        }
    }

    public StatusAndCheckedTime pollWaitingTweet () {
        // Find the first inserted tweet.
        final DBCursor cursor =
                this.getWaitTweetsColl().find()
                        .sort(new BasicDBObject("_id", 1)).limit(1);
        if (cursor.hasNext()) {
            final DBObject dbObject = cursor.next();
            // Remove from waiting list
            this.getWaitTweetsColl().remove(dbObject);

            final Date lastCheckedDate;
            if (dbObject.get(FEILD_WT_DATE) != null) {
                lastCheckedDate = (Date) dbObject.get(FEILD_WT_DATE);
                dbObject.removeField(FEILD_WT_DATE);
            } else {
                lastCheckedDate = null;
            }

            dbObject.removeField("_id");
            final String json = dbObject.toString();
            Status t = null;
            try {
                t = TwitterObjectFactory.createStatus(json);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return new StatusAndCheckedTime(t, lastCheckedDate);
        } else {
            return null;
        }
    }

    public String pollWaitingTweetInJson () {
        // Find the first inserted tweet.
        final DBCursor cursor =
                this.getWaitTweetsColl().find()
                        .sort(new BasicDBObject("_id", 1)).limit(1);
        if (cursor.hasNext()) {
            final DBObject dbObject = cursor.next();
            // Remove from waiting list
            this.getWaitTweetsColl().remove(dbObject);

            if (dbObject.get(FEILD_WT_DATE) != null) {
                dbObject.removeField(FEILD_WT_DATE);
            }
            dbObject.removeField("_id");
            final String json = dbObject.toString();
            return json;
        } else {
            return null;
        }
    }

    public StatusAndCheckedTime peekWaitingTweet () {
        // Find the first inserted tweet.
        final DBCursor cursor =
                this.getWaitTweetsColl().find()
                        .sort(new BasicDBObject("_id", 1)).limit(1);
        if (cursor.hasNext()) {
            final DBObject dbObject = cursor.next();

            final Date lastCheckedDate;
            if (dbObject.get(FEILD_WT_DATE) != null) {
                lastCheckedDate = (Date) dbObject.get(FEILD_WT_DATE);
                dbObject.removeField(FEILD_WT_DATE);
            } else {
                lastCheckedDate = null;
            }

            dbObject.removeField("_id");
            final String json = dbObject.toString();
            Status t = null;
            try {
                t = TwitterObjectFactory.createStatus(json);
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return new StatusAndCheckedTime(t, lastCheckedDate);
        } else {
            return null;
        }
    }

    public int getWaitingTweetsCheckingFrequence () {
        final DBCollection coll = this.getParametersColl();
        DBObject doc =
                coll.findOne(new BasicDBObject(FEILD_PARA_MODEL, PARA_MODEL_WT));
        final int frequence;
        final Object para = doc.get(FEILD_PARA_WT_FREQ);
        if (para != null && ((double) para) >= 1) {
            frequence = (int) ((double) para);
        } else {
            frequence = 1;
        }
        return frequence;
    }

    /* AddNewFollower end ******************** */

    /* TrendsCollector begin *************** */
    public void putTrends (Trends trends) {
        final DBCollection coll = this.getTrendsColl();
        final BasicDBObject doc =
                new BasicDBObject(FEILD_TRENDS_DATE, new Date().getTime())
                        .append(FEILD_TRENDS_INFO,
                                TwitterObjectFactory.getRawJSON(trends));
        coll.insert(doc);
    }

    /** @return trends with closest time to the specified date. */
    public Trends getTrends (Date date) {
        final long queryTime = date.getTime();
        final DBCollection coll = this.getTrendsColl();
        // Find closest older one.
        DBCursor cursor =
                coll.find(
                        new BasicDBObject(FEILD_TRENDS_DATE, new BasicDBObject(
                                "$lte", queryTime)))
                        .sort(new BasicDBObject(FEILD_TRENDS_DATE, -1))
                        .limit(1);
        final DBObject closestBelow = (cursor.hasNext() ? cursor.next() : null);
        // Find closest later one.
        cursor =
                coll.find(
                        new BasicDBObject(FEILD_TRENDS_DATE, new BasicDBObject(
                                "$gt", queryTime)))
                        .sort(new BasicDBObject(FEILD_TRENDS_DATE, 1)).limit(1);
        final DBObject closestAbove = (cursor.hasNext() ? cursor.next() : null);
        assert closestBelow != null || closestAbove != null;
        final DBObject closest;
        if (closestBelow == null) {
            closest = closestAbove;
        } else if (closestAbove == null) {
            closest = closestBelow;
        } else {
            final long belowDiff =
                    Math.abs(queryTime
                            - (long) closestBelow.get(FEILD_TRENDS_DATE));
            final long aboveDiff =
                    Math.abs(queryTime
                            - (long) closestAbove.get(FEILD_TRENDS_DATE));
            if (belowDiff < aboveDiff) {
                closest = closestBelow;
            } else {
                closest = closestAbove;
            }
        }
        final String json = (String) closest.get(FEILD_TRENDS_INFO);
        Trends trends = null;
        try {
            trends = TwitterObjectFactory.createTrends(json);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        return trends;
    }
    /* TrendsCollector end *************** */
}
