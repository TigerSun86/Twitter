package test;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;

public class DataCollector {
    private static final ArrayList<Long> AUTHOR_IDS = new ArrayList<Long>();
    static {
        AUTHOR_IDS.add(497178013L);
        AUTHOR_IDS.add(2551981338L);
        AUTHOR_IDS.add(246774523L);
        AUTHOR_IDS.add(1642106527L);
        AUTHOR_IDS.add(2353075322L);
        AUTHOR_IDS.add(166496708L);
    }
    private static final long SINCE_ID = 522147892644294656L;
    private static final String PATH = "D:/Twitter/userdata/";

    private static int fileCount = 1;

    public static void main(String[] args) {
        final Twitter twitter = new TwitterFactory().getInstance();

        final HashSet<Long> userIds = new HashSet<Long>();
        userIds.addAll(AUTHOR_IDS);
        for (Long authorId : AUTHOR_IDS) {
            // Get all followers of the authors.
            final HashSet<Long> followers = UserData.getFollowers(twitter,
                    authorId);
            userIds.addAll(followers);
        }
        final HashMap<Long, String> idToFile = new HashMap<Long, String>();
        HashMap<Long, UserData> idToUser = new HashMap<Long, UserData>();
        long finishedCount = 0;
        for (Long userId : userIds) {
            final boolean isAuthor = AUTHOR_IDS.contains(userId);
            final UserData userData = UserData.newUserData(twitter, userId, SINCE_ID,isAuthor);
            finishedCount++;
            System.out.printf("Finished # of users %d/%d.%n", finishedCount,
                    userIds.size());

            if(userData!=null){
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

        final String fileName = "idToUser.ser";
        final String fullPath = PATH + fileName;
        storeTweets(idToUser, fullPath);
    }

    private static void storeTweets(Object o, final String fullPath) {
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
}
