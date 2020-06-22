package logic.stack.line;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.stack.Block;
import logic.stack.Context;
import logic.stack.Line;
import logic.stack.expression.Expression;

public class LineCase extends Line {

    private boolean isDefault;
    TokenGroup caseToken;
    Expression caseExp;

    public LineCase(Block block, Token start, Token end) {
        super(block, start, end);

        Token init = start;
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && (token.key == Key.CASE || token.key == Key.DEFAULT)) {
                System.out.println(token.key == Key.DEFAULT ? "DEFAULT" : "CASE");
                init = next;
                isDefault = token.key == Key.DEFAULT;
                state = 1;
            } else if (state == 1 && token.key == Key.COLON) {
                if (init != token) {
                    if (isDefault) {
                        cFile.erro(token, "Default Statment should not have a value", this);
                    }
                    caseToken = new TokenGroup(init, token);
                    caseExp = new Expression(this, init, token);
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

    @Override
    public void load() {
        if (caseExp != null) {
            caseExp.load(new Context(stack));
            // TODO -REQUEST LITERAL
        }
    }

    public boolean isDefault() {
        return isDefault;
    }
}
