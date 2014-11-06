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
            System.out.println("Got # of tweets: " + ud.tweets.size());
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
        System.out.println("Is author: " + isAuthor);
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
                if (te.exceededRateLimitation()) { // Got limitation.
                    final int seconds =
                            te.getRateLimitStatus().getSecondsUntilReset();
                    System.out
                            .println("Got limitation in getUserProfile, retry in "
                                    + seconds + " seconds.");
                    if (seconds > 0) {
                        try {
                            Thread.sleep((seconds + 1) * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Failed in getUserProfile: "
                            + te.getMessage());
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
                if (te.exceededRateLimitation()) { // Got limitation.
                    final int seconds =
                            te.getRateLimitStatus().getSecondsUntilReset();
                    System.out
                            .println("Got limitation in getFollowers, retry in "
                                    + seconds + " seconds.");
                    if (seconds > 0) {
                        try {
                            Thread.sleep((seconds + 1) * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Failed in getFollowers: "
                            + te.getMessage());
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
                if (te.exceededRateLimitation()) { // Got limitation.
                    final int seconds =
                            te.getRateLimitStatus().getSecondsUntilReset();
                    System.out
                            .println("Got limitation in getFriends, retry in "
                                    + seconds + " seconds.");
                    if (seconds > 0) {
                        try {
                            Thread.sleep((seconds + 1) * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Failed in getFriends: "
                            + te.getMessage());
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
                if (te.exceededRateLimitation()) { // Got limitation.
                    final int seconds =
                            te.getRateLimitStatus().getSecondsUntilReset();
                    System.out.println("Got limitation in getTweets, retry in "
                            + seconds + " seconds.");
                    if (seconds > 0) {
                        try {
                            Thread.sleep((seconds + 1) * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Failed in getTweets: "
                            + te.getMessage());
                    isRunning = false;
                }
            }
        }
        return tweets;
    }
}
