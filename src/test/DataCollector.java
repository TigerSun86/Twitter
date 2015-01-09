package test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import util.MyMath;
import util.OReadWriter;
import util.OutputRedirection;

public class DataCollector {
    public static final ArrayList<Long> AUTHOR_IDS = new ArrayList<Long>();
    static {
        AUTHOR_IDS.add(497178013L);
        AUTHOR_IDS.add(2551981338L);
        AUTHOR_IDS.add(246774523L);
        AUTHOR_IDS.add(1642106527L);
        AUTHOR_IDS.add(2353075322L);
        AUTHOR_IDS.add(166496708L);
    }
    /* initializeData begin */
    private static final Date sinceDate;
    static {
        final Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final int day = c.get(Calendar.DATE);
        c.set(Calendar.DATE, day - 7);
        sinceDate = c.getTime();
    }

    private static void storeTweets (Object o, final String fullPath) {
        ObjectOutputStream out = null;
        try {
            System.out
                    .println("Serialized data is saving in " + fullPath + ".");
            FileOutputStream fileOut = new FileOutputStream(fullPath);
            BufferedOutputStream bOut = new BufferedOutputStream(fileOut);
            out = new ObjectOutputStream(bOut);
            out.writeObject(o);
            System.out.println("Saving finished.");
        } catch (IOException i) {
            i.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final String PATH = "D:/Twitter/userdata/";

    private static int fileCount = 1;

    @SuppressWarnings("unused")
    private static void initializeData () {
        System.out.println("Crawl tweets since:" + sinceDate.toString());
        final Twitter twitter = new TwitterFactory().getInstance();

        final HashSet<Long> userIds = new HashSet<Long>();
        userIds.addAll(AUTHOR_IDS);
        for (Long authorId : AUTHOR_IDS) {
            // Get all followers of the authors.
            final HashSet<Long> followers =
                    UserData.getFollowers(twitter, authorId);
            userIds.addAll(followers);
        }
        final HashMap<Long, String> idToFile = new HashMap<Long, String>();
        HashMap<Long, UserData> idToUser = new HashMap<Long, UserData>();
        long finishedCount = 0;
        for (Long userId : userIds) {
            final boolean isAuthor = AUTHOR_IDS.contains(userId);
            final UserData userData =
                    UserData.newUserData(twitter, userId, sinceDate, isAuthor);
            finishedCount++;
            System.out.printf("Finished # of users %d/%d.%n", finishedCount,
                    userIds.size());

            if (userData != null) {
                idToUser.put(userId, userData);
                if (idToUser.size() >= 100) {
                    // Save data to file.
                    final String fileName = "ud" + fileCount + ".ser";
                    final String fullPath = PATH + fileName;
                    storeTweets(idToUser, fullPath);
                    fileCount++;
                    for (Long id : idToUser.keySet()) {
                        // Map user ids to the file storing them.
                        idToFile.put(id, fileName);
                    }

                    idToUser = new HashMap<Long, UserData>();
                }
            }
        }

        // Save last data to file.
        String fileName = "ud" + fileCount + ".ser";
        String fullPath = PATH + fileName;
        storeTweets(idToUser, fullPath);
        fileCount++;
        for (Long id : idToUser.keySet()) {
            // Map user ids to the file storing them.
            idToFile.put(id, fileName);
        }
        // Save file indexes to file
        fileName = "idToFile.ser";
        fullPath = PATH + fileName;
        storeTweets(idToFile, fullPath);
    }

    /* initializeData end */

    /* Update data begin */
    private static final int MAX_FILE_COUNT = 63;
    private static Date UP_SINCE_DATE = null;
    static {
        try {
           // UP_SINCE_DATE =
            //        new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
            //                .parse("Mon Oct 27 22:22:22 EDT 2014");
            UP_SINCE_DATE =
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            .parse("Thu Dec 25 15:45:28 EST 2014");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static boolean exists (final File file, final File[] existingFiles) {
        for (File f : existingFiles) {
            if (f.getName().equals(file.getName())) {
                return true;
            }
        }
        return false;
    }

    private static void updateData () {
        System.out.println("Crawl tweets since:" + UP_SINCE_DATE.toString());
        final File folder = new File(OReadWriter.PATH);
        final File[] fileList = folder.listFiles();
        // 1 log file.
        System.out.println("Old # of users: " +( fileList.length - 1));

        final Twitter twitter = new TwitterFactory().getInstance();

        final File[] existingFiles = new File(OReadWriter.PATH2).listFiles();

        int sum = 0;
        Status latest = null;
        Status oldest = null;
        for (final File fileEntry : fileList) {
            if (exists(fileEntry, existingFiles)) {
                continue;
            }
            
            if (!MyMath.getExtentionOfFileName(fileEntry.getName()).equals(
                    OReadWriter.EXT2)) { // Ignore log file.
                continue;
            }
            
            // Open each file.
            final String fullPath = fileEntry.getAbsolutePath();
            final UserData ud = (UserData) OReadWriter.read(fullPath);

            final ArrayList<Status> newTweets =
                    ud.update(twitter, UP_SINCE_DATE);

            if (newTweets != null) {
                // Save updated data to file in new folder.
                // Won't keep the user anymore if it's been
                // deleted/privated/no such user.
                final String fileName =
                        ud.userProfile.getId() + OReadWriter.EXT;
                final String fullPath2 = OReadWriter.PATH2 + fileName;
                OReadWriter.write(ud, fullPath2);

                if (!newTweets.isEmpty()) {
                    // Statistic info.
                    sum += newTweets.size();
                    final Status lat = newTweets.get(0);
                    final Status old = newTweets.get(newTweets.size() - 1);
                    if (latest == null
                            || latest.getCreatedAt().before(lat.getCreatedAt())) {
                        latest = lat;
                    }
                    if (oldest == null
                            || oldest.getCreatedAt().after(old.getCreatedAt())) {
                        oldest = old;
                    }
                } // if(!newTweets.isEmpty()) {
            } // if (newTweets != null){
        } // for (final File fileEntry : fileList) {

        System.out.println("In total appended # of tweets: " + sum);
        if (latest != null) {
            System.out.println("Latest tweet id: " + latest.getId()
                    + ", date: " + latest.getCreatedAt().toString()
                    + ", userId: " + latest.getUser().getId() + ", userName:"
                    + latest.getUser().getScreenName());
        }
        if (oldest != null) {
            System.out.println("Oldest tweet id: " + oldest.getId()
                    + ", date: " + oldest.getCreatedAt().toString()
                    + ", userId: " + oldest.getUser().getId() + ", userName:"
                    + oldest.getUser().getScreenName());
        }
        // 1 log file
        System.out.println("Current total # of users: "
                + ((new File(OReadWriter.PATH2)).listFiles().length - 1));
    }

    private static void updateData2 () {
        System.out.println("Crawl tweets since:" + UP_SINCE_DATE.toString());

        @SuppressWarnings("unchecked")
        final HashMap<Long, String> idToFile =
                (HashMap<Long, String>) OReadWriter.read(OReadWriter.PATH
                        + OReadWriter.ID2FILE_FILENAME);
        System.out.println("Old # of users: " + idToFile.size());

        final Twitter twitter = new TwitterFactory().getInstance();
        int sum = 0;
        Status latest = null;
        Status oldest = null;
        for (int count = 1; count <= MAX_FILE_COUNT; count++) {
            // Open each file.
            final String fileName =
                    OReadWriter.FILE_NAME + count + OReadWriter.EXT;
            final String fullPath = OReadWriter.PATH + fileName;
            @SuppressWarnings("unchecked")
            final HashMap<Long, UserData> idToUser =
                    (HashMap<Long, UserData>) OReadWriter.read(fullPath);
            final Iterator<Entry<Long, UserData>> iter =
                    idToUser.entrySet().iterator();
            while (iter.hasNext()) {
                final UserData ud = iter.next().getValue();
                final ArrayList<Status> newTweets =
                        ud.update(twitter, UP_SINCE_DATE);
                if (newTweets == null) {
                    // The user has been deleted/privated/no such user.
                    // Should remove the user from the data base.
                    idToFile.remove(ud.userProfile.getId());
                    iter.remove();
                } else if (!newTweets.isEmpty()) {
                    // Statistic info.
                    sum += newTweets.size();
                    final Status lat = newTweets.get(0);
                    final Status old = newTweets.get(newTweets.size() - 1);
                    if (latest == null
                            || latest.getCreatedAt().before(lat.getCreatedAt())) {
                        latest = lat;
                    }
                    if (oldest == null
                            || oldest.getCreatedAt().after(old.getCreatedAt())) {
                        oldest = old;
                    }
                } // } else if (!newTweets.isEmpty()) {
            } // while (iter.hasNext()) {

            // Save updated data to file in new folder.
            final String fullPath2 = OReadWriter.PATH2 + fileName;
            OReadWriter.write(idToUser, fullPath2);
        } // for (int count = 1; count <= MAX_FILE_COUNT; count++) {

        System.out.println("In total appended # of tweets: " + sum);
        if (latest != null) {
            System.out.println("Latest tweet id: " + latest.getId()
                    + ", date: " + latest.getCreatedAt().toString()
                    + ", userId: " + latest.getUser().getId() + ", userName:"
                    + latest.getUser().getScreenName());
        }
        if (oldest != null) {
            System.out.println("Oldest tweet id: " + oldest.getId()
                    + ", date: " + oldest.getCreatedAt().toString()
                    + ", userId: " + oldest.getUser().getId() + ", userName:"
                    + oldest.getUser().getScreenName());
        }
        System.out.println("Current total # of users: " + idToFile.size());
        // Save file indexes to file. Some user could be deleted.
        OReadWriter.write(idToFile, OReadWriter.PATH2
                + OReadWriter.ID2FILE_FILENAME);
    }

    /* Update data end */

    public static void main (String[] args) {
        final String curTime =
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Calendar
                        .getInstance().getTime());
        final String dbgFileName =
                OReadWriter.PATH2 + "log_" + curTime + ".txt";
        final OutputRedirection or = new OutputRedirection(dbgFileName);
        updateData();
        or.close();
    }
}
