package datacollection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Trends;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.User;

/**
 * FileName: TwitterApi.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 23, 2015 7:05:32 PM
 */
public class TwitterApi {
    Twitter twitter = null;

    public TwitterApi() {
        twitter = new TwitterFactory().getInstance();
    }

    public TwitterApi(Twitter twitter) {
        this.twitter = twitter;
    }

    public List<Status> getRetweets (long statusId) {
        List<Status> statuses = null;
        boolean isRunning = true;
        while (isRunning) {
            try {
                statuses = twitter.getRetweets(statusId);
                isRunning = false; // Only get retweets once, if it succeeded.
            } catch (TwitterException te) {
                if (!handleException(te)) { // If got limitation then wait.
                    // User is private, don't try again, just return null.
                    isRunning = false;
                }
            }
        }
        return statuses;
    }

    public Status showStatus (long tweetId) {
        Status status = null;
        boolean isRunning = true;
        while (isRunning) {
            try {
                status = twitter.showStatus(tweetId);
                isRunning = false; // Only get retweets once, if it succeeded.
            } catch (TwitterException te) {
                if (!handleException(te)) { // If got limitation then wait.
                    // It's deleted, don't try again, just return null.
                    isRunning = false;
                }
            }
        }
        return status;

    }

    public User getUserProfile (long userId) {
        User userProfile = null;
        boolean isRunning = true;
        while (isRunning) {
            try {
                userProfile = twitter.showUser(userId);
                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)) { // If got limitation then wait.
                    // User is private, don't try again, just return null.
                    isRunning = false;
                }
            }
        }
        return userProfile;
    }

    public User getUserProfile (String userScreenName) {
        User userProfile = null;
        boolean isRunning = true;
        while (isRunning) {
            try {
                userProfile = twitter.showUser(userScreenName);
                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)) { // If got limitation then wait.
                    // User is private, don't try again, just return null.
                    isRunning = false;
                }
            }
        }
        return userProfile;
    }

    public boolean createFriendship (long userId) {
        boolean isRunning = true;
        boolean suc = false;
        while (isRunning) {
            try {
                twitter.createFriendship(userId);
                isRunning = false;
                suc = true;
            } catch (TwitterException te) {
                if (!handleException(te)) { // Will retry when network issue.
                    // User is private, don't try again, just return null.
                    isRunning = false;
                }
            }
        }
        return suc;
    }

    public Trends getTrends (int woeid) {
        Trends trends = null;
        boolean isRunning = true;
        while (isRunning) {
            try {
                trends = twitter.getPlaceTrends(woeid);
                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)) { // If got limitation then wait.
                    // Impossible to access here.
                    isRunning = false;
                }
            }
        }
        return trends;
    }

    /**
     * Order latest to oldest.
     * @return Json format string new tweets after sinceDate; null if the user
     *         has been deleted/privated/no such user.
     */
    public ArrayList<String> crawlTweets (long userId, Date sinceDate) {
        final ArrayList<String> tweets = new ArrayList<String>();
        final Paging paging = new Paging();
        paging.setCount(200);

        boolean getUserFailed = false;
        boolean isRunning = true;
        while (isRunning) {
            try {
                boolean needMoreTweets = true;
                while (needMoreTweets) {
                    final List<Status> statuses =
                            twitter.getUserTimeline(userId, paging);
                    for (Status t : statuses) {
                        if (sinceDate.before(t.getCreatedAt())) {
                            final String str =
                                    TwitterObjectFactory.getRawJSON(t);
                            tweets.add(str);
                        } else { // Tweet too old.
                            needMoreTweets = false;
                        }
                    }
                    if (statuses.size() < 200) { // No more tweets.
                        needMoreTweets = false;
                    } else if (needMoreTweets) {
                        // Next round search tweets older than last one.
                        final long maxId =
                                statuses.get(statuses.size() - 1).getId();
                        paging.setMaxId(maxId - 1);
                    }
                } // while (needMoreTweets) {

                isRunning = false;
            } catch (TwitterException te) {
                if (!handleException(te)) {
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
            System.out.println("Got limitation of Twitter, retry in " + seconds
                    + " seconds.");
            time = (seconds + 1) * 1000;
        } else if (te.isCausedByNetworkIssue() // Network issue.
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

}
