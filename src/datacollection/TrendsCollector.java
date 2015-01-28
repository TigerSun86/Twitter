package datacollection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import twitter4j.Trends;
import util.OutputRedirection;

/**
 * FileName: TrendsCollector.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 27, 2015 10:48:38 PM
 */
public class TrendsCollector {
    private static final int WOEID = 1;// Get global trends.
    // Crawl data in each 2 hours.
    private static final long TIME_RANGE = TimeUnit.MILLISECONDS.convert(2,
            TimeUnit.HOURS);

    public static void main (String[] args) {
        final String curTime =
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        final String file = "D:/TwitterDB/stream/trendsLog_" + curTime + ".txt";
        new OutputRedirection(file);

        final Database db = Database.getInstance();
        if (db == null) {
            return;
        }

        final TwitterApi tapi = new TwitterApi();
        while (true) {
            final Trends trends = tapi.getTrends(WOEID);
            db.storeTrends(trends);
            System.out.println(new Date().toString());
            System.out.println(trends.toString());
            try { // Sleep 2 hours.
                Thread.sleep(TIME_RANGE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
