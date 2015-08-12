package features;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import main.ExampleGetter;
import twitter4j.HashtagEntity;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.Trend;
import twitter4j.URLEntity;
import twitter4j.User;
import twitter4j.UserMentionEntity;
import weka.clusterers.Clusterer;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.InstanceList;

import common.RawAttr;
import common.RawAttrList;

import datacollection.Database;
import features.AnewMap.Anew;
import features.ClusterWord.ClusterWordSetting;
import features.FeatureEditor.FeatureFactory;

/**
 * FileName: FeatureExtractor.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Dec 18, 2014 4:04:46 PM
 */
public class FeatureExtractor {
    public static final boolean NEED_STEM = false;

    private static final String F0 = "0";
    private static final String F1 = "1";
    private static final int MOUNT_IN_HOUR = 30 * 24;

    public FeatureExtractor() {
        getterListOfPreNum = new ArrayList<FeatureGetter>();
        getterListOfPreNum.addAll(BASE_FEATURE_SET);
    }

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
     * 24Len. Length of text.
     * 
     * 25LongestWord. Length of longest word.
     * 
     * 26Trend. Contains trend word or not.
     * 
     * 27DayInWeek. Monday is 0, Tuesday is 1....
     * 
     * 28HourInDay. 0am is 0, 1am is 1...
     * 
     * 29Hashtag. Whether t has hashtag.
     * 
     * 30Mention. Whether t mentioned someone. */
    private final List<FeatureGetter> getterList =
            new ArrayList<FeatureGetter>();

    @SuppressWarnings("unused")
    private void initGetter () {
        getterList.add(new F1()); // 1Retweet
        getterList.add(new F2()); // 2LastHashtag
        getterList.add(new F3()); // 3Mentioned
        getterList.add(new F4()); // 4Picture
        getterList.add(new F5()); // 5Url
        // getterList.add(new F6()); // 6Hour
        getterList.add(new F7()); // 7Otweet
        getterList.add(new F8()); // 8LastMention
        getterList.add(new F9()); // 9LastDomain
        getterList.add(new F10()); // 10OtLDay
        getterList.add(new F11()); // 11OtStDay
        getterList.add(new F12()); // 12OtLHour
        getterList.add(new F13()); // 13OtStHour
        getterList.add(new F14()); // 14RtLDay
        getterList.add(new F15()); // 15RtStDay
        getterList.add(new F16()); // 16RtLHour
        getterList.add(new F17()); // 17RtStHour
        getterList.add(new F18()); // 18RtWord
        getterList.add(new F19()); // 19Valence
        getterList.add(new F20()); // 20Arousal
        getterList.add(new F21()); // 21Dominance
        getterList.add(new F22()); // 22PosSenti
        getterList.add(new F23()); // 23NegSenti
        getterList.add(new F24()); // 24Len
        getterList.add(new F25()); // 25LongestWord
        getterList.add(new F26()); // 26Trend
    }

    public static final List<FeatureGetter> BASE_FEATURE_SET =
            new ArrayList<FeatureGetter>();

    static {
        BASE_FEATURE_SET.add(new F4()); // 4Picture
        BASE_FEATURE_SET.add(new F5()); // 5Url
        // BASE_FEATURE_SET.add(new F18()); // 18RtWord
        BASE_FEATURE_SET.add(new F19()); // 19Valence
        BASE_FEATURE_SET.add(new F20()); // 20Arousal
        BASE_FEATURE_SET.add(new F21()); // 21Dominance
        BASE_FEATURE_SET.add(new F22()); // 22PosSenti
        BASE_FEATURE_SET.add(new F23()); // 23NegSenti
        BASE_FEATURE_SET.add(new F24()); // 24Len
        BASE_FEATURE_SET.add(new F25()); // 25LongestWord
        BASE_FEATURE_SET.add(new F26()); // 26Trend
        BASE_FEATURE_SET.add(new F27()); // 27DayInWeek
        BASE_FEATURE_SET.add(new F28()); // 28HourInDay
        BASE_FEATURE_SET.add(new F29()); // 29Hashtag
        BASE_FEATURE_SET.add(new F30()); // 30Mention
        // for (FeatureGetter fg : new
        // DayInWeekV2Factory().getNewFeatures(null)) {
        // //BASE_FEATURE_SET.add(fg); // 31DayInWeekV2
        // }
        // for (FeatureGetter fg : new
        // HourInDayV2Factory().getNewFeatures(null)) {
        // //BASE_FEATURE_SET.add(fg); // 32HourInDayV2
        // }
    }

