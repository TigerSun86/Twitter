package util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * FileName: OReadWriter.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Oct 30, 2014 5:11:27 PM
 */
public class OReadWriter {
    public static final String PATH = "D:/Twitter/userdata/";
    // For update data.
    public static final String PATH2 = "D:/Twitter/userdata2/";
    public static final String FILE_NAME = "ud";
    public static final String EXT = ".ser";
    public static final String ID2FILE_FILENAME = "idToFile.ser";

    public static void write (Object o, final String fullPath) {
        ObjectOutputStream out = null;
        try {
            System.out
                    .println("Serialized data is saving in " + fullPath + ".");
            FileOutputStream fileOut = new FileOutputStream(fullPath);
            BufferedOutputStream bOut = new BufferedOutputStream(fileOut);
            out = new ObjectOutputStream(bOut);
            out.writeObject(o);
            System.out.println("Saving finished.");
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
            System.out.println("Reading object from " + fullPath);
            FileInputStream fileIn = new FileInputStream(fullPath);
            BufferedInputStream bIn = new BufferedInputStream(fileIn);
            in = new ObjectInputStream(bIn);
            o = in.readObject();
            System.out.println("Reading finished");
        } catch (IOException | ClassNotFoundException i) {
            i.printStackTrace();
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
}
