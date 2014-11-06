package main;

import java.util.HashMap;

import test.UserData;
import twitter4j.Status;
import util.Cache;
import util.OReadWriter;

/**
 * FileName: ExampleExtractor.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Oct 30, 2014 5:04:46 PM
 */
public class ExampleExtractor {
    private static final long AUTHOR_ID = 2551981338L;

    @SuppressWarnings("unchecked")
    public static void main (String[] args) {
        final HashMap<Long, String> idToFile =
                (HashMap<Long, String>) OReadWriter.read(OReadWriter.PATH
                        + OReadWriter.ID2FILE_FILENAME);
        final Cache<HashMap<Long, UserData>> cache =
                new Cache<HashMap<Long, UserData>>();
        final UserData author = getUserDate(AUTHOR_ID, idToFile, cache);
        if (author == null) {
            System.out.println("No such author");
            return;
        }
        assert author.followersIds != null;
        for (Long folId : author.followersIds) {
            final UserData user = getUserDate(folId, idToFile, cache);
            if (user == null){
                continue; // No such user.
            }

            // Get positive examples.
            final HashMap<Long, Status> pos = new HashMap<Long, Status>();
            for (Status t: user.tweets){
                if (t.isRetweet()){
                    Status t2 = t;
                    while(t2.isRetweet()){ // find the original tweet of t.
                        t2 = t2.getRetweetedStatus();
                    }
                    if (t2.getUser().getId() == AUTHOR_ID){
                        // The author of t is AUTHOR_ID.
                        // Add positive example t2.
                        pos.put(t2.getId(),t2);
                    }
                }
            }
            
            // Get negative examples.
            final HashMap<Long, Status> neg = new HashMap<Long, Status>();
            int i = 0;
            int j = 0;
            Status tA = author.tweets.get(i);

            while(j < user.tweets.size()){
                final Status tF = user.tweets.get(j);
                if (tF.getCreatedAt().before(tA.getCreatedAt())){
                    j++;
                } else {
                    break;
                }
            }
        }

    }

    private static UserData
            getUserDate (Long id, HashMap<Long, String> idToFile,
                    Cache<HashMap<Long, UserData>> cache) {
        final String fileName = idToFile.get(id);
        if (fileName == null) {
            return null;  // No such id.
        }
        HashMap<Long, UserData> idToUser = cache.get(fileName);
        if (idToUser == null) { // Not in the cache.
            final String fullPath = OReadWriter.PATH + fileName;
            idToUser = (HashMap<Long, UserData>) OReadWriter.read(fullPath);
            if (idToUser == null) {
                return null; // No such file.
            } else {
                cache.put(fileName, idToUser); // Put into cache for future use.
            }
        }

        final UserData user = idToUser.get(id);
        assert user != null;
        return user;
    }

    @SuppressWarnings("unchecked")
    private static void writeIdToFile () {
        final HashMap<Long, String> idToFile = new HashMap<Long, String>();

        for (int count = 1; count <= 62; count++) {
            final String fileName =
                    OReadWriter.FILE_NAME + count + OReadWriter.EXT;
            final String fullPath = OReadWriter.PATH + fileName;
            final HashMap<Long, UserData> idToUser =
                    (HashMap<Long, UserData>) OReadWriter.read(fullPath);
            if (idToUser != null) {
                for (Long id : idToUser.keySet()) {
                    // Map user ids to the file storing them.
                    idToFile.put(id, fileName);
                }
            } else {
                System.out.println("Cannot read file" + fileName);
            }
        }
        // Save file indexes to file
        OReadWriter.write(idToFile, OReadWriter.PATH
                + OReadWriter.ID2FILE_FILENAME);
    }
}
