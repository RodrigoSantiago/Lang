package logic.stack.line;

import content.Key;
import content.Token;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;
import logic.stack.expression.Expression;

public class LineReturn extends Line {

    Expression expression;

    public LineReturn(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("RETURN");

        Token key = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.RETURN) {
                key = next;
                state = 1;
            } else if (state == 1 && (token.key == Key.SEMICOLON || next == end)) {
                if (token.key != Key.SEMICOLON) {
                    cFile.erro(token, "Semicolon expected");
                }
                if (key != (token.key == Key.SEMICOLON ? token : next)) {
                    expression = new Expression(this, key, token.key == Key.SEMICOLON ? token : next);
                }
                state = 2;
            } else if (state != 1) {
                cFile.erro(token, "Unexpected token");
            }
            if (state == 0 && next == end) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
