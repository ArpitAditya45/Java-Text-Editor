package com.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class TerminalUI {
    static int cursorRow = 0;
    static int cursorCol = -1;
    static List<StringBuilder> lineBuffer = new ArrayList<>();
    static Info staticPrint = new Info();
    static final int HEADER_DISPLAY = 2;

    public void createTerminal() {
        try {

            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .jna(true) // Enables native terminal control
                    .build();

            terminal.enterRawMode(); // Capture all key presses (like in Vim)
            staticPrint.clearScreen(terminal);
            staticPrint.printImpDetailInStarting(terminal);
            startProcess(terminal);
            terminal.puts(InfoCmp.Capability.clear_screen);
            terminal.close();

        } catch (Exception ex) {
            System.out.println("An exception occured while creating the terminal");
            System.out.println(ex.getStackTrace());
        }
    }

    public static void blockSignals() {
        Signal.handle(new Signal("INT"), new SignalHandler() {
            @Override
            public void handle(Signal sig) {
                System.out.println("Ctrl+C (SIGINT) ignored.");
                // Optional: add custom logic here
            }
        });

        Signal.handle(new Signal("TSTP"), new SignalHandler() {
            @Override
            public void handle(Signal sig) {
                System.out.println("Ctrl+Z (SIGTSTP) ignored.");
                // Optional: remove this if you want to allow suspension
            }
        });
    }

    public static void startProcess(Terminal terminal) throws IOException {

        lineBuffer.add(new StringBuilder());
        while (true) {
            blockSignals();
            int key = terminal.reader().read();
            // terminal.writer().println(key);
            // terminal.flush();
            if (key == 17) // Ctrl+Q to exit
                break;
            if (key == 127 || key == 8) { // backspace
                handleBackSpace(terminal);
                continue;
            }
            if (key >= 32 && key <= 126) { // All the printable ASCII character
                lineBuffer.get(cursorRow).append(Character.toString(key));
                cursorCol++;
                terminal.writer().print((char) key);
                terminal.flush();
            }
            if (key == 13 || key == 10) { // If the user presses return , for new line
                lineBuffer.add(new StringBuilder());
                cursorRow++;
                cursorCol = -1;
                terminal.writer().print("\n");
                terminal.flush();
            }

        }
    }

    public static void handleBackSpace(Terminal terminal) {
        String functionName = "TerminalUI::handleBackSpace";

        try {
            // System.out.println(cursorRow + " " + cursorCol);
            if (cursorRow >= 0) {
                if (cursorCol >= 0) {
                    lineBuffer.get(cursorRow).deleteCharAt(cursorCol);
                    cursorCol--;
                    terminal.writer().print("\b \b");
                    terminal.flush();
                } else if (cursorCol == -1 && cursorRow > 0) {// The whole sentence is deleted , so move to pevious
                                                              // sentence if stored

                    // Future me will hate me for this logic , but current me is so happy !!!
                    lineBuffer.remove(cursorRow);
                    cursorRow--; // Moving sentence to previous
                    cursorCol = lineBuffer.get(cursorRow).length() - 1; // set cursorCol to last index of the previous
                                                                        // word
                    // lineBuffer.get(cursorRow).deleteCharAt(cursorCol);
                    // cursorCol--;
                    staticPrint.clearScreen(terminal);
                    redraw(terminal);
                }

            }
        } catch (Exception ex) {
            System.out.println("An exception occured while creating the terminal in function " + functionName);
            System.out.println(ex.getStackTrace());
        }
    }

    public static void redraw(Terminal terminal) {
        staticPrint.printImpDetailInStarting(terminal);
        for (StringBuilder stringBuilder : lineBuffer) {
            terminal.writer().println(stringBuilder.toString());
        }
        cursorRow = lineBuffer.size() - 1;
        cursorCol = lineBuffer.get(cursorRow).length() - 1; // placing the character on the last index , if delete is
                                                            // pressed , it will work
        terminal.writer().print(String.format("\033[%d;%dH", cursorRow + HEADER_DISPLAY + 1, cursorCol + 2)); // if
                                                                                                              // append
                                                                                                              // is
                                                                                                              // pressed
                                                                                                              // this
                                                                                                              // will
                                                                                                              // work
        terminal.flush();
    }
}
