package com.editor;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

public class Info {
    public void printImpDetailInStarting(Terminal terminal) {
        terminal.writer().println("Welcome to your Text Editor!");
        terminal.writer().println("Press Ctrl+Q to exit.");
        terminal.flush();
    }

    public void clearScreen(Terminal terminal) {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.puts(InfoCmp.Capability.cursor_home);
        terminal.flush();
    }
}
