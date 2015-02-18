package main;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import test.UserData;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

/**
 * FileName: FeatureExtractorBackup.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Dec 18, 2014 4:04:46 PM
 */
public class FeatureExtractorBackup {

    private static final String F0 = "0";
    private static final String F1 = "1";
    private static final int MOUNT_IN_HOUR = 30 * 24;

    private static final List<FeatureGetter> GETTER_LIST =
            new ArrayList<FeatureGetter>();
    static {
        GETTER_LIST.add(new F1());
        GETTER_LIST.add(new F2());
        GETTER_LIST.add(new F3());
        GETTER_LIST.add(new F4());
        GETTER_LIST.add(new F5());
        GETTER_LIST.add(new F6());
        GETTER_LIST.add(new F7());
        GETTER_LIST.add(new F8());
        GETTER_LIST.add(new F9());
    }

    public static ArrayList<String> getFeatures (Status t, UserData ud) {
        final ArrayList<String> fs = new ArrayList<String>();
        for (FeatureGetter f : GETTER_LIST) {
            fs.add(f.getFeature(t, ud));
        }
        return fs;
    }

    private static interface FeatureGetter {
        public String getFeature (Status t, UserData ud);
    }

    /**
     * How many hours before t since last time f retweet something,
     * at most 1 month.
     */
    private static class F1 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final Date endTime = t.getCreatedAt();
            final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);
            for (Status ut : ud.tweets) {
                if (ut.isRetweet()) {
                    final Date utTime = ut.getCreatedAt();
                    if (beginTime.before(utTime) && endTime.after(utTime)) {
                        final long diffInHours =
                                TimeUnit.MILLISECONDS.toHours(endTime.getTime()
                                        - utTime.getTime());
                        feature = Long.toString(diffInHours);
                        break;
                    } else if (beginTime.after(utTime)) {
                        break;
                    }
                }
            }
            return feature;
        }
    }

    /**
     * How many hours before t since last time f published something
     * containing the hashtag in t, at most 1 month.
     */
    private static class F2 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final HashtagEntity[] hashtags = t.getHashtagEntities();
            if (hashtags != null && hashtags.length != 0) {
                final Date endTime = t.getCreatedAt();
                final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);

                for (Status ut : ud.tweets) {
                    final Date utTime = ut.getCreatedAt();
                    if (beginTime.before(utTime) && endTime.after(utTime)) {
                        final HashtagEntity[] utHt = ut.getHashtagEntities();
                        if (utHt != null && utHt.length != 0
                                && hasSameHashtag(hashtags, utHt)) {
                            final long diffInHours =
                                    TimeUnit.MILLISECONDS.toHours(endTime
                                            .getTime() - utTime.getTime());
                            feature = Long.toString(diffInHours);
                            break;
                        }
                    } else if (beginTime.after(utTime)) {
                        break;
                    }
                }
            }

            return feature;
        }

        private static boolean hasSameHashtag (final HashtagEntity[] h1,
                final HashtagEntity[] h2) {
            for (HashtagEntity i : h1) {
                for (HashtagEntity j : h2) {
                    if (i.getText().equals(j.getText())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * Whether f is mentioned in the tweet.
     */
    private static class F3 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            String feature = F0;
            final UserMentionEntity[] mentions = t.getUserMentionEntities();
            if (mentions != null && mentions.length != 0) {
                for (UserMentionEntity u : mentions) {
                    if (u.getId() == ud.userProfile.getId()) {
                        feature = F1;
                        break;
                    }
                }
            }
            return feature;
        }
    }

    /**
     * Whether t has picture.
     */
    private static class F4 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            // 4. Whether t has picture.
            String feature = F0;
            final MediaEntity[] medias = t.getMediaEntities();
            if (medias != null && medias.length != 0) {
                feature = F1;
            }
            return feature;
        }
    }

    /**
     * Whether t has url.
     */
    private static class F5 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            // 5. Whether t has url.
            String feature = F0;
            final URLEntity[] urls = t.getURLEntities();
            if (urls != null && urls.length != 0) {
                feature = F1;
            }
            return feature;
        }
    }

    /**
     * Hour in the week (the time t published). (0-168)
     */
    private static class F6 implements FeatureGetter {
        private static final HashMap<Integer, Integer> DAY_MAP =
                new HashMap<Integer, Integer>();
        static {
            DAY_MAP.put(Calendar.MONDAY, 0);
            DAY_MAP.put(Calendar.TUESDAY, 1);
            DAY_MAP.put(Calendar.WEDNESDAY, 2);
            DAY_MAP.put(Calendar.THURSDAY, 3);
            DAY_MAP.put(Calendar.FRIDAY, 4);
            DAY_MAP.put(Calendar.SATURDAY, 5);
            DAY_MAP.put(Calendar.SUNDAY, 6);
        }

        @Override
        public String getFeature (Status t, UserData ud) {
            final Date date = t.getCreatedAt();
            final Calendar c = Calendar.getInstance();
            c.setTime(date);
            final int dayOfWeek = DAY_MAP.get(c.get(Calendar.DAY_OF_WEEK));
            final int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
            final int hour = dayOfWeek * 24 + hourOfDay;
            final String feature = Integer.toString(hour);
            return feature;
        }
    }

    /**
     * How many hours before t since last time f published an original tweet, at
     * most 1 month (0-720).
     */
    private static class F7 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final Date endTime = t.getCreatedAt();
            final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);
            for (Status ut : ud.tweets) {
                if (!ut.isRetweet()) {
                    final Date utTime = ut.getCreatedAt();
                    if (beginTime.before(utTime) && endTime.after(utTime)) {
                        final long diffInHours =
                                TimeUnit.MILLISECONDS.toHours(endTime.getTime()
                                        - utTime.getTime());
                        feature = Long.toString(diffInHours);
                        break;
                    } else if (beginTime.after(utTime)) {
                        break;
                    }
                }
            }
            return feature;
        }
    }

    /**
     * How many hours before t since last time f published something containing
     * the mentioned user name in t, at most 1 month (0-720).
     */
    private static class F8 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final UserMentionEntity[] mentions =
                    getUserMentionEntitiesExceptRTAuthor(t);
            if (mentions != null && mentions.length != 0) {
                final Date endTime = t.getCreatedAt();
                final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);

                for (Status ut : ud.tweets) {
                    final Date utTime = ut.getCreatedAt();
                    if (beginTime.before(utTime) && endTime.after(utTime)) {
                        final UserMentionEntity[] utmt =
                                getUserMentionEntitiesExceptRTAuthor(ut);
                        if (utmt != null && utmt.length != 0
                                && hasSameMention(mentions, utmt)) {
                            final long diffInHours =
                                    TimeUnit.MILLISECONDS.toHours(endTime
                                            .getTime() - utTime.getTime());
                            feature = Long.toString(diffInHours);
                            break;
                        }
                    } else if (beginTime.after(utTime)) {
                        break;
                    }
                }
            }

            return feature;
        }

        private static UserMentionEntity[]
                getUserMentionEntitiesExceptRTAuthor (Status t) {
            final UserMentionEntity[] mentions = t.getUserMentionEntities();
            if (!t.isRetweet() || mentions == null || mentions.length == 0) {
                return mentions; // Original tweet or no mentions.
            } else {
                final HashSet<String> authors = new HashSet<String>();
                Status t2 = t;
                while (t2.isRetweet()) {
                    t2 = t2.getRetweetedStatus();
                    // Get the author name of retweeted tweet.
                    authors.add(t2.getUser().getScreenName());
                }
                final ArrayList<UserMentionEntity> ms =
                        new ArrayList<UserMentionEntity>();
                for (UserMentionEntity m : mentions) {
                    if (!authors.contains(m.getScreenName())) {
                        ms.add(m);
                    }
                }
                return ms.toArray(new UserMentionEntity[0]);
            }
        }

        private static boolean
                hasSameMention (final UserMentionEntity[] mentions,
                        final UserMentionEntity[] h2) {
            for (UserMentionEntity i : mentions) {
                for (UserMentionEntity j : h2) {
                    if (i.getScreenName().equals(j.getScreenName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * How many hours before t since last time f published something containing
     * the domain of url in t, at most 1 month (0-720).
     */
    private static class F9 implements FeatureGetter {
        @Override
        public String getFeature (Status t, UserData ud) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final String[] domains = getUrlDomains(t);
            if (domains != null && domains.length != 0) {
                final Date endTime = t.getCreatedAt();
                final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);

                for (Status ut : ud.tweets) {
                    final Date utTime = ut.getCreatedAt();
                    if (beginTime.before(utTime) && endTime.after(utTime)) {
                        final String[] utDm = getUrlDomains(ut);
                        if (utDm != null && utDm.length != 0
                                && hasSameUrl(domains, utDm)) {
                            final long diffInHours =
                                    TimeUnit.MILLISECONDS.toHours(endTime
                                            .getTime() - utTime.getTime());
                            feature = Long.toString(diffInHours);
                            break;
                        }
                    } else if (beginTime.after(utTime)) {
                        break;
                    }
                }
            }

            return feature;
        }

        private static final HashSet<String> INVALID;
        static {
            INVALID = new HashSet<String>();
            INVALID.add("wp.me");
            INVALID.add("ow.ly");
            INVALID.add("bit.ly");
            INVALID.add("goo.gl");
            INVALID.add("ht.ly");
            INVALID.add("fb.me");
            INVALID.add("t.co");
            INVALID.add("dlvr.it");
            INVALID.add("dld.bz");
            INVALID.add("shar.es");
            INVALID.add("ift.tt");
            INVALID.add("po.st");
        }

        private static String[] getUrlDomains (Status t) {
            final URLEntity[] urls = t.getURLEntities();
            if (urls == null || urls.length == 0) {
                return null;
            } else {
                final HashSet<String> domains = new HashSet<String>();
                for (URLEntity u : urls) {
                    final String u2 = u.getExpandedURL();
                    if (u2 != null) {
                        try {
                            String domain = new URL(u2).getHost();
                            // Get rid of www.
                            domain =
                                    domain.startsWith("www.") ? domain
                                            .substring(4) : domain;
                            if (!INVALID.contains(domain)) {
                                domains.add(domain);
                            }

                        } catch (MalformedURLException e) {
                            // do nothing.
                        }
                    }
                }

                return domains.toArray(new String[0]);
            }
        }

        private static boolean
                hasSameUrl (final String[] h1, final String[] h2) {
            for (String i : h1) {
                for (String j : h2) {
                    if (i.equals(j)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private static Date getNewTime (Date time, int dif) {
        final Calendar c = Calendar.getInstance();
        c.setTime(time);
        final int h = c.get(Calendar.HOUR);
        c.set(Calendar.HOUR, h + dif);
        return c.getTime();
    }
}
