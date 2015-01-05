package common;

/**
 * FileName: DataWriter.java
 * @Description:
 *               Usage:
 *               DataWriter out = new DataWriter(fName);
 *               if (out.hasNoException()){
 *               out.write(data);
 *               out.close();
 *               }
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Apr 21, 2014 1:56:05 AM
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

public class DataWriter {
    private BufferedWriter out = null;
    private final String fName;

    public DataWriter(final String fName2) {
        this.fName = fName2;
        try {
            URL url = new URL(fName2);
            File file = new File(url.getPath());
            if (!file.exists()) {
                file.createNewFile();
            }
            out = new BufferedWriter(new FileWriter(file));
        } catch (final IOException e) {
            System.err.println("Couldn't create file: " + fName);
            close();
        }
    }

    public final boolean hasNoException () {
        return out != null;
    }

    public final void write (final String s) {
        try {
            out.write(s, 0, s.length());
        } catch (final IOException e) {
            System.err.println("Couldn't write file: " + fName);
            close();
        }
    }

    public final void close () {
        if (out != null) {
            try {
                out.close();
            } catch (final IOException e) {
                System.err.println("Couldn't close file: " + fName);
            }
        }
    }
}
