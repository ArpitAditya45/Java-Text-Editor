package com.editor;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

import com.editor.util.Debugger;

public class Info {
    public void printImpDetailInStarting(Terminal terminal) {
        terminal.writer().println("Welcome to your Text Editor!");
        terminal.writer().println("Press Ctrl+Q to exit.");
        terminal.writer().println("Press Ctrl+U for undo and Ctrl+R for redo");
        terminal.flush();
    }

    public static void showStatus(Terminal terminal, String message) {
        String functionName = "TerminalUI::showStatus";
        try {
            int startRow = terminal.getHeight();
            terminal.writer().print("\033[" + startRow + ";1H");
            terminal.writer().print("\033[2K"); // Clear entire line
            terminal.writer().print(message);
            terminal.writer().flush();
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    synchronized (terminal) {
                        int linesNeeded = (message.length() / terminal.getWidth()) + 1;
                        int statusStart = terminal.getHeight() - linesNeeded + 1;
                        if (statusStart < 1)
                            statusStart = 1; // clamp to top

                        terminal.writer().print("\033[" + statusStart + ";1H");
                        terminal.writer().print("\033[J");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Debugger.log("Status clear thread was interrupted.");
                }
            }).start();

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public void clearScreen(Terminal terminal) {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.puts(InfoCmp.Capability.cursor_home);
        terminal.flush();
    }
}
