package logic.member;

import content.Key;
import content.Token;
import logic.params.Parameters;
import logic.typdef.Type;

public class Constructor extends Member {

    private Parameters params;
    private Token contentToken;

    public Constructor(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();

            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, false, false, true, false, true);
            } else if (state == 0 && token.key == Key.THIS) {
                this.token = token;
                state = 1;
            } else if (state == 1 && token.key == Key.PARAM) {
                params = new Parameters(cFile, token);
                state = 2;
            } else if (state == 2 && (token.key == Key.BRACE || token.key == Key.SEMICOLON)) {
                contentToken = token;
                state = 3;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens");
            }

            token = next;
        }
    }

    public Parameters getParams() {
        return params;
    }

    @Override
    public boolean load() {
        if (token != null && params != null) {
            params.load();

            if (isStatic() && params.args.size() > 0) {
                cFile.erro(token, "Static constructors cannot have parameters");
            }

            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "this " + params;
    }
}
