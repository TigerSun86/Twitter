package test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class UserData implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private static final String HIGH_NUM_STR = " high";

    public User userProfile = null;
    public HashSet<Long> followersIds = null;
    public HashSet<Long> friendsIds = null;
    public ArrayList<Status> tweets = null;
    public boolean isAuthor;

    public static UserData newUserData (final Twitter twitter,
            final long userId, final Date sinceDate, final boolean isAuthor) {
        final UserData ud = new UserData();
        ud.isAuthor = isAuthor;

        Thread tUser = new Thread() {
            public void run () {
                ud.userProfile = getUserProfile(twitter, userId);
            }
        };
        Thread tTweets = new Thread() {
            public void run () {
                ud.tweets = getTweets(twitter, userId, sinceDate);
            }
        };
        Thread tFollowers = new Thread() {
            public void run () {
                ud.followersIds = getFollowers(twitter, userId);
            }
        };
        Thread tFriends = new Thread() {
            public void run () {
                ud.friendsIds = getFriends(twitter, userId);
            }
        };

        tUser.start();
        tTweets.start();
        if (isAuthor) {
            tFollowers.start();
            tFriends.start();
        }
        try {
            tUser.join();
            tTweets.join();
            if (isAuthor) {
                tFollowers.join();
                tFriends.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("****");
        if (ud.userProfile != null) {
            System.out
                    .printf("Finished user %s, id %d, # of follower %d, # of friends %d, # of tweets %d.%n",
                            ud.userProfile.getScreenName(),
                            ud.userProfile.getId(),
                            ud.userProfile.getFollowersCount(),
                            ud.userProfile.getFriendsCount(),
                            ud.userProfile.getStatusesCount());
        }

        if (ud.tweets != null) {
            final String highNum = ud.tweets.size() >= 3000 ? HIGH_NUM_STR : "";
            System.out
                    .println("Got # of tweets: " + ud.tweets.size() + highNum);
            if (!ud.tweets.isEmpty()) {
                System.out.println("Latest tweet: "
                        + ud.tweets.get(0).getCreatedAt());
                System.out.println("Oldest tweet: "
                        + ud.tweets.get(ud.tweets.size() - 1).getCreatedAt());
            }
        }

        if (isAuthor) {
            System.out.println("Got # of followers: " + ud.followersIds.size());
            System.out.println("Got # of friends: " + ud.friendsIds.size());
        }
        final String userType;
        if (isAuthor) {
            userType = "Author";
        } else if (ud.friendsIds != null) {
            userType = "Follower";
        } else {
            userType = "OtherAuthor";
        }
        System.out.println("User type: " + userType);
        System.out.println("****");
        if (ud.userProfile != null && ud.tweets != null && !ud.tweets.isEmpty()) {
            return ud;
        } else {
            return null;
        }
    }

    public static User getUserProfile (Twitter twitter, long userId) {
        User userProfile = null;
        boolean isRunning = true;
        while (isRunning) {
            try {
                final User user = twitter.showUser(userId);
                userProfile = user;
                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)){
                    isRunning = false;
                }
            }
        }
        return userProfile;
    }

    public static HashSet<Long> getFollowers (Twitter twitter, long userId) {
        final HashSet<Long> followersIds = new HashSet<Long>();
        boolean isRunning = true;
        while (isRunning) {
            try {
                long cursor = -1;
                IDs ids;
                do {
                    ids = twitter.getFollowersIDs(userId, cursor, 5000);
                    for (long id : ids.getIDs()) {
                        followersIds.add(id);
                    }
                } while ((cursor = ids.getNextCursor()) != 0);
                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)){
                    isRunning = false;
                }
            }
        }
        return followersIds;
    }

    public static HashSet<Long> getFriends (Twitter twitter, long userId) {
        HashSet<Long> friendsIds = new HashSet<Long>();
        boolean isRunning = true;
        while (isRunning) {
            try {
                long cursor = -1;
                IDs ids;
                do {
                    ids = twitter.getFriendsIDs(userId, cursor, 5000);
                    for (long id : ids.getIDs()) {
                        friendsIds.add(id);
                    }
                } while ((cursor = ids.getNextCursor()) != 0);
                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)){
                    isRunning = false;
                }
            }
        }
        return friendsIds;
    }

    public static ArrayList<Status> getTweets (Twitter twitter, long userId,
            Date sinceDate) {
        ArrayList<Status> tweets = new ArrayList<Status>();
        final Paging paging = new Paging();
        paging.setCount(200);

        boolean isRunning = true;
        while (isRunning) {
            try {
                boolean needMoreTweets = true;
                while (needMoreTweets) {
                    final List<Status> statuses =
                            twitter.getUserTimeline(userId, paging);
                    for (Status t : statuses) {
                        if (sinceDate.before(t.getCreatedAt())) {
                            tweets.add(t);
                            final long maxId =
                                    statuses.get(statuses.size() - 1).getId();
                            // Search tweets older than last one.
                            paging.setMaxId(maxId - 1);
                        } else { // Tweet too old.
                            needMoreTweets = false;
                        }
                    }

                    if (statuses.size() < 200) { // No more tweets.
                        needMoreTweets = false;
                    }
                } // while (needMoreTweets) {

                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)){
                    isRunning = false;
                }
            }
        }
        return tweets;
    }

    /* Update data begin */

    private static class Buffer<T> {
        T b = null;
    }

    /**
     * @return New tweets after oldTweets; null if the user has been
     *         deleted/privated/no such user.
     */
    public ArrayList<Status>
            update (final Twitter twitter, final Date sinceDate) {
        final UserData ud = this;
        // User buffer to pass out the data from thread.
        final Buffer<ArrayList<Status>> buffer =
                new Buffer<ArrayList<Status>>();
        final Thread tTweets = new Thread() {
            public void run () {
                buffer.b =
                        getNewTweets(twitter, ud.userProfile.getId(),
                                sinceDate, ud.tweets);

            }
        };

        tTweets.start();

        try {
            tTweets.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final ArrayList<Status> newTweets = buffer.b;

        System.out.println("****");
        if (ud.userProfile != null) {
            System.out
                    .printf("Finished user %s, id %d, # of follower %d, # of friends %d, # of tweets %d.%n",
                            ud.userProfile.getScreenName(),
                            ud.userProfile.getId(),
                            ud.userProfile.getFollowersCount(),
                            ud.userProfile.getFriendsCount(),
                            ud.userProfile.getStatusesCount());
        }

        if (ud.tweets != null) {
            System.out.println("Old # of tweets: " + ud.tweets.size());
            if (!ud.tweets.isEmpty()) {
                System.out.println("Latest tweet: "
                        + ud.tweets.get(0).getCreatedAt());
                System.out.println("Oldest tweet: "
                        + ud.tweets.get(ud.tweets.size() - 1).getCreatedAt());
            }

        }

        if (newTweets != null) {
            final String highNum = ud.tweets.size() >= 3000 ? HIGH_NUM_STR : "";
            System.out.println("Got # of new tweets: " + newTweets.size()
                    + highNum);
            if (!newTweets.isEmpty()) {
                System.out.println("Latest tweet: "
                        + newTweets.get(0).getCreatedAt());
                System.out.println("Oldest tweet: "
                        + newTweets.get(newTweets.size() - 1).getCreatedAt());
            }
        } else {
            System.out
                    .println("The user has been deleted/privated/no such user");
        }
        final String userType;
        if (isAuthor) {
            userType = "Author";
        } else if (ud.friendsIds != null) {
            userType = "Follower";
        } else {
            userType = "OtherAuthor";
        }
        System.out.println("User type: " + userType);
        System.out.println("****");

        if (newTweets == null) {
            // The user has been deleted/privated/no such user.
            return null;
        } else {
            // Tweets are stored from latest to oldest, so new tweets should
            // be inserted to the beginning, before old ones.
            ud.tweets.addAll(0, newTweets);
            return newTweets;
        }
    }

    /**
     * @return new tweets after oldTweets; null if the user has been
     *         deleted/privated/no such user.
     */
    private static ArrayList<Status> getNewTweets (Twitter twitter,
            long userId, Date sinceDate, ArrayList<Status> oldTweets) {
        ArrayList<Status> tweets = new ArrayList<Status>();
        final Paging paging = new Paging();
        paging.setCount(200);

        final Status old;
        if (oldTweets == null || oldTweets.isEmpty()) {
            // oldTweets == null for collecting data for new user;
            // oldTweets is empty for some user didn't have tweet before.
            // (Should not happen)
            old = null;
        } else {
            old = oldTweets.get(0);
        }

        boolean getUserFailed = false;
        boolean isRunning = true;
        while (isRunning) {
            try {
                boolean needMoreTweets = true;
                while (needMoreTweets) {
                    final List<Status> statuses =
                            twitter.getUserTimeline(userId, paging);
                    for (Status t : statuses) {
                        if (isNewTweet(t, old, sinceDate)) {
                            tweets.add(t);
                            final long maxId =
                                    statuses.get(statuses.size() - 1).getId();
                            // Search tweets older than last one.
                            paging.setMaxId(maxId - 1);
                        } else { // Tweet too old.
                            needMoreTweets = false;
                        }
                    }

                    if (statuses.size() < 200) { // No more tweets.
                        needMoreTweets = false;
                    }
                } // while (needMoreTweets) {

                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)){
                    isRunning = false;
                    getUserFailed = true;
                }
            }
        }
        if (getUserFailed) {
            return null;
        } else {
            return tweets;
        }
    }

    // Retry in 5 minuses.
    private static final int RETRY_TIME = 5 * 60 * 1000;

    /** @return true, ok for run; false, stop. */
    private static boolean handleException (TwitterException te) {
        final int time;
        if (te.exceededRateLimitation()) { // Got limitation.
            final int seconds = te.getRateLimitStatus().getSecondsUntilReset();
            System.out.println("Got limitation of Twitter, retry in "
                    + seconds + " seconds.");
            time = (seconds + 1) * 1000;
        } else if (te.isCausedByNetworkIssue()
                || (te.isErrorMessageAvailable() && (te.getErrorCode() >= 500))) {
            System.out.println("Got network or server problem, retry in "
                    + (RETRY_TIME / 1000) + " seconds.");
            time = RETRY_TIME;
        } else {// The user has been deleted/privated/no such user.
            System.out.println(te.getMessage());
            time = 0;
        }

        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean isNewTweet (Status n, Status old, Date sinceDate) {
        if (old == null) { // No previous tweet or new user.
            return sinceDate.before(n.getCreatedAt());
        } else {
            // Has previous tweets, new tweet should be after the latest one
            // among old ones. old.getCreatedAt() should assert to be after
            // sinceDate.
            return (old.getId() != n.getId())
                    && (old.getCreatedAt().before(n.getCreatedAt()));
        }
    }
    /* Update data end */
}
