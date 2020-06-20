package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;

public class InnerCall extends Call {

    Token token;
    Expression innerExpression;

    public InnerCall(CallGroup group, Token start, Token end) {
        super(group, start, end);
        System.out.println("INNER : ("+ TokenGroup.toString(start, end)+")");
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.PARAM) {
                this.token = token;
                if (token.getChild() != null) {
                    innerExpression = new Expression(getStack(), token.getChild(), token.getLastChild());
                } else {
                    cFile.erro(token, "Unexpected end of tokens");
                }
                state = 1;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state == 0) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }
}