    public final List<FeatureGetter> getterListOfPreNum;

    private static RawAttr getDiscreteAttr (String name) {
        RawAttr attr;
        attr = new RawAttr(name, false);
        attr.valueList.add(F0);
        attr.valueList.add(F1);
        return attr;
    }

    private static final RawAttr CLS_ATTR1 = new RawAttr("Class");
    static {
        CLS_ATTR1.valueList.add(ExampleGetter.Y);
        CLS_ATTR1.valueList.add(ExampleGetter.N);
    }
    private static final RawAttr CLS_ATTR2 = new RawAttr("Class", true);

    public RawAttrList getAttrListOfModel1 () {
        final RawAttrList attrList = new RawAttrList();
        for (FeatureGetter g : getterList) {
            attrList.xList.add(g.getAttr());
        }
        attrList.t = CLS_ATTR1;
        return attrList;
    }

    public RawAttrList getAttrListOfPredictNum () {
        final RawAttrList attrList = new RawAttrList();
        for (FeatureGetter g : getterListOfPreNum) {
            attrList.xList.add(g.getAttr());
        }
        attrList.t = CLS_ATTR2;
        return attrList;
    }

    public ArrayList<String> getFeaturesOfModel1 (Status t, User userProfile,
            List<Status> userTweets) {
        final ArrayList<String> fs = new ArrayList<String>();
        for (FeatureGetter f : getterList) {
            fs.add(f.getFeature(t, userProfile, userTweets));
        }
        return fs;
    }

    public ArrayList<String> getFeaturesOfPredictNum (Status t,
            User userProfile, List<Status> userTweets) {
        final ArrayList<String> fs = new ArrayList<String>();
        for (FeatureGetter f : getterListOfPreNum) {
            fs.add(f.getFeature(t, userProfile, userTweets));
        }
        return fs;
    }

    public static abstract class FeatureGetter {
        // User tweets should be ordered by oldest to latest.
        public abstract String getFeature (Status t, User userProfile,
                List<Status> userTweets);

        public abstract RawAttr getAttr ();

        @Override
        public String toString () {
            return this.getAttr().name;
        }
    }

