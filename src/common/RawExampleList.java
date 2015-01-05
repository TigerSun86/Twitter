package common;

import java.util.ArrayList;

/**
 * FileName: RawExampleList.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Mar 13, 2014 2:48:22 AM
 */
public class RawExampleList extends ArrayList<RawExample> {
    private static final long serialVersionUID = 1L;

    public RawExampleList() {
        super();
    }

    public RawExampleList(final RawExampleList exList) {
        super();

        this.addAll(exList);
    }

    public RawExampleList(final String fileName) {
        super();

        final DataReader in = new DataReader(fileName);
        int numOfAttr = -1;
        while (true) {
            final String line = in.nextLine();
            if (line == null) {
                break;
            }
            if (line.length() <= 1) {
                continue; // Skip empty line.
            }
            final String[] examStr = line.split(" ");
            final RawExample ex = new RawExample();
            // First n-1 items are xList, last one is target.
            for (int i = 0; i < examStr.length - 1; i++) {
                ex.xList.add(examStr[i]);
            }
            ex.t = examStr[examStr.length - 1];

            if (numOfAttr == -1) { // Initialize.
                numOfAttr = ex.xList.size();
                this.add(ex);
            } else if (numOfAttr == ex.xList.size()) {
                this.add(ex);
            } else {
                System.err.println("Inconsistent number of attributes in line "
                        + (in.getLineNumber() + 1));
            }
        } // End of while (true) {
        in.close();
    }

    @Override
    public String toString () {
        final StringBuffer sb = new StringBuffer();
        for (RawExample ex : this) {
            sb.append(ex.toString());
            sb.append(util.Dbg.NEW_LINE);
        }
        return sb.toString();
    }
}
