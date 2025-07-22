package com.editor.util;

public class Debugger {
    private static boolean DEBUG_ENABLED = true;

    public static void enable() {
        DEBUG_ENABLED = true;
    }

    public static void disable() {
        DEBUG_ENABLED = false;
    }

    public static void log(String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[LOG] " + message);
        }
    }

    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }

    public static void log(String tag, String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[" + tag + "] " + message);
        }
    }

    public static void error(String tag, String message) {
        System.err.println("[ERROR][" + tag + "] " + message);
    }

    public static void printStackTrace(Exception e) {
        if (DEBUG_ENABLED) {
            System.err.println("[STACKTRACE] Exception occurred: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
