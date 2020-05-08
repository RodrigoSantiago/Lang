package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.params.Parameters;
import logic.Pointer;
import logic.typdef.Type;

public class Operator extends Member {
    Token operator;
    Pointer castType;
    Pointer returnType;
    Parameters params;

    Token nameToken;
    Token contentToken;
    TokenGroup typeToken;
    Key op;

    public Operator(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, true, true, true, true, false);
            } else if (state == 0 && token.key == Key.WORD) {
                typeToken = new TokenGroup(token, next = TokenGroup.nextType(next, end));
                state = 1;
            } else if (state == 1 && token.key == Key.OPERATOR) {
                this.token = token;
                state = 2;
            } else if (state == 2 && token.key.isOperator) {
                op = token.key;
                state = 2;
            } else if (state == 2 && token.equals("cast")) {
                op = Key.CAST;
                state = 2;
            } else if (state == 2 && token.equals("auto")) {
                op = Key.AUTO;
                state = 2;
            } else if (token.key == Key.PARAM) {
                if (state != 2 || params != null) {
                    cFile.erro(token, "Unexpected parameters");
                } else {
                    state = 3;
                }
                if (params == null) {
                    params = new Parameters(cFile, token);
                }
            } else if (token.key == Key.BRACE || token.key == Key.SEMICOLON) {
                if (state != 3 || contentToken != null) {
                    cFile.erro(token, "Unexpected token");
                } else {
                    state = 4;
                }
                if (contentToken == null) {
                    contentToken = token;
                }
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of token");
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public String toString() {
        return "operator "+op+" "+params+" : "+ returnType;
    }
}
