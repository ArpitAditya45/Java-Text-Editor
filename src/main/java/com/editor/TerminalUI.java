package com.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import com.editor.util.Constants.CursorDirection;
import com.editor.util.Debugger;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class TerminalUI {
    static int cursorRow = 0;
    static int cursorCol = -1;
    static List<StringBuilder> lineBuffer = new ArrayList<>();
    static Info staticPrint = new Info();
    static final int HEADER_DISPLAY = 2;

    public void createTerminal() {
        String functionName = "TerminalUI::createTerminal";
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
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void handelBlockSignals() {
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
            handelBlockSignals();
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
            if (key == 27) { // This is for Escape 27
                int next1 = terminal.reader().read(); // This is [ 91
                int next2 = terminal.reader().read(); // This is final character
                if (next1 == 91) {
                    switch (next2) {
                        case 65:
                            // System.out.println("UP");
                            handleCursorMovements(terminal, CursorDirection.UP);
                            break;
                        case 66:
                            // System.out.println("DOWN");
                            handleCursorMovements(terminal, CursorDirection.DOWN);
                            break;
                        case 67:
                            // System.out.println("RIGHT");
                            handleCursorMovements(terminal, CursorDirection.RIGHT);
                            break;
                        case 68:
                            // System.out.println("LEFT");
                            handleCursorMovements(terminal, CursorDirection.LEFT);
                            break;
                        default:
                            break;
                    }
                }
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
                    handleRedraw(terminal);
                }

            }
        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void handleRedraw(Terminal terminal) {
        String functionName = "TerminalUI::redraw";
        try {
            staticPrint.printImpDetailInStarting(terminal);
            for (StringBuilder stringBuilder : lineBuffer) {
                terminal.writer().println(stringBuilder.toString());
            }
            cursorRow = lineBuffer.size() - 1;
            cursorCol = lineBuffer.get(cursorRow).length() - 1; // placing the character on the last index , if delete
                                                                // is
                                                                // pressed , it will work
            terminal.writer().print(String.format("\033[%d;%dH", cursorRow + HEADER_DISPLAY + 1, cursorCol + 2)); // if
                                                                                                                  // append
                                                                                                                  // is
                                                                                                                  // pressed
                                                                                                                  // this
                                                                                                                  // will
                                                                                                                  // work
            terminal.flush();

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void handleCursorMovements(Terminal terminal, CursorDirection direction) {
        // moveLeft
        switch (direction) {
            case UP:
                terminal.writer().print("\033[A");
                break;
            case DOWN:
                terminal.writer().print("\033[B");
                break;
            case RIGHT:
                terminal.writer().print("\033[C");
                break;
            case LEFT:
                terminal.writer().print("\033[D");
                break;
            default:
                break;
        }
        terminal.flush();

    }
}
