package com.gelakinetic.mtgJson2Familiar;

public class m2fLogger {

    private static LogLevel mLogLevel = LogLevel.INFO;

    public static void setLogLevel(LogLevel lvl) {
        mLogLevel = lvl;
    }

    public static void log(LogLevel lvl, String str) {
        if (lvl.ordinal() <= mLogLevel.ordinal()) {
            switch (lvl) {
                case ERROR: {
                    System.err.println("ERROR! - " + str);
                    break;
                }
                case INFO:
                case DEBUG: {
                    System.out.println(str);
                    break;
                }
            }
        }
    }

    public static void logStackTrace(LogLevel lvl, Exception e) {
        if (lvl.ordinal() <= mLogLevel.ordinal()) {
            e.printStackTrace();
        }
    }

    public enum LogLevel {
        ERROR,
        INFO,
        DEBUG,
    }
}
