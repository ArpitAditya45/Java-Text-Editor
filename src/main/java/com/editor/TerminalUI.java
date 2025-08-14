package com.editor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import com.editor.util.Constants.CursorDirection;
import com.editor.util.Constants.OperationType;
import com.editor.util.Debugger;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.builtins.Completers.FileNameCompleter;

public class TerminalUI {
    static int cursorRow = 0;
    static int cursorCol = 0;
    static List<StringBuilder> lineBuffer = new ArrayList<>();
    static Info staticPrint = new Info();
    static final int HEADER_DISPLAY = 3;
    static int prefferedCol = -1; // remember the vertical memory for up /down cursor movements
    static Stack<TextAction> undoStack = new Stack<>();
    static Stack<TextAction> redoStack = new Stack<>();
    static String saveFilePath = "";

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
            int key = terminal.reader().read();
            // terminal.writer().println(key);
            // terminal.flush();
            // terminal.writer().println(cursorRow + " " + cursorCol);
            if (key == 17) {// Ctrl+Q to exit
                if (saveFilePath.length() == 0) {
                    if (saveFilePath.equals("") && saveFilePath.length() == 0) {
                        performAskPath(terminal, false);
                    }
                    performSaveFile(terminal, saveFilePath);
                }
                break;
            }

            if (key == 127 || key == 8) { // backspace
                handleBackSpace(terminal);
                // clear the redo stack as it is no longer valid
                redoStack.clear();
                continue;
            }
            if (key >= 32 && key <= 126) { // All the printable ASCII character
                // Printing the line after every character append
                undoStack.push(
                        new TextAction(cursorRow, cursorCol, new StringBuilder(Character.toString((char) key)),
                                OperationType.INSERT));
                StringBuilder line = lineBuffer.get(cursorRow);
                line.insert(cursorCol, (char) key);
                cursorCol++;
                clearLineAndRedraw(terminal);
                // clear the redo stack as it is no longer valid
                redoStack.clear();
            }
            if (key == 13 || key == 10) { // If the user presses return , for new line
                performEnter(terminal);
                undoStack.push(new TextAction(cursorRow, cursorCol, new StringBuilder(), OperationType.SPLIT_LINE));
                // clear the redo stack as it is no longer valid
                redoStack.clear();
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
            if (key == 21) { // Implement Undo
                performUndo(terminal);
            }
            if (key == 18) { // Implement Redo
                performRedo(terminal);
            }
            if (key == 19) { // Save to file
                if (saveFilePath.equals("") && saveFilePath.length() == 0) {
                    performAskPath(terminal, false);
                }
                performSaveFile(terminal, saveFilePath);
            }
            if (key == 15) {
                if (lineBuffer.get(0).length() != 0) {
                    Info.showStatus(terminal, "Save the previous file before opening new one!!");
                    performAskPath(terminal, false);
                    performSaveFile(terminal, saveFilePath);
                }
                lineBuffer.clear();
                Info.showStatus(terminal, "Provide path for opening a new file");
                performAskPath(terminal, true);
                copyFileContentsToBuffer(terminal, saveFilePath);

            }

        }
    }

    public static void handleBackSpace(Terminal terminal) {
        String functionName = "TerminalUI::handleBackSpace";

        try {
            if (cursorRow >= 0) {
                StringBuilder line = lineBuffer.get(cursorRow);

                if (cursorCol > 0) {
                    char ch = line.charAt(cursorCol - 1);
                    line.deleteCharAt(cursorCol - 1);
                    cursorCol--;
                    undoStack
                            .push(new TextAction(cursorRow, cursorCol,
                                    new StringBuilder(Character.toString(ch)),
                                    OperationType.DELETE));
                    clearLineAndRedraw(terminal);
                } else if (cursorCol == 0 && cursorRow > 0) {// The whole sentence is deleted or the cursor is at
                                                             // starting , so move to pevious
                    performLineMerge(terminal);
                    undoStack
                            .push(new TextAction(cursorRow, cursorCol, new StringBuilder(),
                                    OperationType.JOIN_LINE));
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
        String functionName = "TerminalUI::moveCursorToSpecificPosition";
        try {
            terminal.writer().print(String.format("\033[%d;%dH", row + HEADER_DISPLAY + 1, col + 1));
            terminal.flush();
        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void performUndo(Terminal terminal) {
        String functionName = "TerminalUI:performUndo";
        try {
            if (undoStack.isEmpty()) {
                return;
            }

            // Pop the last user action
            TextAction textAction = undoStack.pop();
            cursorRow = textAction.row;
            cursorCol = textAction.col;

            switch (textAction.op) {
                case INSERT:
                    // Undoing an insert means we delete the inserted character
                    // Push the action to redoStack so it can be re-inserted later if redo is
                    // triggered
                    redoStack.push(textAction);
                    lineBuffer.get(cursorRow).deleteCharAt(cursorCol);
                    clearLineAndRedraw(terminal);
                    break;

                case DELETE:
                    // Undoing a delete means re-inserting the character(s)
                    redoStack.push(textAction);
                    lineBuffer.get(cursorRow).insert(cursorCol, textAction.text.toString());

                    cursorCol = cursorCol + 1;
                    // move the cursor one ahead so if insertion happens the character is
                    // appended at correct position , why not use it in stack? if it is
                    // appended at stack then index out of bound error happens because the
                    // index is one ahead of entire length for easy insertion, yeah i also
                    // hated coding this
                    //
                    // TL;DR: inserting at buffer.length() is fine, but cursorCol = length()
                    // won't let you deleteCharAt(cursorCol) later (IndexOutOfBounds).
                    clearLineAndRedraw(terminal);
                    break;

                case SPLIT_LINE:
                    // Undoing a split (Enter key) means merging the split lines
                    performLineMerge(terminal);

                    // Push after, since merging lines shifts content and changes cursor state
                    // Pushing before would capture a cursorRow that may not exist after merge
                    redoStack.push(
                            new TextAction(cursorRow, cursorCol, new StringBuilder(textAction.text), textAction.op));
                    break;

                case JOIN_LINE:
                    // Undoing a line join means splitting the lines again
                    performEnter(terminal);

                    // Push after to avoid invalid cursorRow if the split fails or changes structure
                    redoStack.push(
                            new TextAction(cursorRow, cursorCol, new StringBuilder(textAction.text), textAction.op));
                    break;

                default:
                    break;
            }

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void clearLineAndRedraw(Terminal terminal) {
        String functionName = "TerminalUI::clearLineAndRedraw";
        try {
            String line = lineBuffer.get(cursorRow).toString();
            terminal.writer().print(String.format("\033[%d;1H", cursorRow + HEADER_DISPLAY + 1)); // Move to row
            // column 1
            terminal.writer().print("\033[2K"); // Clear the entire line
            terminal.writer().print(line);
            terminal.writer()
                    .print(String.format("\033[%d;%dH", cursorRow + HEADER_DISPLAY + 1, cursorCol + 1)); // keep
                                                                                                         // the
                                                                                                         // cursor
                                                                                                         // at
                                                                                                         // the
                                                                                                         // location
            terminal.flush();

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void performEnter(Terminal terminal) {
        String functionName = "TerminalUI::performEnter";
        try {
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

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }

    }

    public static void performLineMerge(Terminal terminal) {
        String functionName = "TerminalUI::performLineMerge";
        try {
            StringBuilder currentLine = lineBuffer.remove(cursorRow); // remove current
            // Get the column before appending so the cursor is in exact postion
            cursorCol = Math.max(0, lineBuffer.get(cursorRow - 1).length());
            lineBuffer.get(cursorRow - 1).append(currentLine); // merge current into previous
            staticPrint.clearScreen(terminal);
            staticPrint.printImpDetailInStarting(terminal);
            handleRedraw(terminal);
            cursorRow = cursorRow - 1; // move the cursor to previous row
            moveCursorToSpecificPosition(terminal, cursorRow, cursorCol);

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void performRedo(Terminal terminal) {
        String functionName = "TerminalUI::performRedo";
        try {
            if (redoStack.isEmpty()) {
                return;
            }

            // Pop the last undone action from redoStack
            TextAction textAction = redoStack.pop();

            // Restore the cursor to the original position of the action
            cursorRow = textAction.row;
            cursorCol = textAction.col;

            // NOTE: Redo means reapplying the original action that was undone.
            // So we perform the same action again and push it back to undoStack,
            // so we can "undo the redo" if needed.

            switch (textAction.op) {
                case INSERT:
                    // We are redoing an INSERT → means we INSERT the character again
                    lineBuffer.get(cursorRow).insert(cursorCol, textAction.text);
                    cursorCol = cursorCol + 1;// Again the logic if we don't move it one ahead it will insert at same
                                              // position and overrid the character

                    // Push the same action to undo stack so user can undo this redo
                    undoStack.push(textAction); // just to be consistent we push it after , we can push it befor too ,
                                                // it will work

                    clearLineAndRedraw(terminal);
                    break;

                case DELETE:
                    // We are redoing a DELETE → means we DELETE the character again
                    lineBuffer.get(cursorRow).deleteCharAt(cursorCol);

                    // Push the same action to undo stack
                    undoStack.push(textAction); // same as above insert comment

                    clearLineAndRedraw(terminal);
                    break;

                case SPLIT_LINE:
                    // SPLIT_LINE = Enter key (splits current line at cursorCol into two lines)
                    // We are redoing the split, so we perform 'enter' again
                    performEnter(terminal);

                    // IMPORTANT: Push AFTER performing split, because
                    // if cursorRow == 0 and we push before splitting, the split may fail
                    // (since the stack will contain invalid state during split)
                    undoStack.push(
                            new TextAction(cursorRow, cursorCol, new StringBuilder(textAction.text), textAction.op));
                    break;

                case JOIN_LINE:
                    // JOIN_LINE = undo of Enter (merges current line with the previous one)
                    performLineMerge(terminal);

                    // IMPORTANT: Push AFTER performing merge, for same reason as above.
                    // We need cursorRow and cursorCol updated to correct state post-merge
                    undoStack.push(
                            new TextAction(cursorRow, cursorCol, new StringBuilder(textAction.text), textAction.op));
                    break;

                default:
                    // No-op or unknown action
                    break;
            }

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void performSaveFile(Terminal terminal, String filePath) {
        String functionName = "performSaveFile";
        try {
            FileWriter writer = new FileWriter(filePath);
            for (StringBuilder line : lineBuffer) {
                writer.write(line.toString());
                writer.write("\n");
            }
            writer.close();
            Info.showStatus(terminal, "Saved file to" + filePath);
            terminal.flush();
        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static void performAskPath(Terminal terminal, boolean isOpen) {
        String functionName = "TerminalUI:performAskPath";
        try {
            Info.showStatus(terminal,
                    "Please enter the path to your file for home start with '~' this message will clear once you start the path press tab for autocompletion :");
            LineReader fileReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(new FileNameCompleter()) // enables tab completion for files/dirs
                    .build();

            saveFilePath = fileReader.readLine();
            if (saveFilePath.contains("~")) {
                String homeDir = System.getProperty("user.home");
                saveFilePath = saveFilePath.replace("~", homeDir);
            }
            if (checkFileisPresent(terminal) && !isOpen) {
                Info.showStatus(terminal,
                        "The file " + saveFilePath + " is already present do you want to override it? y/n : ");
                while (true) {
                    String text = fileReader.readLine();
                    if (text.length() > 1) {
                        Info.showStatus(terminal, "Only Enter y/n: ");
                        continue;
                    }
                    if (text.length() == 1) {
                        if (text.charAt(0) == 'y' || text.charAt(0) == 'Y') {
                            Info.showStatus(terminal, "The File will be overriden");
                            break;
                        } else if (text.charAt(0) == 'n' || text.charAt(0) == 'N') {
                            long timestamp = System.currentTimeMillis();
                            saveFilePath = saveFilePath + timestamp;
                            break;
                        }
                    }
                }
            }
        } catch (UserInterruptException ex) {

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }

    public static boolean checkFileisPresent(Terminal terminal) {
        String functionName = "TerminalUI:checkFileisPresent";
        try {
            File file = new File(saveFilePath);
            if (file.exists()) {
                // moved the status out of here
                return true;
            }

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
        return false;
    }

    public static void copyFileContentsToBuffer(Terminal terminal, String filePath) {
        String functionName = "TerminalUI:copyFileToBuffer";
        try {
            File file = new File(saveFilePath);
            if (file.exists()) {
                FileReader fileReader = new FileReader(file);
                int content = fileReader.read();
                StringBuilder currentLine = new StringBuilder();
                while (content != -1) {
                    if (content == 13) {
                        // /r --> carrige return
                        continue;
                    } else if (content == 10) {
                        lineBuffer.add(currentLine);
                        currentLine = new StringBuilder();
                    } else if (currentLine.length() >= terminal.getWidth()) {
                        lineBuffer.add(currentLine);
                        currentLine = new StringBuilder();
                    } else {
                        currentLine.append((char) content);
                    }

                    content = fileReader.read();
                }
                if (currentLine.length() > 0) {
                    lineBuffer.add(currentLine);
                }
                staticPrint.clearScreen(terminal);
                staticPrint.printImpDetailInStarting(terminal);
                handleRedraw(terminal);
                cursorRow = lineBuffer.size() - 1;
                cursorCol = lineBuffer.get(cursorRow).length();
                moveCursorToSpecificPosition(terminal, cursorRow, cursorCol);
                fileReader.close();

            } else {
                Info.showStatus(terminal, "The given file does not exist try again");
            }

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }
}
