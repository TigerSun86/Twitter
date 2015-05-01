package datacollection;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import twitter4j.User;

/**
 * FileName: UserInfo.java
 * @Description: User info for Mongodb and streaming api.
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 26, 2015 8:10:07 PM
 */
public class UserInfo {
    public static final HashSet<Long> KEY_AUTHORS;
    static {
        KEY_AUTHORS = new HashSet<Long>();
        // screen name: Greenpeace, name: Greenpeace
        KEY_AUTHORS.add(3459051L);
        // screen name: UNFCCC, name: UNFCCC
        KEY_AUTHORS.add(17463923L);
        // screen name: climateprogress, name: Climate Progress
        KEY_AUTHORS.add(28657802L);
        // screen name: ClimateReality, name: Climate Reality
        KEY_AUTHORS.add(16958346L);
        // screen name: ClimateDesk, name: Climate Desk
        KEY_AUTHORS.add(120181944L);
        // screen name: EarthVitalSigns, name: NASA Climate
        KEY_AUTHORS.add(15461733L);
        // screen name: UNEP, name: UN Environment
        KEY_AUTHORS.add(38146999L);

    }
    public static final HashMap<Long, String> KA_ID2SCREENNAME;
    static {
        KA_ID2SCREENNAME = new HashMap<Long, String>();
        // screen name: Greenpeace, name: Greenpeace
        KA_ID2SCREENNAME.put(3459051L, "Greenpeace");
        // screen name: UNFCCC, name: UNFCCC
        KA_ID2SCREENNAME.put(17463923L, "UNFCCC");
        // screen name: climateprogress, name: Climate Progress
        KA_ID2SCREENNAME.put(28657802L, "climateprogress");
        // screen name: ClimateReality, name: Climate Reality
        KA_ID2SCREENNAME.put(16958346L, "ClimateReality");
        // screen name: ClimateDesk, name: Climate Desk
        KA_ID2SCREENNAME.put(120181944L, "ClimateDesk");
        // screen name: EarthVitalSigns, name: NASA Climate
        KA_ID2SCREENNAME.put(15461733L, "EarthVitalSigns");
        // screen name: UNEP, name: UN Environment
        KA_ID2SCREENNAME.put(38146999L, "UNEP");
    }

    public final long userId; // For database search.
    public final User userProfile;
    public final Date crawledAt;
    public final HashSet<Long> friendsIds;
    // Put fols last because fols could be chanced frequently (for key author),
    // but friends won't.
    public final HashSet<Long> followersIds;

    public UserInfo(long userId, User userProfile, Date crawledAt) {
        super();
        this.userId = userId;
        this.userProfile = userProfile;
        this.crawledAt = crawledAt;
        this.friendsIds = new HashSet<Long>();
        this.followersIds = new HashSet<Long>();
    }

    @Override
    public String toString () {
        final StringBuilder sb = new StringBuilder();
        final User u = this.userProfile;
        sb.append(String
                .format("Id: %d, screen name: %s, name: %s, crawledAt: %s, description: %s%n",
                        userId, u.getScreenName(), u.getName(),
                        crawledAt.toString(), u.getDescription()));
        sb.append("Friends: ");
        for (long id : friendsIds) {
            sb.append(id + " ");
        }
        sb.append(String.format("%n"));
        sb.append("Followers: ");
        for (long id : followersIds) {
            sb.append(id + " ");
        }
        sb.append(String.format("%n"));
        return sb.toString();
    }
}
