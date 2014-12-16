package util;

/**
 * FileName: Dbg.java
 * @Description: Display verbose debug information.
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Feb 25, 2014
 */
import java.util.HashSet;

public class Dbg {
    public static final String NEW_LINE = System.getProperty("line.separator");
    
    private static final String DBG_STR_SWITCH = "verbose";
    private static final String DBG_STR_ALL = "all";
    private static final String DBG_STR_DEFAULT = "default";

    // Don'tList print debug info if didn'tList assign -Dverbose.
    public static boolean dbgSwitch = false; // Default don'tList debug anything.
    private static boolean allSwitch = false; // Debug all modules.
    // Debug modules specified by module it self.
    public static boolean defaultSwitch = false;
    // Store names of modules allowed to print debug info.
    private static final HashSet<String> DBG_MODULES = new HashSet<String>();
    static {
        final String str = System.getProperty(DBG_STR_SWITCH);
        if (str != null) {
            dbgSwitch = true;
            final String[] modules = str.split(",");
            for (String module : modules) {
                if (module.equals(DBG_STR_ALL)) {
                    allSwitch = true;
                    break; // Debug all module, needn'tList look remain modules.
                } else if (module.equals(DBG_STR_DEFAULT)) {
                    defaultSwitch = true;
                } else { // Print debug info of this module.
                    DBG_MODULES.add(module);
                }
            }
        }
    }

    public static void print (final boolean dbg, final String module,
            final String dbgInfo) {
        if (!dbgSwitch) {
            return;
        }
        boolean needDbg = false;
        if (allSwitch) {
            needDbg = true;
        }

        if (!needDbg && defaultSwitch) {
            if (dbg) {
                needDbg = true;
            }
        }
        if (!needDbg && DBG_MODULES.contains(module)) {
            needDbg = true;
        }
        if (needDbg) {
            System.out.println(module + ": " + dbgInfo);
        }
    }
}
