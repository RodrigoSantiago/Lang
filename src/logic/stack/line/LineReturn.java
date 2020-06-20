package logic.stack.line;

import content.Key;
import content.Token;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;

public class LineReturn extends Line {

    Token contentStart;
    Token contentEnd;

    public LineReturn(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("RETURN");

        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.RETURN) {
                contentStart = next;
                state = 1;
            } else if (state == 1 && (token.key == Key.SEMICOLON || next == end)) {
                contentEnd = (token.key == Key.SEMICOLON ? token : next);
                state = 2;
            } else if (state != 1) {
                cFile.erro(token, "Unexpected token");
            }
            if (state != 2 && next == end) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
