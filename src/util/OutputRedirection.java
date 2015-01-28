package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * FileName: OutputRedirection.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: sunx2013@my.fit.edu
 * @date Jan 8, 2015 9:47:26 PM
 */
public class OutputRedirection {
    private final PrintStream ps;

    public OutputRedirection(final String fileName) {
        this.ps = System.out;
        redirect(fileName);
    }

    public OutputRedirection() {
        this.ps = System.out;

        final String curTime =
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        final String fileName =
                Thread.currentThread().getContextClassLoader().getResource("")
                        .getPath()
                        + "log_" + curTime + ".txt";
        redirect(fileName);
    }

    public void close () {
        System.out.println(new Date());
        System.out.close();
        System.setOut(ps);
        System.out.println("Writing finished");
    }

    private static void redirect (final String fileName) {
        try {
            System.out.println("Writing result into " + fileName);
            final File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            System.setOut(new PrintStream(new FileOutputStream(file)));
            System.out.println(new Date());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
