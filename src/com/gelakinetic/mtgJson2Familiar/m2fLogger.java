package com.gelakinetic.mtgJson2Familiar;

public class m2fLogger {

    public enum LogLevel {
        ERROR,
        INFO,
        DEBUG,
    }

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

    public static void logStackTrace(Exception e) {
        if (LogLevel.ERROR.ordinal() <= mLogLevel.ordinal()) {
            e.printStackTrace();
        }
    }
}
