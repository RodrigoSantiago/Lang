package logic.stack.line;

import content.Key;
import content.Token;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

public class LineBreak extends Line {

    Token label;

    public LineBreak(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("BREAK");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.BREAK) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                label = token;
                state = 2;
            } else if ((state == 1 || state == 2) && (token.key == Key.SEMICOLON || next == end)) {
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (state == 0 && next == end) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