    /**
     * How many hours before t since last time f retweet something,
     * at most 1 month.
     */
    private static class F1 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("1Retweet", true);
        }
    }

    /**
     * How many hours before t since last time f published something
     * containing the hashtag in t, at most 1 month.
     */
    private static class F2 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("2LastHashtag", true);
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
    private static class F3 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("3Mentioned");
        }
    }

    /**
     * Whether t has picture.
     */
    private static class F4 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("4Picture");
        }
    }

    /**
     * Whether t has url.
     */
    private static class F5 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("5Url");
        }
    }

    /**
     * Hour in the week (the time t published). (0-168)
     */
    @SuppressWarnings("unused")
    private static class F6 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("6Hour", true);
        }
    }

    /**
     * How many hours before t since last time f published an original tweet, at
     * most 1 month (0-720).
     */
    private static class F7 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("7Otweet", true);
        }
    }

    /**
     * How many hours before t since last time f published something containing
     * the mentioned user name in t, at most 1 month (0-720).
     */
    private static class F8 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("8LastMention", true);
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
    private static class F9 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("9LastDomain", true);
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
    private static class F10 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("10OtLDay", true);
        }
    }

    /**
     * 11. Standard deviation of writing ot at the day (Mon, Tus...):
     * 
     * SdLd(m) = sqrt of (Ld(m,w1) - AvgLd(m))^2 + (Ld(m,w2) - AvgLd(m))^2 +
     * (Ld(m,w3) - AvgLd(m))^2 / weekCount
     */
    private static class F11 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("11OtStDay", true);
        }
    }

    /**
     * 12. Likelihood of writing an original tweet at the hour (0-23) of the day
     * (Mon, Tus...) in last week:
     * Lh(18pm,m,w1) = 18 of m of w1 / m of w1
     * If the user do not have information at the last week, just use average
     * probability.
     */
    private static class F12 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("12OtLHour", true);
        }
    }

    /**
     * 13. Standard deviation of writing ot at the hour (0-23) of the day (Mon,
     * Tus...):
     * SdLh(18,m) = sqrt of (Lh(18,m,w1) - AvgLh(18,m))^2+ (Lh(18,m,w2) -
     * AvgLh(18,m))^2 + (Lh(18,m,w1) - AvgLh(18,m))^2 / weekCount
     */
    private static class F13 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("13OtStHour", true);
        }
    }

    /**
     * 14. Likelihood of retweet some thing at the day (Mon, Tus...) in last
     * week:
     * Ld(mon,w1) = m of w1 / sum of w1
     * If the user do not have information at the last week, just use average
     * probability.
     */
    private static class F14 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("14RtLDay", true);
        }
    }

    /**
     * 15. Standard deviation of rt at the day (Mon, Tus...):
     * SdLd(m) = sqrt of (Ld(m,w1) - AvgLd(m))^2 + (Ld(m,w2) - AvgLd(m))^2 +
     * (Ld(m,w3) - AvgLd(m))^2 / weekCount
     */
    private static class F15 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("15RtStDay", true);
        }
    }

    /**
     * 16. Likelihood of rt at the hour (0-23) of the day (Mon, Tus...) in last
     * week:
     * Lh(18pm,m,w1) = 18 of m of w1 / m of w1
     * If the user do not have information at the last week, just use average
     * probability.
     */
    private static class F16 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("16RtLHour", true);
        }
    }

    /**
     * 17. Standard deviation of rt at the hour (0-23) of the day (Mon, Tus...):
     * SdLh(18,m) = sqrt of (Lh(18,m,w1) - AvgLh(18,m))^2+ (Lh(18,m,w2) -
     * AvgLh(18,m))^2 + (Lh(18,m,w1) - AvgLh(18,m))^2 / weekCount
     */
    private static class F17 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return new RawAttr("17RtStHour", true);
        }
    }

    /**
     * 18RtWord. Contain words "retweet", "rt" or not.
     * This means this (original) tweet has a sentence like
     * "retweet this please" or "plz rt", it could have a higher chance
     * than not saying that.
     */
    private static class F18 extends FeatureGetter {
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

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("18RtWord");
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
    private static class F19 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Anew score = getAnewScore(t);
            String feature = Double.toString(score.valence);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("19Valence", true);
        }
    }

    /**
     * 20Arousal. ANEW score arousal (excitement vs calmness).
     */
    private static class F20 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Anew score = getAnewScore(t);
            String feature = Double.toString(score.arousal);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("20Arousal", true);
        }
    }

    /**
     * 21Dominance. ANEW score dominance (weakness vs strength).
     */
    private static class F21 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Anew score = getAnewScore(t);
            String feature = Double.toString(score.dominance);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("21Dominance", true);
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
    private static class F22 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            // sentiScore[0] is positive sentiment
            int[] score = getSentiScore(t);
            String feature = Integer.toString(score[0]);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("22PosSenti", true);
        }
    }

    /**
     * 23NegSenti. SentiStrength negative sentiment score.
     */
    private static class F23 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            // sentiScore[1] is negative sentiment
            int[] score = getSentiScore(t);
            String feature = Integer.toString(score[1]);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("23NegSenti", true);
        }
    }

    /**
     * 24Len. Length of text.
     */
    private static class F24 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature = Integer.toString(t.getText().length());
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("24Len", true);
        }
    }

    /**
     * 25LongestWord. Length of longest word.
     */
    private static class F25 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            int max = 0;
            for (String w : WordFeature.splitIntoWords(t, false, false)) {
                int len = w.length();
                if (max < len) {
                    max = len;
                }
            }
            String feature = Integer.toString(max);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("25LongestWord", true);
        }
    }

    /**
     * 26Trend. Contains trend word or not.
     */
    private static class F26 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            String feature;
            if (tIdToTrendResult.containsKey(t.getId())) {
                feature = tIdToTrendResult.get(t.getId());
            } else {
                Database db = Database.getInstance();
                Trend[] trends = db.getTrends(t.getCreatedAt()).getTrends();
                boolean contains = false;
                for (Trend trend : trends) {
                    if (t.getText().contains(trend.getName())) {
                        contains = true;
                        break;
                    }
                }
                feature = (contains ? F1 : F0);
                tIdToTrendResult.put(t.getId(), feature);
            }

            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("26Trend");
        }

        private static HashMap<Long, String> tIdToTrendResult =
                new HashMap<Long, String>();
    }

    /**
     * 27DayInWeek.
     */
    private static class F27 extends FeatureGetter {
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
            final String feature = Integer.toString(dayOfWeek);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("27DayInWeek", true);
        }
    }

    /**
     * 28HourInDay.
     */
    private static class F28 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            final Date date = t.getCreatedAt();
            final Calendar c = Calendar.getInstance();
            c.setTime(date);
            final int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
            final String feature = Integer.toString(hourOfDay);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr("28HourInDay", true);
        }
    }

    /**
     * Ffol.
     */
    public static class Ffol extends FeatureGetter {
        long folId;

        public Ffol(long folId) {
            this.folId = folId;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            boolean rted =
                    Database.getInstance().isRetweetedByUser(t.getId(),
                            this.folId);
            final String feature = rted ? F1 : F0;
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr(Long.toString(folId));
        }
    }

    /**
     * FTweetCluster.
     */
    public static class FTweetCluster extends FeatureGetter {
        private static final double THRESHOLD = 0.2;
        private static final HashMap<Long, double[]> tweetsCache =
                new HashMap<Long, double[]>();

        int clusterId;
        Clusterer clusterer;
        StringToWordVector str2vec;

        public FTweetCluster(int clusterId, Clusterer clusterer,
                StringToWordVector str2vec) {
            this.clusterId = clusterId;
            this.clusterer = clusterer;
            this.str2vec = str2vec;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            try {
                double[] clusterProbs = tweetsCache.get(t.getId());
                if (clusterProbs == null) {
                    Instance vec = getWordVector(t);
                    clusterProbs = clusterer.distributionForInstance(vec);
                    tweetsCache.put(t.getId(), clusterProbs);
                }
                final String feature =
                        (clusterProbs[clusterId] >= THRESHOLD ? F1 : F0);
                return feature;
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
            return null;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("TweetCluster_" + clusterId);
        }

        private Instance getWordVector (Status t) throws Exception {
            Attribute strAttr =
                    new Attribute("tweet content", (FastVector) null);
            FastVector attributes = new FastVector();
            attributes.addElement(strAttr);
            Instances data = new Instances("Test-dataset", attributes, 1);

            String s = WordFeature.getTextOfTweet(t);

            double[] values = new double[data.numAttributes()];
            values[0] = data.attribute(0).addStringValue(s);
            Instance inst = new Instance(1.0, values);
            data.add(inst);

            Instances dataFiltered = Filter.useFilter(data, str2vec);

            return dataFiltered.instance(0);
        }
    }

    /**
     * FWordCluster.
     */
    public static class FWordCluster extends FeatureGetter {
        public static class SharedCache {
            private final HashMap<Long, Integer> totalCache =
                    new HashMap<Long, Integer>();
            private final HashMap<Long, int[]> clCache =
                    new HashMap<Long, int[]>();
        }

        int clusterId;
        int numOfCl;
        HashMap<String, Integer> word2Cl;
        ClusterWordSetting para;
        SharedCache cache;

        public FWordCluster(int clusterId, int numOfCl,
                HashMap<String, Integer> word2Cl, ClusterWordSetting para,
                SharedCache cache) {
            this.clusterId = clusterId;
            this.numOfCl = numOfCl;
            this.word2Cl = word2Cl;
            this.para = para;
            this.cache = cache;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            int total;
            int[] countForEachCl;
            if (cache.totalCache.containsKey(t.getId())) {
                total = cache.totalCache.get(t.getId());
                countForEachCl = cache.clCache.get(t.getId());
            } else { // Haven't cached.
                total = 0;
                countForEachCl = new int[numOfCl];
                Set<String> entities = getEntitySet(t);
                for (String w : entities) {
                    Integer cl = word2Cl.get(w);
                    if (cl != null && cl >= 0 && cl < countForEachCl.length) {
                        countForEachCl[cl]++; // It's a word can be clustered.
                        total++;
                    }
                }
                cache.totalCache.put(t.getId(), total);
                cache.clCache.put(t.getId(), countForEachCl);
            }

            final String feature;
            if (total == 0) {
                feature = "0";
            } else {
                feature =
                        String.valueOf(countForEachCl[clusterId]
                                / ((double) total));
            }
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr(ClusterWordFeatureFactory.PREFIX + clusterId,
                    true);
        }
    }

    /**
     * F29 Whether t has hashtag.
     */
    private static class F29 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            // 29. Whether t has hashtag.
            String feature = F0;
            final HashtagEntity[] entries = t.getHashtagEntities();
            if (entries != null && entries.length != 0) {
                feature = F1;
            }
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("29Hashtag");
        }
    }

    /**
     * F30 Whether t mentioned someone.
     */
    private static class F30 extends FeatureGetter {
        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            // 30. Whether t mentioned someone.
            String feature = F0;
            final UserMentionEntity[] entries = t.getUserMentionEntities();
            if (entries != null && entries.length != 0) {
                feature = F1;
            }
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("30Mention");
        }
    }

    private static final HashMap<Long, Set<String>> TWEET_2_ENTITY_SET_CACHE =
            new HashMap<Long, Set<String>>();

    private static Set<String> getEntitySet (Status t) {
        Set<String> entities = TWEET_2_ENTITY_SET_CACHE.get(t.getId());
        if (entities == null) {
            // Always split tweet with all entities.
            entities = new HashSet<String>(getEntityList(t));
            TWEET_2_ENTITY_SET_CACHE.put(t.getId(), entities);
        }
        return entities;
    }

    private static final HashMap<Long, List<String>> TWEET_2_ENTITY_LIST_CACHE =
            new HashMap<Long, List<String>>();

    private static List<String> getEntityList (Status t) {
        List<String> entities = TWEET_2_ENTITY_LIST_CACHE.get(t.getId());
        if (entities == null) {
            // Always split tweet with all entities.
            entities =
                    WordStatisDoc.getEntitiesFromTweet(t,
                            WordStatisDoc.EntityType.ALLTYPE);
            TWEET_2_ENTITY_LIST_CACHE.put(t.getId(), entities);
        }
        return entities;
    }

    /**
     * FTopEntity.
     */
    public static class FTopEntity extends FeatureGetter {
        String en;

        public FTopEntity(String en) {
            this.en = en;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Set<String> entities = getEntitySet(t);
            final String feature;
            if (entities.contains(en)) {
                feature = F1;
            } else {
                feature = F0;
            }
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr(WordFeatureFactory.FEATURE_PRIFIX + en);
        }
    }

    /**
     * FEntityPair.
     */
    public static class FEntityPair extends FeatureGetter {
        String en1;
        String en2;

        public FEntityPair(String en1, String en2) {
            this.en1 = en1;
            this.en2 = en2;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            Set<String> entities = getEntitySet(t);
            final String feature;
            if (entities.contains(en1) && entities.contains(en2)) {
                feature = F1;
            } else {
                feature = F0;
            }
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr(EntityPairFactory.PREFIX
                    + SimTable.getTwoWordsKey(en1, en2));
        }
    }

    /**
     * 31DayInWeekV2.
     */
    private static class F31 extends FeatureGetter {
        int day;

        public F31(int day) {
            this.day = day;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            final Date date = t.getCreatedAt();
            final Calendar c = Calendar.getInstance();
            c.setTime(date);
            boolean isDay = day == c.get(Calendar.DAY_OF_WEEK);
            final String feature = isDay ? F1 : F0;
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("31DayInWeekV2"
                    + DayInWeekV2Factory.DAY_MAP.get(day));
        }
    }

    private static class DayInWeekV2Factory implements FeatureFactory {
        public static final LinkedHashMap<Integer, String> DAY_MAP =
                new LinkedHashMap<Integer, String>();
        static {
            DAY_MAP.put(Calendar.MONDAY, "Mon");
            DAY_MAP.put(Calendar.TUESDAY, "Tue");
            DAY_MAP.put(Calendar.WEDNESDAY, "Wed");
            DAY_MAP.put(Calendar.THURSDAY, "Thu");
            DAY_MAP.put(Calendar.FRIDAY, "Fri");
            DAY_MAP.put(Calendar.SATURDAY, "Sat");
            DAY_MAP.put(Calendar.SUNDAY, "Sun");
        }

        @Override
        public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
            List<FeatureGetter> fs = new ArrayList<FeatureGetter>();
            for (int day : DAY_MAP.keySet()) {
                fs.add(new F31(day));
            }
            return fs;
        }

        @Override
        public Set<String> conflictedFeaturesOfBase () {
            Set<String> result = new HashSet<String>();
            result.add("27DayInWeek");
            return result;
        }
    }

    /**
     * 32HourInDayV2.
     */
    private static class F32 extends FeatureGetter {
        int hourRangeIdx;

        public F32(int hourRangeIdx) {
            this.hourRangeIdx = hourRangeIdx;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            final Date date = t.getCreatedAt();
            final Calendar c = Calendar.getInstance();
            c.setTime(date);
            final int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
            int min = HourInDayV2Factory.HOUR_RANGE_TO_MIN_MAX[hourRangeIdx][0];
            int max = HourInDayV2Factory.HOUR_RANGE_TO_MIN_MAX[hourRangeIdx][1];
            boolean isInRange = hourOfDay >= min && hourOfDay < max;
            final String feature = isInRange ? F1 : F0;
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return getDiscreteAttr("32HourInDayV2" + "Range" + hourRangeIdx);
        }
    }

    private static class HourInDayV2Factory implements FeatureFactory {
        public static final int[][] HOUR_RANGE_TO_MIN_MAX = { { 0, 6 },
                { 6, 12 }, { 12, 18 }, { 18, 24 } };

        @Override
        public List<FeatureGetter> getNewFeatures (List<Status> tweets) {
            List<FeatureGetter> fs = new ArrayList<FeatureGetter>();
            for (int hourRangeIdx = 0; hourRangeIdx < HOUR_RANGE_TO_MIN_MAX.length; hourRangeIdx++) {
                fs.add(new F32(hourRangeIdx));
            }
            return fs;
        }

        @Override
        public Set<String> conflictedFeaturesOfBase () {
            Set<String> result = new HashSet<String>();
            result.add("28HourInDay");
            return result;
        }
    }

    /**
     * FLda.
     */
    public static class FLda extends FeatureGetter {
        public static class LdaSharedCache {
            private final HashMap<Long, double[]> clCache =
                    new HashMap<Long, double[]>();
        }

        int clusterId;
        TopicInferencer inferencer;
        Pipe pipe;
        LdaSharedCache cache;

        public FLda(int clusterId, TopicInferencer inferencer, Pipe pipe,
                LdaSharedCache cache) {
            this.clusterId = clusterId;
            this.inferencer = inferencer;
            this.pipe = pipe;
            this.cache = cache;
        }

        private double[] getClProbs (Status t) {
            StringBuilder sb = new StringBuilder();
            List<String> entities = getEntityList(t);
            if (!entities.isEmpty()) {
                for (String s : entities) {
                    sb.append(s + " ");
                }
                sb.deleteCharAt(sb.length() - 1); // Remove the last space.
            }
            InstanceList testing = new InstanceList(pipe);
            testing.addThruPipe(new cc.mallet.types.Instance(sb.toString(),
                    null, "test instance", null));

            // Don't need to care about the words appearing only in test set but
            // not in training set, because Mallet ignores the words like that,
            // see TopicInferencer.java line 101.
            // Parameters 100,10,10 are in the tutorial slide:
            // http://mallet.cs.umass.edu/mallet-tutorial.pdf page 105.
            double[] clProbs =
                    inferencer.getSampledDistribution(testing.get(0), 100, 10,
                            10);
            return clProbs;
        }

        @Override
        public String getFeature (Status t, User userProfile,
                List<Status> userTweets) {
            double[] clProbs;
            if (cache.clCache.containsKey(t.getId())) {
                clProbs = cache.clCache.get(t.getId());
            } else { // Haven't cached.
                clProbs = getClProbs(t);
                cache.clCache.put(t.getId(), clProbs);
            }
            final String feature = String.valueOf(clProbs[clusterId]);
            return feature;
        }

        @Override
        public RawAttr getAttr () {
            return new RawAttr(LdaFeatureFactory.PREFIX + clusterId, true);
        }
    }

}
