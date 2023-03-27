package android.util;

public class Log {
    @SuppressWarnings("checkstyle:methodname")
    public static int d(String tag, String msg) {
        System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    @SuppressWarnings("checkstyle:methodname")
    public static int i(String tag, String msg) {
        System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    @SuppressWarnings("checkstyle:methodname")
    public static int w(String tag, String msg) {
        System.out.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    @SuppressWarnings("checkstyle:methodname")
    public static int e(String tag, String msg) {
        System.out.println("ERROR: " + tag + ": " + msg);
        return 0;
    }
}