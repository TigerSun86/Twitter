package common;

/**
 * FileName: DataReader.java
 * @Description: Read data from file.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Mar 6, 2014
 */
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

public class DataReader{
    private LineNumberReader in = null;
    private final String fName;

    public DataReader(final String fNameIn) {
        this.fName = fNameIn;
        try {
            final URL attrURL = new URL(fName);
            in =
                    new LineNumberReader(new InputStreamReader(
                            attrURL.openStream()));
        } catch (final IOException e) {
            System.err.println("Couldn't open file: " + fName);
            close();
        }
    }

    public final String nextLine () {
        String inputLine = null;
        try {
            inputLine = in.readLine();
        } catch (final IOException e) {
            System.err.println("Couldn't read file: " + fName);
            close();
        }

        if (inputLine == null) {
            // End of file.
            return null;
        }

        return inputLine;
    }

    public final void close () {
        if (in != null) {
            try {
                in.close();
            } catch (final IOException e) {
                System.err.println("Couldn't close file: " + fName);
            }
        }
    }
    
    public final int getLineNumber(){
        return in.getLineNumber();
    }
}
