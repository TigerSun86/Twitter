package datacollection;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import main.ExampleGetter;
import main.ExampleGetter.PosAndNeg;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;

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
    private static final String[] DB_TWEETS_NAMES = { "tweets", "tweets2",
            "tweets3" };
    private static final String DB_OTHER = "other";
    private static final String COLL_USERINFOS = "userInfos";
    private static final String COLL_WAITINGTWEETS = "waitingTweets";
    private static final String COLL_TRENDS = "trends";
    private static final String COLL_PARAMETERS = "parameters";
    private static final String COLL_CB = "cb";
    private static final String COLL_STREAMUSERS = "streamUsers";

    private static final String FEILD_GENERAL_ID = "id";

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
    private static final String FEILD_WT_USERID = "UserId";
    private static final String FEILD_WT_TWEETID = "TweetId";
    private static final String FEILD_WT_DATE = "LastCheckedDate";
    // Parameter fields
    private static final String FEILD_PARA_MODEL = "Model";
    private static final String PARA_MODEL_WT = "waitingTweets";
    private static final String FEILD_PARA_WT_FREQ = "Frequence";
    private static final String PARA_MODEL_RDC = "restDataCollector";
    private static final String FEILD_PARA_RDC_SLEEPTIME = "SleepTime";

    // CB fields
    private static final String FEILD_CBANDCRAWL_ID = "cbId";
    private static final String FEILD_CB_CK = "consumerKey";
    private static final String FEILD_CB_CS = "consumerSecret";
    private static final String FEILD_CB_AT = "accessToken";
    private static final String FEILD_CB_ATS = "accessTokenSecret";
    private static final String FEILD_CB_DEBUG = "debug";
    private static final String FEILD_CB_JSON = "jsonStoreEnabled";

    private static final String COLL_NAME_PREFIX = "u";

    private MongoClient mongoClient = null;
    private DB[] tweetsDbs = new DB[DB_TWEETS_NAMES.length];

    private DBCollection userInfosColl = null;
    private DBCollection wtColl = null;
    private DBCollection trendsColl = null;
    private DBCollection parametersColl = null;
    private DBCollection cbColl = null;
    private DBCollection streamUsersColl = null;

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

    public DB getTweetsDb (int index) {
        assert index >= 0 && index < tweetsDbs.length;
        if (tweetsDbs[index] == null) {
            tweetsDbs[index] = mongoClient.getDB(DB_TWEETS_NAMES[index]);
        }
        return tweetsDbs[index];
    }

    private DBCollection getTweetsDbCollection (long userId) {
        final int indexOfLastDb = tweetsDbs.length - 1;
        DBCollection coll = null;
        for (int i = 0; i <= indexOfLastDb; i++) {
            coll = this.getTweetsDb(i).getCollection(COLL_NAME_PREFIX + userId);
            if (coll.count() > 0) {
                break; // Found the collection.
            }
        }
        if (coll == null || coll.count() == 0) {
            // Didn't find, just use the last db to store it.
            coll =
                    this.getTweetsDb(indexOfLastDb).getCollection(
                            COLL_NAME_PREFIX + userId);
        }
        return coll;
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

    public DBCollection getCbColl () {
        if (cbColl == null) {
            cbColl = mongoClient.getDB(DB_OTHER).getCollection(COLL_CB);
        }
        return cbColl;
    }

    public DBCollection getStreamUsersColl () {
        if (streamUsersColl == null) {
            streamUsersColl =
                    mongoClient.getDB(DB_OTHER).getCollection(COLL_STREAMUSERS);
        }
        return streamUsersColl;
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

    public List<Long> getAllUsers () {
        final ArrayList<Long> users = new ArrayList<Long>();
        final DBCollection coll = this.getUserInfosColl();
        final DBCursor cursor = coll.find();
        while (cursor.hasNext()) {
            final DBObject doc = cursor.next();
            final Long userId = (Long) doc.get(FEILD_ID);
            users.add(userId);
        }
        return users;
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
        final DBCollection coll = getTweetsDbCollection(userId);
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
        final DBCollection coll = getTweetsDbCollection(userId);
        coll.insert(dbObject);
    }

    public void putTweet (long userId, String status) {
        final DBObject dbObject = (DBObject) JSON.parse(status);
        final DBCollection coll = getTweetsDbCollection(userId);
        coll.insert(dbObject);
    }

    public void
            putWaitingTweet (long userId, long tweetId, Date lastCheckedDate) {
        final BasicDBObject doc =
                new BasicDBObject(FEILD_WT_USERID, userId).append(
                        FEILD_WT_TWEETID, tweetId).append(FEILD_WT_DATE,
                        lastCheckedDate);
        this.getWaitTweetsColl().insert(doc);
    }

    public void removeTweet (long userId, long tweetId) {
        // Remove tweet in tweets DB.
        final DBCollection coll = getTweetsDbCollection(userId);
        coll.remove(new BasicDBObject(FEILD_TWEET_ID, tweetId));
    }

    public Status getTweet (long userId, long tweetId) {
        // Get tweet in tweets DB.
        final DBCollection coll = getTweetsDbCollection(userId);
        final DBObject doc =
                coll.findOne(new BasicDBObject(FEILD_TWEET_ID, tweetId));
        if (doc != null) {
            final Status t = docToTweet(doc);
            return t;
        } else {
            return null;
        }
    }

    public List<Status> getTweetList (long userId) {
        final List<Status> tweets = new ArrayList<Status>();
        // Get tweet in tweets DB.
        final DBCollection coll = getTweetsDbCollection(userId);
        // Older to Later.
        final DBCursor cursor = coll.find().sort(new BasicDBObject("_id", 1));
        while (cursor.hasNext()) {
            final DBObject doc = cursor.next();
            final Status t = docToTweet(doc);
            tweets.add(t);
        }
        return tweets;
    }

    public Status getLatestTweet (long userId) {
        final DBCollection coll = getTweetsDbCollection(userId);
        // Find the last inserted one.
        final DBCursor cursor =
                coll.find().sort(new BasicDBObject("_id", -1)).limit(1);
        if (cursor.hasNext()) {
            final DBObject dbObject = cursor.next();
            final Status t = docToTweet(dbObject);
            return t;
        } else {
            return null;
        }
    }

    /* MyUserStream methods end ************** */

    /* AddNewFollower begin ******************** */
    public static class StatusAndCheckedTime {
        public final long userId;
        public final long tweetId;
        public final Date date;

        public StatusAndCheckedTime(long userId, long tweetId, Date date) {
            super();
            this.userId = userId;
            this.tweetId = tweetId;
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
            final long userId = (long) dbObject.get(FEILD_WT_USERID);
            final long tweetId = (long) dbObject.get(FEILD_WT_TWEETID);
            final Date lastCheckedDate = (Date) dbObject.get(FEILD_WT_DATE);
            return new StatusAndCheckedTime(userId, tweetId, lastCheckedDate);
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
            final long userId = (long) dbObject.get(FEILD_WT_USERID);
            final long tweetId = (long) dbObject.get(FEILD_WT_TWEETID);
            final Date lastCheckedDate = (Date) dbObject.get(FEILD_WT_DATE);
            return new StatusAndCheckedTime(userId, tweetId, lastCheckedDate);

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
        if (para != null && ((long) para) >= 1) {
            frequence = (int) ((long) para);
        } else {
            frequence = 1;
        }
        return frequence;
    }

    /* AddNewFollower end ******************** */

    /* Stream users begin ******************** */
    public boolean existStreamUser (long userId) {
        final DBCollection coll = this.getStreamUsersColl();
        final BasicDBObject query = new BasicDBObject(FEILD_GENERAL_ID, userId);
        final DBCursor cursor = coll.find(query);
        return cursor.hasNext();
    }

    /* Stream users end ******************** */

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

    /* Cb begin ********************* */
    public ArrayList<ConfigurationBuilder> getCbs () {
        final ArrayList<ConfigurationBuilder> cbs =
                new ArrayList<ConfigurationBuilder>();

        final DBCollection coll = this.getCbColl();
        final DBCursor cursor =
                coll.find().sort(new BasicDBObject(FEILD_CBANDCRAWL_ID, 1));

        while (cursor.hasNext()) {
            final DBObject doc = cursor.next();
            final ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setOAuthConsumerKey((String) doc.get(FEILD_CB_CK))
                    .setOAuthConsumerSecret((String) doc.get(FEILD_CB_CS))
                    .setOAuthAccessToken((String) doc.get(FEILD_CB_AT))
                    .setOAuthAccessTokenSecret((String) doc.get(FEILD_CB_ATS))
                    .setDebugEnabled((boolean) doc.get(FEILD_CB_DEBUG))
                    .setJSONStoreEnabled((boolean) doc.get(FEILD_CB_JSON));
            cbs.add(cb);
        }
        return cbs;
    }

    public ConfigurationBuilder getCb (long cbId) {
        final DBCollection coll = this.getCbColl();
        final BasicDBObject query =
                new BasicDBObject(FEILD_CBANDCRAWL_ID, cbId);
        final DBCursor cursor = coll.find(query);
        if (!cursor.hasNext()) { // No such cb.
            return null;
        } else {
            final DBObject doc = cursor.next();
            final ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setOAuthConsumerKey((String) doc.get(FEILD_CB_CK))
                    .setOAuthConsumerSecret((String) doc.get(FEILD_CB_CS))
                    .setOAuthAccessToken((String) doc.get(FEILD_CB_AT))
                    .setOAuthAccessTokenSecret((String) doc.get(FEILD_CB_ATS))
                    .setDebugEnabled((boolean) doc.get(FEILD_CB_DEBUG))
                    .setJSONStoreEnabled((boolean) doc.get(FEILD_CB_JSON));
            return cb;
        }
    }

    @SuppressWarnings("unused")
    private void putCb (String ck, String cs, String at, String ats) {
        final DBCollection coll = this.getCbColl();
        final BasicDBObject doc =
                new BasicDBObject(FEILD_CBANDCRAWL_ID, coll.count())
                        .append(FEILD_CB_CK, ck).append(FEILD_CB_CS, cs)
                        .append(FEILD_CB_AT, at).append(FEILD_CB_ATS, ats)
                        .append(FEILD_CB_DEBUG, false)
                        .append(FEILD_CB_JSON, true);
        coll.insert(doc);
    }

    /* Cb end ********************* */

    public long getRDCSleepTime () {
        final DBCollection coll = this.getParametersColl();
        DBObject doc =
                coll.findOne(new BasicDBObject(FEILD_PARA_MODEL, PARA_MODEL_RDC));
        final long time;
        final Object para = doc.get(FEILD_PARA_RDC_SLEEPTIME);
        if (para != null) {
            time = (long) para;
        } else {
            time = 0;
        }
        return time;
    }

    public List<Status> getOriginalTweetListInTimeRange (long userId,
            Date fromDate, Date toDate) {
        final List<Status> tweets = new ArrayList<Status>();
        // Get tweet in tweets DB.
        final DBCollection coll = getTweetsDbCollection(userId);
        // Older to Later.
        final DBCursor cursor = coll.find().sort(new BasicDBObject("_id", 1));
        boolean laterThanToDate = false;
        while (cursor.hasNext() && !laterThanToDate) {
            final DBObject doc = cursor.next();
            final Status t = docToTweet(doc);
            final Date date = t.getCreatedAt();
            if (date.after(fromDate) && date.before(toDate)) {
                if (!t.isRetweet()) { // Original tweet.
                    tweets.add(t);
                }
            } else if (date.after(toDate)) {
                laterThanToDate = true;
            }
        }

        return tweets;
    }

    public PosAndNeg getPosAndNeg (long fId, List<Status> auTweets) {
        final List<Status> pos = new ArrayList<Status>();
        final List<Status> neg = new ArrayList<Status>();

        assert !auTweets.isEmpty();
        final long auId = auTweets.get(0).getUser().getId();
        final Date lastDate =
                new Date(auTweets.get(auTweets.size() - 1).getCreatedAt()
                        .getTime()
                        + ExampleGetter.DAY_IN_MILLISECONDS);
        final HashMap<Long, Status> idToTweet = new HashMap<Long, Status>();
        for (Status t : auTweets) {
            idToTweet.put(t.getId(), t);
        }
        final DBCollection coll = getTweetsDbCollection(fId);
        // Older to Later.
        final DBCursor cursor = coll.find().sort(new BasicDBObject("_id", 1));
        boolean laterThanLastDate = false;
        while (cursor.hasNext() && !laterThanLastDate) {
            final DBObject doc = cursor.next();
            final Status t = docToTweet(doc);
            if (t.isRetweet()
                    && t.getRetweetedStatus().getUser().getId() == auId) {
                // Retweeted from key author.
                final long otId = t.getRetweetedStatus().getId();
                final Status ot = idToTweet.get(otId);
                if (ot != null) { // The original tweet is in the auTweets.
                    final long timeDiff =
                            t.getCreatedAt().getTime()
                                    - ot.getCreatedAt().getTime();
                    // Retweet should be later than original one.
                    assert timeDiff >= 0;
                    if (timeDiff < ExampleGetter.DAY_IN_MILLISECONDS) {
                        // It's positive example only when it was retweeted with
                        // in one day after published.
                        pos.add(ot);
                        // Remove this ot so all remaining ots are negative.
                        idToTweet.remove(otId);
                    }
                }
            }
            final Date date = t.getCreatedAt();
            if (date.after(lastDate)) {
                laterThanLastDate = true;
            }
        } // while (cursor.hasNext() && !laterThanLastDate) {

        // Add all remaining tweets as negative examples.
        for (Entry<Long, Status> entry : idToTweet.entrySet()) {
            neg.add(entry.getValue());
        }

        return new PosAndNeg(pos, neg);
    }

    private static final Status docToTweet (final DBObject doc) {
        doc.removeField("_id");
        final String json = doc.toString();
        Status t = null;
        try {
            t = TwitterObjectFactory.createStatus(json);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        assert t != null;
        return t;
    }
}
