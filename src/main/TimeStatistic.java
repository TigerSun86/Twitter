package main;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * FileName: TimeStatistic.java
 * @Description:
 *
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Mar 11, 2015 6:53:35 PM
 */
public class TimeStatistic {
    WeekSt total = new WeekSt();
    TreeMap<String, WeekSt> weekData = new TreeMap<String, WeekSt>();

    public void add (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        // Update statistic for all weeks.
        total.add(c.getTime());
        // Update statistic for given week.
        String weekId = getWeekId(c);
        WeekSt wData;
        if (weekData.containsKey(weekId)) {
            wData = weekData.get(weekId);
        } else {
            wData = new WeekSt();
            weekData.put(weekId, wData);
        }
        wData.add(c.getTime());
    }

    /**
     * Get the probability of given day (Mon, Tus...) in given week.
     * Ld(mon,w1) = m of week1 / sum of week1
     */
    public double probOfDay (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        String weekId = getWeekId(c);
        if (!weekData.containsKey(weekId)) {
            return 0; // Have no statistic for the given day.
        } else {
            return probOfDay2(weekId, c.get(Calendar.DAY_OF_WEEK));
        }
    }

    private double probOfDay2 (String weekId, int day) {
        WeekSt wData = weekData.get(weekId);
        double dayCount = (double) wData.get(day);
        double sum = (double) wData.get();
        if (sum == 0) {
            return 0; // No data for the week (Impossible for this code).
        } else {
            return dayCount / sum;
        }
    }

    /**
     * Get the average probability of given day (Mon, Tus...)
     * AvgLd(m) = m of total / sum of total
     */
    public double avgProbOfDay (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        int day = c.get(Calendar.DAY_OF_WEEK);
        double dayCountInTotal = (double) total.get(day);
        double sumInTotal = (double) total.get();
        if (sumInTotal == 0) {
            return 0; // No any data.
        } else {
            return dayCountInTotal / sumInTotal;
        }
    }

    /**
     * Get the standard deviation of given day (Mon, Tus...)
     * SdLd(m) = sqrt of (Ld(m,w1) - AvgLd(m))^2 + (Ld(m,w2) - AvgLd(m))^2 +
     * (Ld(m,w3) - AvgLd(m))^2 / weekCount
     * */
    public double stdDivOfDay (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        double avg = avgProbOfDay(c.getTime());
        double sd = 0;
        for (String weekId : weekData.keySet()) {
            double probOfDay = probOfDay2(weekId, c.get(Calendar.DAY_OF_WEEK));
            double dif = probOfDay - avg;
            sd += dif * dif;
        }
        int weekCount = weekData.size();
        sd /= weekCount;
        sd = Math.sqrt(sd);
        return sd;
    }

    /**
     * Get the probability of given hour (0,...,23) in given day (Mon, Tus...)
     * in given week.
     * Lh(18pm,m,w1) = 18 of m of w1 / m of w1
     */
    public double probOfHour (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        String weekId = getWeekId(c);
        if (!weekData.containsKey(weekId)) {
            return 0; // Have no statistic for the given time.
        } else {
            return probOfHour2(weekId, c.get(Calendar.DAY_OF_WEEK),
                    c.get(Calendar.HOUR_OF_DAY));
        }
    }

    private double probOfHour2 (String weekId, int day, int hour) {
        WeekSt wData = weekData.get(weekId);
        double hourCount = (double) wData.get(day, hour);
        double dayCount = (double) wData.get(day);
        if (dayCount == 0) {
            return 0; // No data for such day.
        } else {
            return hourCount / dayCount;
        }
    }

    /**
     * Get the average probability of given hour (0,...,23) in given day (Mon,
     * Tus...)
     * AvgLh(18,m) = sum of 18 of m of w1,w2,w3 / m of total
     */
    public double avgProbOfHour (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        int day = c.get(Calendar.DAY_OF_WEEK);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        double hourCountInTotal = (double) total.get(day, hour);
        double dayCountInTotal = (double) total.get(day);
        if (dayCountInTotal == 0) {
            return 0; // No any data at the day.
        } else {
            return hourCountInTotal / dayCountInTotal;
        }
    }

    /**
     * Get the standard deviation of given hour (0,...,23) in given day (Mon,
     * Tus...)
     * SdLh(18,m) = sqrt of (Lh(18,m,w1) - AvgLh(18,m))^2+ (Lh(18,m,w2) -
     * AvgLh(18,m))^2 + (Lh(18,m,w1) - AvgLh(18,m))^2 / weekCount
     * */
    public double stdDivOfHour (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        double avg = avgProbOfHour(c.getTime());
        int day = c.get(Calendar.DAY_OF_WEEK);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        double sd = 0;
        for (String weekId : weekData.keySet()) {
            double probOfHour = probOfHour2(weekId, day, hour);
            double dif = probOfHour - avg;
            sd += dif * dif;
        }
        int weekCount = weekData.size();
        sd /= weekCount;
        sd = Math.sqrt(sd);
        return sd;
    }

    /** @return true, if has data in the week of given date; false, otherwise. */
    public boolean hasDataInTheWeek (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        String weekId = getWeekId(c);
        return weekData.containsKey(weekId);
    }

    public static Date getLastWeekDate (Date date) {
        Calendar c = getCalendarInConstantTimeZone(date);
        c.set(Calendar.WEEK_OF_YEAR, c.get(Calendar.WEEK_OF_YEAR) - 1);
        return c.getTime();
    }

    private static Calendar getCalendarInConstantTimeZone (Date date) {
        final Calendar c = Calendar.getInstance();
        c.setTime(date);
        // Set the time with the same time zone to guarantee author and all
        // followers are measured in the same way.
        c.setTimeZone(TimeZone.getDefault());
        return c;
    }

    private static String getWeekId (Calendar c) {
        int year = c.get(Calendar.YEAR);
        int weekOfYear = c.get(Calendar.WEEK_OF_YEAR);
        String weekId = year + " " + weekOfYear;
        return weekId;
    }

    /** Statistic table for whole week time (7 days * 24 hours) */
    private static class WeekSt {
        int total = 0;
        // In java source code Calendar.SUNDAY is 1 and Calendar.SATURDAY is 7
        int[] totalOfDay = new int[Calendar.SATURDAY + 1];
        // Row: day, Column: hour
        int[][] st = new int[Calendar.SATURDAY + 1][24];

        public void add (Date date) {
            final Calendar c = Calendar.getInstance();
            c.setTime(date);
            int day = c.get(Calendar.DAY_OF_WEEK);
            int hour = c.get(Calendar.HOUR_OF_DAY);
            total++;
            totalOfDay[day]++;
            st[day][hour]++;
        }

        public int get () {
            return total;
        }

        public int get (int day) {
            return totalOfDay[day];
        }

        public int get (int day, int hour) {
            return st[day][hour];
        }
    }
}
