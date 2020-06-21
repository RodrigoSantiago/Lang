package logic.stack.line;

import content.Key;
import content.Token;
import logic.stack.Block;
import logic.stack.Line;
import logic.stack.expression.Expression;

public class LineCase extends Line {

    private boolean isDefault;
    Expression expression;

    public LineCase(Block block, Token start, Token end) {
        super(block, start, end);

        Token key = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.CASE || token.key == Key.DEFAULT)) {
                System.out.println(token.key == Key.DEFAULT ? "DEFAULT" : "CASE");
                key = next;
                isDefault = token.key == Key.DEFAULT;
                state = 1;
            } else if (state == 1 && token.key == Key.COLON) {
                if (key != token) {
                    if (isDefault) {
                        cFile.erro(token, "Default Statment should not have a value", this);
                    }
                    expression = new Expression(this, key, token);
                }
                state = 2;
            } else if (state != 1) {
                cFile.erro(token, "Unexpected token", this);
            }
            if (state != 2 && next == end) {
                cFile.erro(token, "Colon expected", this);
            }
            token = next;
        }
    }

    public boolean isDefault() {
        return isDefault;
    }
}
