package logic.stack.expression;

import content.Key;
import content.Token;
import content.TokenGroup;

public class LiteralCall extends Call {

    Token token;

    public LiteralCall(CallGroup group, Token start, Token end) {
        super(group, start, end);

        System.out.println("LITERAL : "+ TokenGroup.toString(start, end));
        Token token = start;
        Token next;
        int state = 0;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key == Key.STRING) {
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.NUMBER) {
                this.token = token;
                state = 1;
            } else if (state == 0 && (token.key == Key.TRUE || token.key == Key.FALSE)) {
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.NULL) {
                this.token = token;
                state = 1;
            } else if (state == 0 && token.key == Key.DEFAULT) {
                this.token = token;
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
