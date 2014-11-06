package main;

import java.util.HashMap;

import test.UserData;

/**
 * FileName: FileAndId2User.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Oct 30, 2014 6:36:45 PM
 */
public class FileAndId2User {
    public String file = null;
    public HashMap<Long, UserData> idToUser = null;

    @Override
    public boolean equals (Object o) {
        if (!(o instanceof FileAndId2User)) {
            return false;
        }
        FileAndId2User o2 = (FileAndId2User) o;
        if (this.file == o2.file) {
            return true;
        }
        if (this.file == null) {
            return false;
        } else {
            return this.file.equals(o2.file);
        }
    }

    @Override
    public int hashCode () {
        if (file == null) {
            return 0;
        } else {
            return file.hashCode();
        }
    }
}
