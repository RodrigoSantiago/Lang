package logic.member;

import content.Key;
import content.Token;
import data.CppBuilder;
import logic.typdef.Type;

public class Destructor extends Member {

    private Token contentToken;

    public Destructor(Type type, Token start, Token end) {
        super(type);

        int state = 0;
        Token next;
        Token last = start;
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

            last = token;
            token = next;
        }

        if (state != 4) {
            cFile.erro(last, "Unexpected end of tokens");
        }
    }

    @Override
    public boolean load() {
        return contentToken != null;
    }

    public void build(CppBuilder cBuilder) {

    }

    @Override
    public String toString() {
        return "~this ()";
    }
}
