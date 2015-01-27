package datacollection;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

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

/**
 * FileName: UserInfo.java
 * @Description: User info for Mongodb and streaming api.
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 26, 2015 8:10:07 PM
 */
public class UserInfo {
    private static final String FEILD_ID = "UserId";
    private static final String FEILD_UP = "UserProfile";
    private static final String FEILD_TIME = "CrawledAtTime";
    private static final String FEILD_FRI = "FriendsIds";
    private static final String FEILD_FOL = "FollowersIds";

    public static final HashSet<Long> KEY_AUTHORS;
    static {
        KEY_AUTHORS = new HashSet<Long>();
        KEY_AUTHORS.add(0L);
    }

    public final long userId; // For database search.
    public final User userProfile;
    public final Date crawledAt;
    public final HashSet<Long> friendsIds;
    // Put fols last because fols could be chanced frequently (for key author),
    // but friends won't.
    public final HashSet<Long> followersIds;

    public UserInfo(long userId, User userProfile, Date crawledAt) {
        super();
        this.userId = userId;
        this.userProfile = userProfile;
        this.crawledAt = crawledAt;
        this.friendsIds = new HashSet<Long>();
        this.followersIds = new HashSet<Long>();
    }

    public static UserInfo getUser (long userId, DBCollection coll) {
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

    public static void storeUser (UserInfo user, DBCollection coll) {
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

    public static boolean addFriendOrFollower (long userId, long idToAdd,
            boolean addFriend, DBCollection coll) {
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

    public static void main (String[] args) {
        DBCollection coll = connectDB();
        DBCursor cursor = coll.find();

        TwitterApi tapi = new TwitterApi();
        // UserInfo user = new UserInfo();
        long id = 111L;
        addFriendOrFollower(id, 9999L, false, coll);
        addFriendOrFollower(id, 9999L, false, coll);
        addFriendOrFollower(9999L, 444L, false, coll);
        try {
            while (cursor.hasNext()) {
                System.out.println(cursor.next());
            }
        } finally {
            cursor.close();
        }
        /* user.userId = id;
         * user.userProfile = tapi.getUserProfile(497178013L);
         * user.crawledAt = new Date();
         * user.friendsIds = new HashSet<Long>();
         * user.friendsIds.add(111L);
         * user.friendsIds.add(222L);
         * user.followersIds = new HashSet<Long>();
         * storeUser(user, coll);
         * UserInfo u2 = getUser(id, coll);
         * System.out.println(u2.userId);
         * System.out.println(u2.userProfile);
         * System.out.println(u2.crawledAt);
         * System.out.println(u2.friendsIds);
         * System.out.println(u2.followersIds); */
    }

    private static DBCollection connectDB () {
        boolean suc = true;
        try {
            MongoClient mongoClient = new MongoClient();
            DB otherDb = mongoClient.getDB("Other");
            DBCollection coll = otherDb.getCollection("Users");
            return coll;
        } catch (UnknownHostException e) {
            suc = false;
            e.printStackTrace();
        }
        return null;
    }
}
