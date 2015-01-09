package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import test.UserData;

/**
 * FileName: OReadWriter.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Oct 30, 2014 5:11:27 PM
 */
public class OReadWriter {
    public static final String MODULE = "ORW";
    public static final boolean DBG = false;

    public static final String PATH = "D:/Twitter/userdata/";
    // For update data.
    public static final String PATH2 = "D:/Twitter/userdata2/";
    public static final String FILE_NAME = "ud";
    public static final String EXT = ".ser";
    public static final String EXT2 = "ser";
    public static final String ID2FILE_FILENAME = "idToFile.ser";

    public static void write (Object o, final String fullPath) {
        ObjectOutputStream out = null;
        try {
            Dbg.print(DBG, MODULE, "Serialized data is saving in " + fullPath
                    + ".");
            FileOutputStream fileOut = new FileOutputStream(fullPath);
            BufferedOutputStream bOut = new BufferedOutputStream(fileOut);
            out = new ObjectOutputStream(bOut);
            out.writeObject(o);
            Dbg.print(DBG, MODULE, "Saving finished.");
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

    public static Object read (String fullPath) {
        Object o = null;
        ObjectInputStream in = null;
        try {
            Dbg.print(DBG, MODULE, "Reading object from " + fullPath);
            FileInputStream fileIn = new FileInputStream(fullPath);
            BufferedInputStream bIn = new BufferedInputStream(fileIn);
            in = new ObjectInputStream(bIn);
            o = in.readObject();
            Dbg.print(DBG, MODULE, "Reading finished");
        } catch (IOException | ClassNotFoundException i) {
            Dbg.print(DBG, MODULE, "No such file: " + fullPath);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return o;
    }
    
    public static UserData getUserDate (long id) {
        final String fullPath =
                OReadWriter.PATH + Long.toString(id) + OReadWriter.EXT;
        final UserData ud = (UserData) OReadWriter.read(fullPath);
        return ud;
    }
}
