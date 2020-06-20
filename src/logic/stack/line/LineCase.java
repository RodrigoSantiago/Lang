package logic.stack.line;

import content.Key;
import content.Token;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

public class LineCase extends Line {
    Token contentStart;
    Token contentEnd;

    public LineCase(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("CASE");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.CASE) {
                state = 1;
            } else if (state == 1 && token.key == Key.WORD) {
                contentStart = token;
                contentEnd = next;
                state = 2;
            } else if (state == 1 && (token.key == Key.NUMBER || token.key == Key.STRING)) {

                state = 4;
            } else if (state == 1 && (token.key == Key.SUB && next != end && next.key == Key.NUMBER)) {
                next = next.getNext();
                state = 4;
            } else if (state == 2 && (token.key == Key.DOT)) {
                state = 3;
            } else if (state == 3 && (token.key == Key.WORD)) {
                contentEnd = next;
                state = 4;
            } else if ((state == 2 || state == 4) && token.key == Key.COLON) {
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (state != 5 && next == end) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
