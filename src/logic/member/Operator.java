package logic.member;

import content.Key;
import content.Token;
import content.TokenGroup;
import logic.params.Parameters;
import logic.Pointer;
import logic.typdef.Type;

public class Operator extends Member {

    Parameters params;

    Token operator;
    Key op = Key.NOONE;
    Token contentToken;
    TokenGroup typeToken;
    Pointer typePtr;

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
                state = 2;
            } else if (state == 2 && token.key.isOperator) {
                this.token = operator = token;
                op = token.key;
                state = 3;
            } else if (state == 2 && token.equals("cast")) {
                this.token = operator = token;
                op = Key.CAST;
                state = 3;
            } else if (state == 2 && token.equals("auto")) {
                this.token = operator = token;
                op = Key.AUTO;
                state = 3;
            } else if (state == 3 && token.key == Key.PARAM) {
                params = new Parameters(cFile, token);
                state = 4;
            } else if (state == 4 && (token.key == Key.BRACE || token.key == Key.SEMICOLON)) {
                contentToken = token;
                state = 5;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 5) {
                cFile.erro(token, "Unexpected end of token");
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        if (typeToken != null) {
            typePtr = cFile.getPointer(typeToken.start, typeToken.end, null, type);

            if (params != null) {
                params.load();

                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "operator "+op+" "+params+" : "+ typePtr;
    }
}
