package com.editor;

import com.editor.util.Constants.OperationType;

public class TextAction {
    int row;
    int col;
    StringBuilder text;
    OperationType op;

    TextAction(int row, int col, StringBuilder text, OperationType operation) {
        this.row = row;
        this.col = col;
        this.text = text;
        this.op = operation;
    }

}
