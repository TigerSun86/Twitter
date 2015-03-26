package features;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import features.AnewMap.Anew;

/**
 * FileName: FeatureExtractor.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Dec 18, 2014 4:04:46 PM
 */
public class FeatureExtractor {
    private static final String F0 = "0";
    private static final String F1 = "1";
    private static final int MOUNT_IN_HOUR = 30 * 24;

    /* 1Retweet. How many hours before t since last time f retweet something, at
     * most 1 month (0-720).
     * 
     * 2LastHashtag. How many hours before t since last time f published
     * something containing the hashtag in t, at most 1 month (0-720).
     * 
     * 3Mentioned. Whether f is mentioned in the tweet.
     * 
     * 4Picture. Whether t has picture.
     * 
     * 5Url. Whether t has url.
     * 
     * 6Hour. Hour in the week (the time t published). (0-167)
     * 
     * 7Otweet. How many hours before t since last time f published an original
     * tweet, at most 1 month (0-720).
     * 
     * 8LastMention. How many hours before t since last time f published
     * something containing the mentioned user name in t, at most 1 month
     * (0-720).
     * 
     * 9LastDomain. How many hours before t since last time f published
     * something containing the domain of url in t, at most 1 month (0-720).
     * 
     * 10OtLDay. Likelihood of writing an original tweet at the day (Mon,
     * Tus...) in last week
     * 
     * 11OtStDay. Standard deviation of likelihood of writing ot at the day
     * (Mon, Tus...)
     * 
     * 12OtLHour. Likelihood of writing an original tweet at the hour (0-23) of
     * the day (Mon, Tus...) in last week
     * 
     * 13OtStHour. Standard deviation of likelihood of writing ot at the hour
     * (0-23) of the day (Mon, Tus...)
     * 
     * 14RtLDay. Likelihood of retweet some thing at the day (Mon, Tus...) in
     * last week
     * 
     * 15RtStDay. Standard deviation of likelihood of rt at the day (Mon,
     * Tus...)
     * 
     * 16RtLHour. Likelihood of rt at the hour (0-23) of the day (Mon, Tus...)
     * in last week
     * 
     * 17RtStHour. Standard deviation of likelihood of rt at the hour (0-23) of
     * the day (Mon, Tus...)
     * 
     * 18RtWord. Contain words "retweet", "rt" or not.
     * 
     * 19Valence. ANEW score valence (pleasure vs displeasure).
     * 
     * 20Arousal. ANEW score arousal (excitement vs calmness).
     * 
     * 21Dominance. ANEW score dominance (weakness vs strength).
     * 
     * 22PosSenti. SentiStrength positive sentiment score.
     * 
     * 23NegSenti. SentiStrength negative sentiment score.
     * 
     * 24Len. Length of text. */
    private static final List<FeatureGetter> GETTER_LIST =
            new ArrayList<FeatureGetter>();
    static {
        GETTER_LIST.add(new F1()); // 1Retweet
        GETTER_LIST.add(new F2()); // 2LastHashtag
        GETTER_LIST.add(new F3()); // 3Mentioned
        GETTER_LIST.add(new F4()); // 4Picture
        GETTER_LIST.add(new F5()); // 5Url
        // GETTER_LIST.add(new F6()); // 6Hour
        GETTER_LIST.add(new F7()); // 7Otweet
        GETTER_LIST.add(new F8()); // 8LastMention
        GETTER_LIST.add(new F9()); // 9LastDomain
        GETTER_LIST.add(new F10()); // 10OtLDay
        GETTER_LIST.add(new F11()); // 11OtStDay
        GETTER_LIST.add(new F12()); // 12OtLHour
        GETTER_LIST.add(new F13()); // 13OtStHour
        GETTER_LIST.add(new F14()); // 14RtLDay
        GETTER_LIST.add(new F15()); // 15RtStDay
        GETTER_LIST.add(new F16()); // 16RtLHour
        GETTER_LIST.add(new F17()); // 17RtStHour
        GETTER_LIST.add(new F18()); // 18RtWord
        GETTER_LIST.add(new F19()); // 19Valence
        GETTER_LIST.add(new F20()); // 20Arousal
        GETTER_LIST.add(new F21()); // 21Dominance
        GETTER_LIST.add(new F22()); // 22PosSenti
        GETTER_LIST.add(new F23()); // 23NegSenti
        GETTER_LIST.add(new F24()); // 24Len
    }

    public static ArrayList<String> getFeatures (Status t, User userProfile,
            List<Status> userTweets) {
        final ArrayList<String> fs = new ArrayList<String>();
        for (FeatureGetter f : GETTER_LIST) {
            fs.add(f.getFeature(t, userProfile, userTweets));
        }
        return fs;
    }

