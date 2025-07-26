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
    static int cursorCol = 0;
    static List<StringBuilder> lineBuffer = new ArrayList<>();
    static Info staticPrint = new Info();
    static final int HEADER_DISPLAY = 2;
    static int prefferedCol = -1; // remember the vertical memory for up /down cursor movements

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
        String functionName = "TerminalUI::handelBlockSignals";
        try {
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
        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void startProcess(Terminal terminal) throws IOException {
        lineBuffer.add(new StringBuilder());
        while (true) {
            handelBlockSignals();
            int key = terminal.reader().read();
            // terminal.writer().println(key);
            // terminal.flush();
            // terminal.writer().println(cursorRow + " " + cursorCol);
            if (key == 17) // Ctrl+Q to exit
                break;
            if (key == 127 || key == 8) { // backspace
                handleBackSpace(terminal);
                continue;
            }
            if (key >= 32 && key <= 126) { // All the printable ASCII character
                // Printing the line after every character append
                StringBuilder line = lineBuffer.get(cursorRow);
                line.insert(cursorCol, (char) key);
                cursorCol++;
                terminal.writer().print(String.format("\033[%d;1H", cursorRow + HEADER_DISPLAY + 1)); // Move to row
                                                                                                      // column 1
                terminal.writer().print("\033[2K"); // Clear the entire line
                terminal.writer().print(line.toString());
                terminal.writer().print(String.format("\033[%d;%dH", cursorRow + HEADER_DISPLAY + 1, cursorCol + 1)); // keep
                                                                                                                      // the
                                                                                                                      // cursor
                                                                                                                      // at
                                                                                                                      // the
                                                                                                                      // location
                terminal.flush();
            }
            if (key == 13 || key == 10) { // If the user presses return , for new line
                int currentLineSize = lineBuffer.get(cursorRow).length();
                String left = lineBuffer.get(cursorRow).substring(0, cursorCol);
                String right = lineBuffer.get(cursorRow).substring(cursorCol);
                lineBuffer.get(cursorRow).replace(0, cursorCol, left);
                lineBuffer.get(cursorRow).replace(cursorCol, currentLineSize, "");
                lineBuffer.add(cursorRow + 1, new StringBuilder(right));
                cursorRow++;
                cursorCol = 0;
                staticPrint.clearScreen(terminal);
                staticPrint.printImpDetailInStarting(terminal);
                handleRedraw(terminal);
                moveCursorToSpecificPosition(terminal, cursorRow, cursorCol);
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
            if (cursorRow >= 0) {
                if (cursorCol > 0) {
                    String line = lineBuffer.get(cursorRow).deleteCharAt(cursorCol - 1).toString();
                    cursorCol--;
                    terminal.writer().print(String.format("\033[%d;1H", cursorRow + HEADER_DISPLAY + 1)); // Move to row
                    // column 1
                    terminal.writer().print("\033[2K"); // Clear the entire line
                    terminal.writer().print(line.toString());
                    terminal.writer()
                            .print(String.format("\033[%d;%dH", cursorRow + HEADER_DISPLAY + 1, cursorCol + 1)); // keep
                                                                                                                 // the
                                                                                                                 // cursor
                                                                                                                 // at
                                                                                                                 // the
                                                                                                                 // location
                    terminal.flush();
                } else if (cursorCol == 0 && cursorRow > 0) {// The whole sentence is deleted , so move to pevious
                    StringBuilder currentLine = lineBuffer.remove(cursorRow); // remove current
                    // Get the column before appending so the cursor is in exact postion
                    cursorCol = Math.max(0, lineBuffer.get(cursorRow - 1).length());
                    lineBuffer.get(cursorRow - 1).append(currentLine); // merge current into previous
                    staticPrint.clearScreen(terminal);
                    staticPrint.printImpDetailInStarting(terminal);
                    handleRedraw(terminal);
                    cursorRow = lineBuffer.size() - 1;
                    moveCursorToSpecificPosition(terminal, cursorRow, cursorCol);
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
            for (StringBuilder stringBuilder : lineBuffer) {
                terminal.writer().println(stringBuilder.toString());
            }
            terminal.flush();
        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void handleCursorMovements(Terminal terminal, CursorDirection direction) {
        String functionName = "TerminalUI::handleCursorMovements";
        try {
            switch (direction) {
                case UP:
                    if (cursorRow > 0) {
                        if (prefferedCol == -1)
                            prefferedCol = cursorCol;
                        cursorRow--;
                        int prevLineLength = lineBuffer.get(cursorRow).length();
                        cursorCol = Math.min(prefferedCol, prevLineLength);
                        terminal.writer()
                                .print(String.format("\033[%d;%dH", cursorRow + HEADER_DISPLAY + 1, cursorCol + 1));
                    }
                    break;
                case DOWN:
                    if (cursorRow < lineBuffer.size() - 1) {
                        if (prefferedCol == -1)
                            prefferedCol = cursorCol;
                        cursorRow++;
                        int nextLineLength = lineBuffer.get(cursorRow).length();
                        cursorCol = Math.min(prefferedCol, nextLineLength);
                        terminal.writer()
                                .print(String.format("\033[%d;%dH", cursorRow + HEADER_DISPLAY + 1, cursorCol + 1));
                    }
                    break;
                case RIGHT:
                    if (cursorCol < lineBuffer.get(cursorRow).length()) {
                        terminal.writer().print("\033[C");
                        cursorCol++;
                        prefferedCol = -1;
                    }
                    break;
                case LEFT:
                    if (cursorCol > 0) {
                        terminal.writer().print("\033[D");
                        cursorCol--;
                        prefferedCol = -1;
                    }
                    break;
                default:
                    break;
            }
            terminal.flush();
        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }

    }

    public static void moveCursorToSpecificPosition(Terminal terminal, int row, int col) {
        terminal.writer().print(String.format("\033[%d;%dH", row + HEADER_DISPLAY + 1, col + 1));
        terminal.flush();
    }
}
