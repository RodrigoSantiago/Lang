package logic.member;

import content.Key;
import content.Token;
import logic.typdef.Type;

public class Destructor extends Member {

    private Token contentToken;

    public Destructor(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token token = start;
        while (token != null && token != end) {
            next = token.getNext();
            if (state == 0 && token.key.isAttribute) {
                readModifier(cFile, token, true, true, false, false, true, false, true);
            } else if (state == 0 && token.key == Key.BITNOT) {
                state = 1;
            } else if (state == 1 && token.key == Key.THIS) {
                this.token = token;
                state = 2;
            } else if (state == 2 && token.key == Key.PARAM) {
                if (token.getChild() != null && token.getChild() != token.getLastChild()) {
                    cFile.erro(token, "A destruct cannot have parameters");
                }
                state = 3;
            } else if (state == 3 && (token.key == Key.BRACE || token.key == Key.SEMICOLON)) {
                contentToken = token;
                state = 4;
            } else {
                cFile.erro(token, "Unexpected token");
            }
            if (next == end && state != 4) {
                cFile.erro(token, "Unexpected end of tokens");
            }
            token = next;
        }
    }

    @Override
    public boolean load() {
        return contentToken != null;
    }

    @Override
    public String toString() {
        return "~this ()";
    }
}
