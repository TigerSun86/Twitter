package util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * FileName: Cache.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Oct 30, 2014 6:45:25 PM
 */
public class Cache<T> {
    private static final int DEF_SIZE = 10;
    private int maxSize;

    private final HashMap<String, DataAndTime<T>> map =
            new HashMap<String, DataAndTime<T>>();

    public Cache(int maxSize) {
        this.maxSize = maxSize;
    }

    public Cache() {
        this(DEF_SIZE);
    }

    public T get (String name) {
        DataAndTime<T> dt = map.get(name);

        if (dt != null) { // Data exists.
            dt.time = new Date(); // Current time.
            return dt.data;
        } else {
            return null;
        }
    }

    public void put (String name, T data) {
        final DataAndTime<T> dt = map.get(name);
        if (dt != null) { // Already exists.
            // Update the time to current. That means put operation is a usage.
            dt.data = data;
            dt.time = new Date();
        } else { // Not exists.
            // Discard oldest one.
            if (map.size() >= maxSize) {
                discardOldestOne();
            }
            // Add new one.
            map.put(name, new DataAndTime<T>(data));
        }
    }

    public int size () {
        return map.size();
    }

    private void discardOldestOne () {
        if (map.isEmpty()) {
            return;
        }
        Date oldTime = null;
        String name = null;
        for (Entry<String, DataAndTime<T>> e : map.entrySet()) {
            Date time = e.getValue().time;
            if (oldTime == null || oldTime.after(time)) {
                oldTime = time;
                name = e.getKey();
            }
        }
        assert name != null;
        map.remove(name);
    }

    private static class DataAndTime<T> {
        public T data;
        public Date time;

        public DataAndTime(T data) {
            this.data = data;
            this.time = new Date();
        }
    }
}
