package logic.stack.line;

import content.Key;
import content.Token;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

public class LineContinue extends Line {

    Token label;

    public LineContinue(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("CONTINUE");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.CONTINUE) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                label = token;
                state = 2;
            } else if ((state == 1 || state == 2) && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected");
                }

                state = 3;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end) {
                if (state == 0) {
                    cFile.erro(token, "Unexpected end of tokens");
                } else {
                    cFile.erro(token, "Semicolon expected");
                }
            }
            token = next;
        }
    }
}
