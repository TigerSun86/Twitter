package datacollection;

import java.net.UnknownHostException;
import java.util.HashSet;

import twitter4j.DirectMessage;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterObjectFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

/**
 * FileName:     MyUserStream.java
 * @Description: 
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 20, 2015 3:11:13 PM 
 */
public class MyUserStream {
    public static final String COLL_NAME_PREFIX = "_";
    public static final HashSet<Long> KEY_AUTHORS;
    static {
        KEY_AUTHORS = new HashSet<Long>();
        KEY_AUTHORS.add(0L);
    }
    DB tweetsDb = null;
    DB userDb = null;
    DBCollection wtColl = null;
    
    private void run(){
        if(!connectDB()){
            return;
        }
        
        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(new MyUserStreamListener());
        // user() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
        twitterStream.user();
    }
    
    private boolean connectDB(){
        boolean suc = true;
        try {
            MongoClient mongoClient = new MongoClient();
            // get handle to "mydb"
            tweetsDb = mongoClient.getDB("Tweets");
            userDb = mongoClient.getDB("User");
            DB otherDb =  mongoClient.getDB("Other");
            wtColl = otherDb.getCollection("WaitingTweets");
        } catch (UnknownHostException e) {
            suc = false;
            e.printStackTrace();
        }
        return suc;
    }
    
    public static void main(String[] args) {
        new MyUserStream().run();
    }

    private class MyUserStreamListener implements UserStreamListener {
        @Override
        public void onStatus(Status status) {
            String str = TwitterObjectFactory.getRawJSON(status);   
            System.out.println("raw json str: "+str);
            DBObject dbObject =(DBObject)JSON.parse(str);
            long userId = status.getUser().getId();
            DBCollection coll = tweetsDb.getCollection(MyUserStream.COLL_NAME_PREFIX + userId);
            coll.insert(dbObject); 

            if(KEY_AUTHORS.contains(userId)){
                wtColl.insert(dbObject); // add to check list
            }
            
            DBCursor cursor = coll.find(new BasicDBObject("_id",false));
            try {
                while (cursor.hasNext()) {
                    String str1 = cursor.next().toString();
                    System.out.println(str1);
                }
            } finally {
                cursor.close();
            }
            System.out.println("onStatus @" + status.getUser().getScreenName() + " - " + status.getText());
        }

        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
        }

        @Override
        public void onDeletionNotice(long directMessageId, long userId) {
            System.out.println("Got a direct message deletion notice id:" + directMessageId);
        }

        @Override
        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            System.out.println("Got a track limitation notice:" + numberOfLimitedStatuses);
        }

        @Override
        public void onScrubGeo(long userId, long upToStatusId) {
            System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
        }

        @Override
        public void onStallWarning(StallWarning warning) {
            System.out.println("Got stall warning:" + warning);
        }

        @Override
        public void onFriendList(long[] friendIds) {
            System.out.print("onFriendList");
            for (long friendId : friendIds) {
                System.out.print(" " + friendId);
            }
            System.out.println();
        }

        @Override
        public void onFavorite(User source, User target, Status favoritedStatus) {
            System.out.println("onFavorite source:@"
                    + source.getScreenName() + " target:@"
                    + target.getScreenName() + " @"
                    + favoritedStatus.getUser().getScreenName() + " - "
                    + favoritedStatus.getText());
        }

        @Override
        public void onUnfavorite(User source, User target, Status unfavoritedStatus) {
            System.out.println("onUnFavorite source:@"
                    + source.getScreenName() + " target:@"
                    + target.getScreenName() + " @"
                    + unfavoritedStatus.getUser().getScreenName()
                    + " - " + unfavoritedStatus.getText());
        }

        @Override
        public void onFollow(User source, User followedUser) {
            System.out.println("onFollow source:@"
                    + source.getScreenName() + " target:@"
                    + followedUser.getScreenName());
        }

        @Override
        public void onUnfollow(User source, User followedUser) {
            System.out.println("onFollow source:@"
                    + source.getScreenName() + " target:@"
                    + followedUser.getScreenName());
        }

        @Override
        public void onDirectMessage(DirectMessage directMessage) {
            System.out.println("onDirectMessage text:"
                    + directMessage.getText());
        }

        @Override
        public void onUserListMemberAddition(User addedMember, User listOwner, UserList list) {
            System.out.println("onUserListMemberAddition added member:@"
                    + addedMember.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        @Override
        public void onUserListMemberDeletion(User deletedMember, User listOwner, UserList list) {
            System.out.println("onUserListMemberDeleted deleted member:@"
                    + deletedMember.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        @Override
        public void onUserListSubscription(User subscriber, User listOwner, UserList list) {
            System.out.println("onUserListSubscribed subscriber:@"
                    + subscriber.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        @Override
        public void onUserListUnsubscription(User subscriber, User listOwner, UserList list) {
            System.out.println("onUserListUnsubscribed subscriber:@"
                    + subscriber.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        @Override
        public void onUserListCreation(User listOwner, UserList list) {
            System.out.println("onUserListCreated  listOwner:@"
                    + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        @Override
        public void onUserListUpdate(User listOwner, UserList list) {
            System.out.println("onUserListUpdated  listOwner:@"
                    + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        @Override
        public void onUserListDeletion(User listOwner, UserList list) {
            System.out.println("onUserListDestroyed  listOwner:@"
                    + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        @Override
        public void onUserProfileUpdate(User updatedUser) {
            System.out.println("onUserProfileUpdated user:@" + updatedUser.getScreenName());
        }

        @Override
        public void onUserDeletion(long deletedUser) {
            System.out.println("onUserDeletion user:@" + deletedUser);
        }

        @Override
        public void onUserSuspension(long suspendedUser) {
            System.out.println("onUserSuspension user:@" + suspendedUser);
        }

        @Override
        public void onBlock(User source, User blockedUser) {
            System.out.println("onBlock source:@" + source.getScreenName()
                    + " target:@" + blockedUser.getScreenName());
        }

        @Override
        public void onUnblock(User source, User unblockedUser) {
            System.out.println("onUnblock source:@" + source.getScreenName()
                    + " target:@" + unblockedUser.getScreenName());
        }

        @Override
        public void onException(Exception ex) {
            ex.printStackTrace();
            System.out.println("onException:" + ex.getMessage());
        }
    };
}