    private static interface FeatureGetter {
        // User tweets should be ordered by oldest to latest.
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets);
    }

    /**
     * How many hours before t since last time f retweet something,
     * at most 1 month.
     */
    private static class F1 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final Date endTime = t.getCreatedAt();
            final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);
            // Go through tweets from latest to oldest.
            for (int i = userTweets.size() - 1; i >= 0; i--) {
                final Status ut = userTweets.get(i);
                if (ut.isRetweet()) {
                    final Date utTime = ut.getCreatedAt();
                    if (beginTime.before(utTime) && endTime.after(utTime)) {
                        // The first one found is the closest one before t.
                        final long diffInHours =
                                TimeUnit.MILLISECONDS.toHours(endTime.getTime()
                                        - utTime.getTime());
                        feature = Long.toString(diffInHours);
                        break;
                    } else if (beginTime.after(utTime)) {
                        break; // Too old tweets.
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final HashtagEntity[] hashtags = t.getHashtagEntities();
            if (hashtags != null && hashtags.length != 0) {
                final Date endTime = t.getCreatedAt();
                final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);

                // Go through tweets from latest to oldest.
                for (int i = userTweets.size() - 1; i >= 0; i--) {
                    final Status ut = userTweets.get(i);
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = F0;
            final UserMentionEntity[] mentions = t.getUserMentionEntities();
            if (mentions != null && mentions.length != 0) {
                for (UserMentionEntity u : mentions) {
                    if (u.getId() == userProfile.getId()) {
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
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
    @SuppressWarnings("unused")
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final Date endTime = t.getCreatedAt();
            final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);
            // Go through tweets from latest to oldest.
            for (int i = userTweets.size() - 1; i >= 0; i--) {
                final Status ut = userTweets.get(i);
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final UserMentionEntity[] mentions =
                    getUserMentionEntitiesExceptRTAuthor(t);
            if (mentions != null && mentions.length != 0) {
                final Date endTime = t.getCreatedAt();
                final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);

                // Go through tweets from latest to oldest.
                for (int i = userTweets.size() - 1; i >= 0; i--) {
                    final Status ut = userTweets.get(i);
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
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = Long.toString(MOUNT_IN_HOUR);
            final String[] domains = getUrlDomains(t);
            if (domains != null && domains.length != 0) {
                final Date endTime = t.getCreatedAt();
                final Date beginTime = getNewTime(endTime, -MOUNT_IN_HOUR);

                // Go through tweets from latest to oldest.
                for (int i = userTweets.size() - 1; i >= 0; i--) {
                    final Status ut = userTweets.get(i);
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

    private static long userIdOfLikelihood = -1;
    private static TimeStatistic otStatistic = null; // Original tweet st.
    private static TimeStatistic rtStatistic = null; // Retweet st.

    private static void doStatistic (long userId, List<Status> userTweets) {
        if (userId != userIdOfLikelihood) { // A different user.
            // Do statistic for the user: count all tweets.
            userIdOfLikelihood = userId;
            otStatistic = new TimeStatistic();
            rtStatistic = new TimeStatistic();
            for (Status t : userTweets) {
                Date d = t.getCreatedAt();
                if (!t.isRetweet()) { // Is original tweet.
                    otStatistic.add(d);
                } else {// Is retweet.
                    rtStatistic.add(d);
                }
            }
        }
    }

    /**
     * 10. Likelihood of writing an original tweet at the day (Mon, Tus...) in
     * last week:
     * Ld(mon,w1) = m of w1 / sum of w1
     * If the user do not have information at the last week, just use average
     * probability.
     */
    private static class F10 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob;
            if (otStatistic.hasDataInTheWeek(date)) {
                prob = otStatistic.probOfDay(date);
            } else { // Have no information at that week.
                prob = otStatistic.avgProbOfDay(date);
            }
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 11. Standard deviation of writing ot at the day (Mon, Tus...):
     * 
     * SdLd(m) = sqrt of (Ld(m,w1) - AvgLd(m))^2 + (Ld(m,w2) - AvgLd(m))^2 +
     * (Ld(m,w3) - AvgLd(m))^2 / weekCount
     */
    private static class F11 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob = otStatistic.stdDivOfDay(date);
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 12. Likelihood of writing an original tweet at the hour (0-23) of the day
     * (Mon, Tus...) in last week:
     * Lh(18pm,m,w1) = 18 of m of w1 / m of w1
     * If the user do not have information at the last week, just use average
     * probability.
     */
    private static class F12 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob;
            if (otStatistic.hasDataInTheWeek(date)) {
                prob = otStatistic.probOfHour(date);
            } else { // Have no information at that week.
                prob = otStatistic.avgProbOfHour(date);
            }
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 13. Standard deviation of writing ot at the hour (0-23) of the day (Mon,
     * Tus...):
     * SdLh(18,m) = sqrt of (Lh(18,m,w1) - AvgLh(18,m))^2+ (Lh(18,m,w2) -
     * AvgLh(18,m))^2 + (Lh(18,m,w1) - AvgLh(18,m))^2 / weekCount
     */
    private static class F13 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob = otStatistic.stdDivOfHour(date);
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 14. Likelihood of retweet some thing at the day (Mon, Tus...) in last
     * week:
     * Ld(mon,w1) = m of w1 / sum of w1
     * If the user do not have information at the last week, just use average
     * probability.
     */
    private static class F14 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob;
            if (rtStatistic.hasDataInTheWeek(date)) {
                prob = rtStatistic.probOfDay(date);
            } else { // Have no information at that week.
                prob = rtStatistic.avgProbOfDay(date);
            }
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 15. Standard deviation of rt at the day (Mon, Tus...):
     * SdLd(m) = sqrt of (Ld(m,w1) - AvgLd(m))^2 + (Ld(m,w2) - AvgLd(m))^2 +
     * (Ld(m,w3) - AvgLd(m))^2 / weekCount
     */
    private static class F15 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob = rtStatistic.stdDivOfDay(date);
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 16. Likelihood of rt at the hour (0-23) of the day (Mon, Tus...) in last
     * week:
     * Lh(18pm,m,w1) = 18 of m of w1 / m of w1
     * If the user do not have information at the last week, just use average
     * probability.
     */
    private static class F16 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob;
            if (rtStatistic.hasDataInTheWeek(date)) {
                prob = rtStatistic.probOfHour(date);
            } else { // Have no information at that week.
                prob = rtStatistic.avgProbOfHour(date);
            }
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 17. Standard deviation of rt at the hour (0-23) of the day (Mon, Tus...):
     * SdLh(18,m) = sqrt of (Lh(18,m,w1) - AvgLh(18,m))^2+ (Lh(18,m,w2) -
     * AvgLh(18,m))^2 + (Lh(18,m,w1) - AvgLh(18,m))^2 / weekCount
     */
    private static class F17 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            doStatistic(userProfile.getId(), userTweets);
            // Get the date one week before t.
            Date date = TimeStatistic.getLastWeekDate(t.getCreatedAt());
            // Get the probability.
            double prob = rtStatistic.stdDivOfHour(date);
            String feature = Double.toString(prob);
            return feature;
        }
    }

    /**
     * 18RtWord. Contain words "retweet", "rt" or not.
     * This means this (original) tweet has a sentence like
     * "retweet this please" or "plz rt", it could have a higher chance
     * than not saying that.
     */
    private static class F18 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = F0;
            String content = t.getText().toLowerCase(); // Ignore case.
            if (content.contains("retweet") || content.contains("rt")) {
                feature = F1;
            }
            return feature;
        }
    }

    // Initialize when need it.
    private static AnewMap ANEW_MAP = null;
    // Make anew score reusable for f19,20,21
    private static HashMap<Long, Anew> anewScores = null;

    private static Anew getAnewScore (Status t) {
        if (ANEW_MAP == null) {
            ANEW_MAP = new AnewMap();
            anewScores = new HashMap<Long, Anew>();
        }
        if (!anewScores.containsKey(t.getId())) {
            Anew anewScore = ANEW_MAP.score(t.getText());
            anewScores.put(t.getId(), anewScore);
        }
        return anewScores.get(t.getId());
    }

    /**
     * 19Valence. ANEW_MAP score valence (pleasure vs displeasure).
     */
    private static class F19 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Anew score = getAnewScore(t);
            String feature = Double.toString(score.valence);
            return feature;
        }
    }

    /**
     * 20Arousal. ANEW score arousal (excitement vs calmness).
     */
    private static class F20 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Anew score = getAnewScore(t);
            String feature = Double.toString(score.arousal);
            return feature;
        }
    }

    /**
     * 21Dominance. ANEW score dominance (weakness vs strength).
     */
    private static class F21 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Anew score = getAnewScore(t);
            String feature = Double.toString(score.dominance);
            return feature;
        }
    }

    private static MySentiStrength SENTI_STRENGTH = null;
    // Make sentiment score reusable for f22,23
    private static HashMap<Long, int[]> sentiScores = null;

    private static int[] getSentiScore (Status t) {
        if (SENTI_STRENGTH == null) {
            SENTI_STRENGTH = new MySentiStrength();
            sentiScores = new HashMap<Long, int[]>();
        }
        if (!sentiScores.containsKey(t.getId())) {
            int[] sentiScore = SENTI_STRENGTH.score(t.getText());
            sentiScores.put(t.getId(), sentiScore);
        }
        return sentiScores.get(t.getId());
    }

    /**
     * 22PosSenti. SentiStrength positive sentiment score.
     */
    private static class F22 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            // sentiScore[0] is positive sentiment
            int[] score = getSentiScore(t);
            String feature = Integer.toString(score[0]);
            return feature;
        }
    }

    /**
     * 23NegSenti. SentiStrength negative sentiment score.
     */
    private static class F23 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            // sentiScore[1] is negative sentiment
            int[] score = getSentiScore(t);
            String feature = Integer.toString(score[1]);
            return feature;
        }
    }

    /**
     * 24Len. Length of text.
     */
    private static class F24 implements FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = Integer.toString(t.getText().length());
            return feature;
        }
    }
}
