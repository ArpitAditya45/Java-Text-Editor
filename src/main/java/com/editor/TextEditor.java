package com.editor;

import com.editor.util.Debugger;

/**
 * Hello world!
 *
 */
public class TextEditor {
    public static void main(String[] args) {
        String functionName = "TextEditor::main";
        try {
            TerminalUI terminal = new TerminalUI();
            terminal.createTerminal();

        } catch (Exception ex) {
            Debugger.log(functionName);
            Debugger.error(ex.getMessage());
            Debugger.printStackTrace(ex);
        }
    }
}
