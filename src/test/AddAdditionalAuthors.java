package test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import util.MyMath;
import util.OReadWriter;

/**
 * FileName: AddAdditionalAuthors.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 5, 2015 3:01:21 PM
 */
public class AddAdditionalAuthors {
    private static final int LEAST_TIMES = 10;
    private static Date SINCEDATE = null;
    static {
        try {
            SINCEDATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Thu Dec 25 15:45:28 EST 2014");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void main (String[] args) {
        final PrintStream ps = System.out;
        try {
            final String curTime =
                    new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar
                            .getInstance().getTime());
            final String dbgFileName =
                    OReadWriter.PATH2 + "log_" + curTime + ".txt";
            final File file = new File(dbgFileName);
            if (!file.exists()) {
                // Create folders if not exist.
                new File(OReadWriter.PATH2).mkdirs();
                file.createNewFile();
            }
            System.out.println("Writing result into " + dbgFileName);
            System.setOut(new PrintStream(new FileOutputStream(file)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        final HashSet<Long> adset = getAdditionalAuthorsAndUpdatefriendsIds();
        downloadAdditionalAuthors(adset);

        System.out.close();
        System.setOut(ps);
        System.out.println("Writing finished");
    }

    private static void downloadAdditionalAuthors (HashSet<Long> adset) {
        System.out.println("****");
        System.out.println(Calendar.getInstance().getTime());
        System.out.println("Authors to crawl: " + adset.size());
        System.out.println("Crawl tweets since:" + SINCEDATE.toString());

        final Twitter twitter = new TwitterFactory().getInstance();
        long finishedCount = 0;
        for (Long userId : adset) {
            final boolean isAuthor = false; // Not the original 6 authors.
            final UserData userData =
                    UserData.newUserData(twitter, userId, SINCEDATE, isAuthor);
            finishedCount++;
            System.out.printf("Finished # of users %d/%d.%n", finishedCount,
                    adset.size());

            if (userData != null) {
                // Save updated data to file in new folder.
                // Won't keep the user anymore if it's been
                // deleted/privated/no such user.
                final String fileName =
                        userData.userProfile.getId() + OReadWriter.EXT;
                final String fullPath2 = OReadWriter.PATH2 + fileName;
                OReadWriter.write(userData, fullPath2);
            }
        }
        System.out.println(Calendar.getInstance().getTime());
        System.out.println("****");
    }

    private static HashSet<Long> getAdditionalAuthorsAndUpdatefriendsIds () {
        final HashSet<Long> adset = new HashSet<Long>();
        final HashSet<Long> existset = new HashSet<Long>();

        final List<UserData> oas = getOriginalAuthors();

        final File folder = new File(OReadWriter.PATH);
        final File[] fileList = folder.listFiles();
        for (File fileEntry : fileList) { // Initialize existing users.
            if (!MyMath.getExtentionOfFileName(fileEntry.getName()).equals(
                    OReadWriter.EXT2)) { // Ignore log file.
                continue;
            }
            existset.add((Long.parseLong(MyMath.getFileNameWithoutExt(fileEntry
                    .getName()))));
        }

        System.out.println("****");
        System.out.println(Calendar.getInstance().getTime());
        System.out.println("Existing users: " + existset.size());

        int count = 0;
        for (File fileEntry : fileList) { // Initialize existing users.
            if (!MyMath.getExtentionOfFileName(fileEntry.getName()).equals(
                    OReadWriter.EXT2)) { // Ignore log file.
                continue;
            }
            System.out.printf("Finished # of users %d/%d.%n", count,
                    existset.size());
            count++;

            // Find additional authors by checking each retweet.
            final HashMap<Long, Integer> auTimes = new HashMap<Long, Integer>();
            // Open each file.
            final String fullPath = fileEntry.getAbsolutePath();
            final UserData ud = (UserData) OReadWriter.read(fullPath);
            for (Status t : ud.tweets) {
                if (t.isRetweet()) {
                    final Status t2 = t.getRetweetedStatus();
                    final Long aid = new Long(t2.getUser().getId());
                    Integer times = auTimes.get(aid);
                    if (times == null) {
                        times = 1;
                    } else {
                        times = times + 1;
                    }
                    auTimes.put(aid, times);
                    if (times >= LEAST_TIMES && !existset.contains(aid)) {
                        adset.add(aid);
                    }
                }
            }

            // Add the friends (authors) of this follower.
            if (!ud.isAuthor) {
                if (ud.friendsIds == null) {
                    ud.friendsIds = new HashSet<Long>();
                }
                // Add 6 authors as friends of this follower (if they are).
                for (UserData oa : oas) {
                    if (oa.followersIds.contains(ud.userProfile.getId())) {
                        ud.friendsIds.add(oa.userProfile.getId());
                    }
                }

                // Add additional authors as friends.
                for (Entry<Long, Integer> entry : auTimes.entrySet()) {
                    final int times = entry.getValue();
                    if (times >= LEAST_TIMES) { // An author.
                        ud.friendsIds.add(entry.getKey());
                    }
                }
            }

            // Save updated data (and also the unchanged 6 authors) to file in
            // new folder.
            final String fileName = ud.userProfile.getId() + OReadWriter.EXT;
            final String fullPath2 = OReadWriter.PATH2 + fileName;
            OReadWriter.write(ud, fullPath2);

        }

        System.out.println("Ot set size: " + adset.size());
        System.out.println(Calendar.getInstance().getTime());
        System.out.println("****");
        return adset;
    }

    private static List<UserData> getOriginalAuthors () {
        final ArrayList<UserData> oas = new ArrayList<UserData>();
        for (Long id : DataCollector.AUTHOR_IDS) {
            final UserData ud = OReadWriter.getUserDate(id);
            oas.add(ud);
        }
        return oas;
    }
}
