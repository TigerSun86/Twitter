package datacollection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

/**
 * FileName: RestDataCollector.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Feb 3, 2015 8:09:20 PM
 */
public class RestDataCollector {
    private static Date SINCE_DATE = null;
    static {
        try {
            SINCE_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Tue Jan 27 22:34:47 EST 2015");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private Database db = null;
    private TwitterApi tapi = null;

    private boolean init () {
        db = Database.getInstance();
        if (db == null) {
            return false;
        }

        tapi = new TwitterApi();
        return true;
    }

    private void run () {
        if (!init()) {
            System.out.println("Data base problem.");
            return;
        }
        while (true) {
            for (Long userId : db.getAllUsers()) {
                if (!db.existStreamUser(userId)) {
                    crawlTweetsOfUser(userId);
                }
            }

            final long sleepTime = db.getRDCSleepTime();
            if (sleepTime > 0) {
                try {
                    System.out.printf("[%s] Sleep %d miliseconds.%n",new Date().toString(),
                            sleepTime);
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void crawlTweetsOfUser (Long userId) {
        final Status lastTweet = db.getLatestTweet(userId);
        final Date sinceDate; // Later one as sinceDate.
        if (lastTweet != null && lastTweet.getCreatedAt().after(SINCE_DATE)) {
            sinceDate = lastTweet.getCreatedAt();
        } else {
            sinceDate = SINCE_DATE;
        }
        final ArrayList<String> tweets = tapi.crawlTweets(userId, sinceDate);
        if (tweets != null) {
            for (int i = tweets.size() - 1; i >= 0; i--) {
                // The storing order in database is from oldest to latest.
                final String tweet = tweets.get(i);
                db.putTweet(userId, tweet);
            }
            printDbgInfo(userId, tweets);
        } else {
            System.out.printf("[%s] User: %d, crawling user failed.%n", new Date().toString(),userId);
        }
    }

    private void
            printDbgInfo (final long userId, final ArrayList<String> tweets) {
        String oldtime = "None";
        String newtime = "None";
        if (!tweets.isEmpty()) {
            try {
                Status t =
                        TwitterObjectFactory.createStatus(tweets.get(tweets
                                .size() - 1));
                oldtime = t.getCreatedAt().toString();
                t = TwitterObjectFactory.createStatus(tweets.get(0));
                newtime = t.getCreatedAt().toString();
            } catch (TwitterException e) {
                e.printStackTrace();
            }
        }

        System.out.printf(
                "[%s] User: %d, new tweets: %d, oldest: %s, latest: %s.%n", new Date().toString(),userId,
                tweets.size(), oldtime, newtime);
    }

    public static void main (String[] args) {
        new RestDataCollector().run();
    }
}
