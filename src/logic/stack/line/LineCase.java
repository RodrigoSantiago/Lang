package logic.stack.line;

import content.Key;
import content.Token;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.Stack;
import logic.stack.expression.Expression;

public class LineCase extends Line {
    Expression expression;

    public LineCase(Block block, Token start, Token end) {
        super(block, start, end);
        System.out.println("CASE");

        Token key = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.CASE) {
                key = token;
                state = 1;
            } else if (state == 1 && token.key == Key.COLON) {
                expression = new Expression(this, key.getNext(), token);
                state = 2;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (state != 2 && next == end) {
                cFile.erro(token, "Colon expected");
            }
            token = next;
        }
    }
}
