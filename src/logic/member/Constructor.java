package logic.member;

import content.Key;
import content.Token;
import data.ContentFile;
import logic.params.Parameters;
import logic.typdef.Type;

public class Constructor extends Member {

    public Parameters params;
    public Token contentToken;

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
            } else if (token.key == Key.BRACE || token.key == Key.SEMICOLON) {
                if (contentToken == null) {
                    contentToken = token;
                }
                if (state != 2) {
                    cFile.erro(token, "Unexpected token");
                } else {
                    state = 3;
                }
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 3) {
                cFile.erro(token, "Unexpected end of tokens");
            }

            token = next;
        }
    }

    @Override
    public boolean load() {
        if (token != null && params != null) {
            params.load();
            if (isStatic() && params.args.size() > 0) {
                cFile.erro(token, "Static constructors cannot have parameters");
            }
            return true;
        } else {
            return token != null && contentToken != null;
        }
    }

    @Override
    public String toString() {
        return "this " + params;
    }
}
