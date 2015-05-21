package datacollection;

import java.util.HashMap;
import java.util.Map.Entry;

import common.DataReader;

/**
 * FileName: UserRemover.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 17, 2015 6:09:57 PM
 */
public class UserRemover {
    Database db = Database.getInstance();

    public static void main (String[] args) {
        new UserRemover().remove();
    }

    private static final long THRESHOLD = 2;

    private void remove () {
        long count = 0;
        long tw = 0;
        HashMap<Long, Long> folToCount = countFolsPos();
        for (Entry<Long, Long> entry : folToCount.entrySet()) {
            long pos = entry.getValue();
            if (pos <= THRESHOLD) {
                long id = entry.getKey();
                if (!isKeyUser(id)) {
                    // remove
                    long tc = db.getTweetsCount(id);
                    count++;
                    tw += tc;

                    System.out.printf(
                            "Removing user: %d, #pos: %d. #tweets: %d%n", id,
                            pos, tc);
                    db.removeUser(id);
                }
            }
        }
        System.out.printf("Totally removed user: %d, tweets: %d%n", count, tw);
    }

    private boolean isKeyUser (long id) {
        // Don't remove key author and the first 1000 followers.
        return UserInfo.KEY_AUTHORS.contains(id) || db.existStreamUser(id);
    }

    private HashMap<Long, Long> countFolsPos () {
        String fileName =
                "file://localhost/C:/WorkSpace/Twitter/data/validUsers.txt";
        final DataReader in = new DataReader(fileName);
        HashMap<Long, Long> map = new HashMap<Long, Long>();
        while (true) {
            final String str = in.nextLine();
            if (str == null) {
                break;
            }
            if (!str.isEmpty() && Character.isDigit(str.charAt(0))) {
                String[] s = str.split(" ");
                long fol = Long.parseLong(s[1]);
                long pos = Long.parseLong(s[2]);
                if (map.containsKey(fol)) {
                    long oldPos = map.get(fol);
                    if (pos > oldPos) {
                        // Record the max number of pos, if one fol appear
                        // twice.
                        map.put(fol, pos);
                    }
                } else {
                    map.put(fol, pos);
                }
            }
        } // End of while (true) {
        in.close();
        return map;
    }

}
